package org.helioviewer.viewmodel.view.jp2view;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.SocketException;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ReaderErrorReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.view.CachedMovieView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus.CacheStatus;
import org.helioviewer.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.io.http.HTTPRequest;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPRequest;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPRequestField;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduUtils;

/**
 * This class has two different purposes. The first is to connect to and
 * retrieve image data from a JPIP server (if the image is remote). The second
 * is that all view-changed signals are routed through this thread... so it must
 * forward them to the J2KRender thread through that threads signal.
 *
 * TODO The server may change the parameters of the request, and we should take
 * it into account...
 *
 * @author caplins
 * @author Juan Pablo
 * @author Markus Langenberg
 */
class J2KReader implements Runnable {
    private enum CacheStrategy {
        CURRENTFRAMEONLY, CURRENTFRAMEFIRST, MISSINGFRAMESFIRST, ALLFRAMESEQUALLY
    }

    /** Whether IOExceptions should be shown on System.err or not */
    private static final boolean verbose = false;

    /** The thread that this object runs on. */
    private volatile Thread myThread;

    /** A boolean flag used for stopping the thread. */
    private volatile boolean stop;

    /** A reference to the JP2Image this object is owned by. */
    private final JP2Image parentImageRef;

    /** A reference to the JP2ImageView this object is owned by. */
    private final JHVJP2View parentViewRef;

    /** The JPIPSocket used to connect to the server. */
    private JPIPSocket socket;

    /** The a reference to the cache object used by the run method. */
    private final JHV_Kdu_cache cacheRef;

    /**
     * The time when the last response was received. It is used for performing
     * the flow control. A negative value means that there is not a previous
     * valid response to take into account.
     */
    private volatile long lastResponseTime = -1;

    /** The current length in bytes to use for requests */
    private volatile int JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

    /**
     * The constructor. Creates and connects the socket if image is remote.
     *
     * @param _imageViewRef
     * @throws IOException
     * @throws JHV_KduException
     */
    J2KReader(JHVJP2View _imageViewRef) throws IOException, JHV_KduException {
        parentViewRef = _imageViewRef;

        // These two vars are only here for convenience sake.
        parentImageRef = parentViewRef.jp2Image;
        cacheRef = parentImageRef.getCacheRef();

        // Attempts to connect socket if image is remote.
        if (parentImageRef.isRemote()) {

            socket = parentImageRef.getSocket();

            if (socket == null) {
                socket = new JPIPSocket();
                JPIPResponse res = (JPIPResponse) socket.connect(parentImageRef.getURI());
                cacheRef.addJPIPResponseData(res);
            }

            // Somehow it seems we need to update the server cache model for
            // movies too
            // otherwise there can be a weird bug where the meta data seems
            // missing
            // if (!parentImageRef.isMultiFrame())
            KakaduUtils.updateServerCacheModel(socket, cacheRef, true);

        } else {
            socket = null;
        }

        myThread = null;
        stop = false;
    }

    /** Starts the J2KReader thread. */
    void start() {
        if (myThread != null)
            stop();
        myThread = new Thread(this, "J2KReader");
        stop = false;
        myThread.start();
    }

    /** Stops the J2KReader thread. */
    synchronized void stop() {
        if (myThread != null && myThread.isAlive()) {
            try {
                stop = true;

                do {
                    myThread.interrupt();
                    myThread.join(100);
                } while (myThread.isAlive());

            } catch (InterruptedException ex) {
                ex.printStackTrace();

            } catch (NullPointerException e) {
            } finally {
                myThread = null;
            }
        }
    }

    /** Releases the resources associated with this object. */
    void abolish() {
        stop();

        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method perfoms the flow control, that is, adjusts dynamically the
     * value of the variable <code>JPIP_REQUEST_LEN</code>. The used algorithm
     * is the same as the used one by the viewer kdu_show of Kakadu (if
     * something works... why not to use it?)
     */
    private void flowControl() {
        int adjust = 0;
        int receivedBytes = socket.getReceivedData();
        long replyTextTime = socket.getReplyTextTime();
        long replyDataTime = socket.getReplyDataTime();

        long tdat = replyDataTime - replyTextTime;

        if (((receivedBytes - JpipRequestLen) < (JpipRequestLen >> 1)) && (receivedBytes > (JpipRequestLen >> 1))) {
            if (tdat > 10000)
                adjust = -1;
            else if (lastResponseTime > 0) {
                long tgap = replyTextTime - lastResponseTime;

                if ((tgap + tdat) < 1000)
                    adjust = +1;
                else {
                    double gapRatio = ((double) tgap) / ((double) (tgap + tdat));
                    double targetRatio = (tdat + tgap) / 10000.0;

                    if (gapRatio > targetRatio)
                        adjust = +1;
                    else
                        adjust = -1;
                }
            }
        }

        JpipRequestLen += (JpipRequestLen >> 2) * adjust;

        if (JpipRequestLen > JPIPConstants.MAX_REQUEST_LEN)
            JpipRequestLen = JPIPConstants.MAX_REQUEST_LEN;

        if (JpipRequestLen < JPIPConstants.MIN_REQUEST_LEN)
            JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

        lastResponseTime = replyDataTime;
    }

    private JPIPQuery createQuery(JP2ImageParameter currParams, int iniLayer, int endLayer) {
        JPIPQuery query = new JPIPQuery();

        query.setField(JPIPRequestField.CONTEXT.toString(), "jpxl<" + iniLayer + "-" + endLayer + ">");
        query.setField(JPIPRequestField.LAYERS.toString(), String.valueOf(currParams.qualityLayers));

        Rectangle resDims = currParams.resolution.getResolutionBounds();

        query.setField(JPIPRequestField.FSIZ.toString(), String.valueOf(resDims.width) + "," + String.valueOf(resDims.height) + "," + "closest");
        query.setField(JPIPRequestField.ROFF.toString(), String.valueOf(currParams.subImage.x) + "," + String.valueOf(currParams.subImage.y));
        query.setField(JPIPRequestField.RSIZ.toString(), String.valueOf(currParams.subImage.width) + "," + String.valueOf(currParams.subImage.height));

        return query;
    }

    public boolean isConnected() {
        return (socket != null && socket.isConnected());
    }

    @Override
    public void run() {
        JPIPRequest req = null;

        boolean complete = false;
        boolean viewChanged = false;
        boolean downgradeNecessary = false;

        int prevCompositionLayer = -1;
        JP2ImageParameter prevParams = null;
        JP2ImageParameter currParams = null;

        // iamPersistent = parentViewRef.isMainView();

        while (!stop) {
            // Wait for signal
            try {
                parentViewRef.readerSignal.waitForSignal();
            } catch (InterruptedException e) {
                continue;
            }

            // If image is not remote image, do nothing and just signal render
            if (parentViewRef.getReaderMode() == ReaderMode.SIGNAL_RENDER_ONCE) {
                parentViewRef.setReaderMode(ReaderMode.NEVERFIRE);
                parentViewRef.renderRequestedSignal.signal(RenderReasons.NEW_DATA);
            } else if (!parentImageRef.isRemote() && parentViewRef.getReaderMode() != ReaderMode.NEVERFIRE) {
                parentViewRef.renderRequestedSignal.signal(RenderReasons.NEW_DATA);
            } else {

                // check, whether view parameters have changed
                prevParams = currParams;
                currParams = parentViewRef.getImageViewParams();
                viewChanged = prevParams == null || !(currParams.subImage.equals(prevParams.subImage) && currParams.resolution.equals(prevParams.resolution) && currParams.qualityLayers == prevParams.qualityLayers);

                if (!parentViewRef.isMainView) {
                    viewChanged = viewChanged || currParams.compositionLayer != prevCompositionLayer;
                    prevCompositionLayer = currParams.compositionLayer;
                }

                // if view has changed downgrade caching status
                if (viewChanged) {
                    complete = false;
                    downgradeNecessary = true;
                }

                // if socket is closed, but communication is necessary, open it
                if (socket != null && socket.isClosed() && (parentViewRef.isPersistent() || viewChanged)) {
                    try {
                        socket = new JPIPSocket();
                        socket.connect(parentImageRef.getURI());
                        if (!parentImageRef.isMultiFrame()) {
                            KakaduUtils.updateServerCacheModel(socket, cacheRef, true);
                        }

                    } catch (IOException e) {
                        if (verbose) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException ioe) {
                            Log.error(">> J2KReader.run() > Error closing socket.", ioe);
                        }
                        ChangeEvent event = new ChangeEvent(new SubImageDataChangedReason(parentViewRef));
                        event.addReason(new ReaderErrorReason(parentViewRef, e));
                        parentViewRef.fireChangeEvent(event);

                        // Send signal to try again
                        parentViewRef.readerSignal.signal();
                    } catch (JHV_KduException e) {
                        e.printStackTrace();
                    }
                }

                // if socket is open, get image data
                if (socket != null && !socket.isClosed()) {

                    try {

                        // If nothing to do, check whether there are some
                        // queries left
                        // (actually, I do not know, when this might happen...)
                        if (complete) {
                            if (parentViewRef.isPersistent() && req != null && req.getQuery() != null) {
                                socket.send(req);
                                socket.receive();
                            }

                            // If nothing to do and this is not the main view,
                            // close socket
                            if (!parentViewRef.isPersistent()) {
                                socket.close();
                            }

                            // requesting data
                        } else {

                            JPIPResponse res = null;
                            boolean stopReading = false;
                            int curLayer = currParams.compositionLayer;
                            Interval<Integer> layers = parentImageRef.getCompositionLayerRange();
                            int num_layers = layers.getEnd() - layers.getStart() + 1;

                            lastResponseTime = -1;

                            int complete_steps = 0;
                            int current_step;

                            // build queries
                            JPIPQuery[] stepQuerys;

                            // Decide, what cache strategy to use:
                            // - If this is not the main view, chose
                            // FIRSTFRAMEONLY
                            // - If this is not a movie, chose FIRSTFRAMEONLY
                            // - If the image has been zoomed, chose
                            // CURRENTFRAMEFIRST
                            // - If the meta data is not complete yet, chose
                            // MISSINGFRAMESFIRST
                            // - In any other case, choose ALLFRAMESEQUALLY
                            CacheStrategy strategy;

                            if (!parentViewRef.isMainView || !parentImageRef.isMultiFrame()) {
                                strategy = CacheStrategy.CURRENTFRAMEONLY;
                            } else if (!((MovieView) parentViewRef).isMoviePlaying() && ((CachedMovieView) parentViewRef).getImageCacheStatus().getImageStatus(curLayer) != CacheStatus.COMPLETE) {
                                strategy = CacheStrategy.CURRENTFRAMEFIRST;
                            } else if (parentViewRef.getMetaData() instanceof HelioviewerMetaData && parentViewRef instanceof MovieView && ((MovieView) parentViewRef).getMaximumAccessibleFrameNumber() < num_layers - 1) {
                                strategy = CacheStrategy.MISSINGFRAMESFIRST;
                            } else {
                                strategy = CacheStrategy.ALLFRAMESEQUALLY;
                            }

                            // build query, based on strategy:
                            switch (strategy) {
                            case CURRENTFRAMEONLY:
                            case CURRENTFRAMEFIRST:
                                stepQuerys = new JPIPQuery[1];
                                stepQuerys[0] = createQuery(currParams, curLayer, curLayer);
                                current_step = 0;
                                break;

                            default:

                                int num_steps = num_layers / JPIPConstants.MAX_REQ_LAYERS;
                                if ((num_layers % JPIPConstants.MAX_REQ_LAYERS) != 0)
                                    num_steps++;

                                int lpf = 0,
                                lpi = 0;
                                stepQuerys = new JPIPQuery[num_steps];

                                // create queries for packages containing
                                // several frames
                                for (int i = 0; i < num_steps; i++) {
                                    lpf += JPIPConstants.MAX_REQ_LAYERS;
                                    if (lpf > layers.getEnd())
                                        lpf = layers.getEnd();

                                    stepQuerys[i] = createQuery(currParams, lpi, lpf);

                                    lpi = lpf + 1;
                                    if (lpi > layers.getEnd())
                                        lpi = layers.getStart();
                                }

                                // select current step based on strategy:
                                if (strategy == CacheStrategy.MISSINGFRAMESFIRST) {

                                    current_step = ((MovieView) parentViewRef).getMaximumAccessibleFrameNumber() / JPIPConstants.MAX_REQ_LAYERS;

                                } else {
                                    current_step = curLayer / JPIPConstants.MAX_REQ_LAYERS;
                                }
                            }

                            req = new JPIPRequest(HTTPRequest.Method.GET);

                            // long time = System.currentTimeMillis();

                            // send queries, until everything is complete or
                            // caching is interrupted
                            while ((complete_steps < stepQuerys.length) && !stopReading) {
                                if (current_step >= stepQuerys.length)
                                    current_step = 0;

                                // If query is already complete, to to next step
                                if (stepQuerys[current_step] == null) {
                                    current_step++;

                                    continue;
                                }

                                // Update requested package size
                                stepQuerys[current_step].setField(JPIPRequestField.LEN.toString(), String.valueOf(JpipRequestLen));

                                req.setQuery(stepQuerys[current_step].toString());
                                // Log.debug(stepQuerys[current_step].toString());
                                socket.send(req);
                                if (Boolean.parseBoolean(System.getProperty("export.movie.debug.on"))) {
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    throw new IOException();
                                }
                                // long start = System.currentTimeMillis();
                                res = socket.receive();
                                // if(iamPersistent)
                                // System.out.println(res.getResponseSize() /
                                // (System.currentTimeMillis() - start));

                                // receive data
                                if (res != null) {

                                    // Update optimal package size
                                    flowControl();

                                    // Downgrade, if necessary
                                    if (downgradeNecessary && res.getResponseSize() > 0 && parentViewRef.isMainView() && parentViewRef instanceof CachedMovieView) {

                                        ImageCacheStatus cacheStatus = ((CachedMovieView) parentViewRef).getImageCacheStatus();

                                        switch (strategy) {
                                        case CURRENTFRAMEONLY:
                                        case CURRENTFRAMEFIRST:
                                            for (int i = 0; i <= layers.getEnd(); i++) {
                                                cacheStatus.downgradeImageStatus(i);
                                            }
                                            break;

                                        default:
                                            for (int i = 0; i < stepQuerys.length; i++) {

                                                if (stepQuerys[i] == null) {
                                                    continue;
                                                }

                                                for (int j = i * JPIPConstants.MAX_REQ_LAYERS; j < Math.min((i + 1) * JPIPConstants.MAX_REQ_LAYERS, layers.getEnd() + 1); j++) {

                                                    cacheStatus.downgradeImageStatus(j);
                                                }
                                            }
                                        }

                                        downgradeNecessary = false;
                                    }

                                    // add response to cache - if query
                                    // complete, react
                                    if (cacheRef.addJPIPResponseData(res)) {

                                        // mark query as complete
                                        complete_steps++;
                                        stepQuerys[current_step] = null;

                                        // tell the cache status
                                        if (parentViewRef.isMainView() && parentViewRef instanceof CachedMovieView) {

                                            ImageCacheStatus cacheStatus = ((CachedMovieView) parentViewRef).getImageCacheStatus();

                                            switch (strategy) {
                                            case CURRENTFRAMEONLY:
                                            case CURRENTFRAMEFIRST:
                                                cacheStatus.setImageStatus(curLayer, CacheStatus.COMPLETE);
                                                break;

                                            default:
                                                for (int j = Math.min((current_step + 1) * JPIPConstants.MAX_REQ_LAYERS, layers.getEnd() + 1) - 1; j >= current_step * JPIPConstants.MAX_REQ_LAYERS; j--) {

                                                    cacheStatus.setImageStatus(j, CacheStatus.COMPLETE);
                                                }
                                            }
                                        }
                                    }

                                    // Fire ChangeEvent, if wanted
                                    if ((parentViewRef.getReaderMode() == ReaderMode.ONLYFIREONCOMPLETE && stepQuerys[current_step] == null) || parentViewRef.getReaderMode() == ReaderMode.ALWAYSFIREONNEWDATA) {

                                        // if package belongs to current frame,
                                        // tell the render-thread
                                        switch (strategy) {
                                        case CURRENTFRAMEONLY:
                                        case CURRENTFRAMEFIRST:
                                            parentViewRef.renderRequestedSignal.signal(RenderReasons.NEW_DATA);
                                            break;
                                        default:
                                            if (curLayer / JPIPConstants.MAX_REQ_LAYERS == current_step) {
                                                parentViewRef.renderRequestedSignal.signal(RenderReasons.NEW_DATA);
                                            }
                                        }
                                    }
                                }

                                // select next query, based on strategy
                                switch (strategy) {
                                case MISSINGFRAMESFIRST:

                                    int metaStatus = ((CachedMovieView) parentViewRef).getDateTimeCache().getMetaStatus();

                                    if (metaStatus >= Math.min((current_step + 1) * JPIPConstants.MAX_REQ_LAYERS, layers.getEnd())) {

                                        // if(metaStatus < num_layers-1) {
                                        // float loadPerSecond =
                                        // (float)metaStatus /
                                        // (System.currentTimeMillis() - time) *
                                        // 1000.0f;
                                        // int remainingFrames = num_layers -
                                        // metaStatus;
                                        // float remainingTime = remainingFrames
                                        // / loadPerSecond;
                                        // float playTime = (num_layers -
                                        // currParams.compositionLayer) / 20.0f;
                                        // System.out.println(remainingTime +
                                        // " " + playTime);
                                        // if(playTime >= remainingTime) {
                                        // ((CachedMovieView)
                                        // parentViewRef).playMovie();
                                        // } else {
                                        // ((CachedMovieView)
                                        // parentViewRef).pauseMovie();
                                        // }
                                        // }

                                        current_step++;
                                    }
                                    break;

                                case ALLFRAMESEQUALLY:
                                    current_step++;
                                    break;
                                default:
                                    break;
                                }

                                // let others do their work, too
                                Thread.yield();

                                // Check, whether caching has to be interrupted
                                if (parentViewRef.readerSignal.isSignaled() || Thread.interrupted()) {
                                    stopReading = true;
                                }
                            }

                            // Check, whether all queries are complete
                            complete = (complete_steps >= stepQuerys.length) && strategy != CacheStrategy.CURRENTFRAMEFIRST;

                            // If current frame first -> signal again, to go on
                            // reading
                            if (strategy == CacheStrategy.CURRENTFRAMEFIRST) {
                                parentViewRef.readerSignal.signal();
                            }
                        }

                    } catch (IOException e) {
                        if (verbose) {
                            Log.error(e.getMessage() + ": " + req.getMessageBody() + " " + req.getQuery());
                            e.printStackTrace();
                        }
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException ioe) {
                                Log.error(">> J2KReader.run() > Error closing socket", ioe);
                                if (ioe instanceof SocketException && ioe.getMessage().contains("Broken pipe")) {
                                    Message.err("Broken pipe error", "Broken pipe error! This error is a known bug. It occurs when too many movies with too many frames are loaded. Movie playback might not work or will be very slow. Try removing the current layers and load shorter movies or select a larger movie cadence. We are sorry for this inconvenience and are working on the problem.", false);
                                }
                            }
                        }
                        parentViewRef.fireChangeEvent(new ChangeEvent(new ReaderErrorReason(parentViewRef, e)));

                        // Send signal to try again
                        parentViewRef.readerSignal.signal();

                    } catch (JHV_KduException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
