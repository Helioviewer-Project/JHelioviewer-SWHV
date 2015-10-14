package org.helioviewer.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.SocketException;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.concurrency.BooleanSignal;
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
    volatile boolean stop;

    /** A reference to the JP2Image this object is owned by. */
    private final JP2Image parentImageRef;

    /** A reference to the JP2ImageView this object is owned by. */
    private final JP2View parentViewRef;

    /** The JPIPSocket used to connect to the server. */
    private JPIPSocket socket;

    /** The a reference to the cache object used by the run method. */
    private final JHV_Kdu_cache cacheRef;

    /**
     * The time when the last response was received. It is used for performing
     * the flow control. A negative value means that there is not a previous
     * valid response to take into account.
     */
    private long lastResponseTime = -1;

    /** The current length in bytes to use for requests */
    private int JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

    private final ImageCacheStatus cacheStatusRef;

    private final BooleanSignal readerSignal = new BooleanSignal(false);

    /**
     * The constructor. Creates and connects the socket if image is remote.
     *
     * @param _imageViewRef
     * @throws IOException
     * @throws JHV_KduException
     */
    J2KReader(JP2View _imageViewRef, JP2Image _jp2ImageRef) throws IOException, JHV_KduException {
        parentViewRef = _imageViewRef;
        parentImageRef = _jp2ImageRef;

        cacheRef = parentImageRef.getCacheRef();
        cacheStatusRef = parentImageRef.getImageCacheStatus();

        socket = parentImageRef.getSocket();
        if (socket == null) {
            socket = new JPIPSocket();
            JPIPResponse res = (JPIPResponse) socket.connect(parentImageRef.getURI());
            cacheRef.addJPIPResponseData(res, cacheStatusRef);
            MoviePanel.cacheStatusChanged();
        }

        myThread = null;
        stop = false;
    }

    /** Starts the J2KReader thread. */
    void start() {
        if (myThread != null)
            stop();
        myThread = new Thread(this, "J2KReader " + parentImageRef.getName(0));
        stop = false;
        myThread.start();
    }

    /** Stops the J2KReader thread. */
    private void stop() {
        if (myThread != null && myThread.isAlive()) {
            try {
                do {
                    if (socket != null) { // try to unblock i/o
                        socket.close();
                    }
                    myThread.interrupt();
                    myThread.join(100);
                } while (myThread.isAlive());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                myThread = null;
            }
        }
    }

    /** Releases the resources associated with this object. */
    void abolish() {
        stop = true;
        stop();
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method perfoms the flow control, that is, adjusts dynamically the
     * value of the variable <code>JPIP_REQUEST_LEN</code>. The used algorithm
     * is the same as the used one by the viewer kdu_show of Kakadu
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

        Rectangle resDims = currParams.resolution.getResolutionBounds();
        query.setField(JPIPRequestField.FSIZ.toString(), String.valueOf(resDims.width) + "," + String.valueOf(resDims.height) + "," + "closest");
        query.setField(JPIPRequestField.ROFF.toString(), String.valueOf(currParams.subImage.x) + "," + String.valueOf(currParams.subImage.y));
        query.setField(JPIPRequestField.RSIZ.toString(), String.valueOf(currParams.subImage.width) + "," + String.valueOf(currParams.subImage.height));

        return query;
    }

    private boolean isConnected() {
        return (socket != null && socket.isConnected());
    }

    private void signalRender() {
        if (stop)
            return;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                parentImageRef.somethingWasActuallyRead = true;
                parentViewRef.signalRenderFromReader(parentImageRef);
            }
        });
    }

    protected void signalReader(JP2ImageParameter params) {
        readerSignal.signal(params);
    }

    @Override
    public void run() {
        JPIPRequest req = null;

        boolean complete = false;
        boolean viewChanged = false;
        boolean downgradeNecessary = false;

        JP2ImageParameter prevParams = null;
        JP2ImageParameter currParams = null;

        while (!stop) {
            prevParams = currParams;
            // wait for signal
            try {
                currParams = readerSignal.waitForSignal();
            } catch (InterruptedException e) {
                continue;
            }

            ReaderMode readerMode = parentImageRef.getReaderMode();
            if (readerMode == ReaderMode.NEVERFIRE) {
                // nothing
            } else if (readerMode == ReaderMode.SIGNAL_RENDER_ONCE) {
                parentImageRef.setReaderMode(ReaderMode.NEVERFIRE);
                signalRender();
            } else {
                // check whether view parameters have changed
                viewChanged = prevParams == null || !(currParams.subImage.equals(prevParams.subImage) && currParams.resolution.equals(prevParams.resolution));

                // if view has changed downgrade caching status
                if (viewChanged) {
                    complete = false;
                    downgradeNecessary = true;
                }

                // if socket is closed, but communication is necessary, open it
                if (socket != null && socket.isClosed()) {
                    try {
                        socket = new JPIPSocket();
                        socket.connect(parentImageRef.getURI());
                    } catch (IOException e) {
                        if (verbose) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException ioe) {
                            Log.error(">> J2KReader.run() > Error closing socket", ioe);
                        }
                        // Send signal to try again
                        readerSignal.signal(currParams);
                    }
                }

                // if socket is open, get image data
                if (socket != null && !socket.isClosed()) {
                    try {
                        // if nothing to do, check whether there are some queries left
                        // (actually, I do not know when this might happen...)
                        if (complete) {
                            // contrary to the above comment, last query was spuriously resent
                            req = null;
                            /*
                             * if (req != null && req.getQuery() != null) {
                             * socket.send(req); socket.receive(); }
                             */
                            // keep socket open as we may need more data
                            //socket.close();

                            // requesting data
                        } else {
                            JPIPResponse res = null;
                            boolean stopReading = false;
                            int curLayer = currParams.compositionLayer;

                            int num_layers = parentImageRef.getMaximumFrameNumber() + 1;

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

                            if (!parentImageRef.isMultiFrame()) {
                                strategy = CacheStrategy.CURRENTFRAMEONLY;
                            } else if (!Layers.isMoviePlaying() /*! */ && cacheStatusRef.getImageStatus(curLayer) != CacheStatus.COMPLETE) {
                                strategy = CacheStrategy.CURRENTFRAMEFIRST;
                            } else if (cacheStatusRef.getImageCachedPartiallyUntil() < num_layers - 1) {
                                strategy = CacheStrategy.MISSINGFRAMESFIRST;
                            } else {
                                strategy = CacheStrategy.ALLFRAMESEQUALLY;
                            }

                            // build query based on strategy
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
                                lpi = 0,
                                max_layers = num_layers - 1;
                                stepQuerys = new JPIPQuery[num_steps];

                                // create queries for packages containing several frames
                                for (int i = 0; i < num_steps; i++) {
                                    lpf += JPIPConstants.MAX_REQ_LAYERS;
                                    if (lpf > max_layers)
                                        lpf = max_layers;

                                    stepQuerys[i] = createQuery(currParams, lpi, lpf);

                                    lpi = lpf + 1;
                                    if (lpi > max_layers)
                                        lpi = 0;
                                }
                                // select current step based on strategy
                                if (strategy == CacheStrategy.MISSINGFRAMESFIRST) {
                                    current_step = cacheStatusRef.getImageCachedPartiallyUntil() / JPIPConstants.MAX_REQ_LAYERS;
                                } else {
                                    current_step = curLayer / JPIPConstants.MAX_REQ_LAYERS;
                                }
                            }

                            req = new JPIPRequest(HTTPRequest.Method.GET);

                            // long time = System.currentTimeMillis();

                            // send queries until everything is complete or caching is interrupted
                            while ((complete_steps < stepQuerys.length) && !stopReading) {
                                if (current_step >= stepQuerys.length)
                                    current_step = 0;

                                // if query is already complete, go to next step
                                if (stepQuerys[current_step] == null) {
                                    current_step++;
                                    continue;
                                }

                                // update requested package size
                                stepQuerys[current_step].setField(JPIPRequestField.LEN.toString(), String.valueOf(JpipRequestLen));

                                req.setQuery(stepQuerys[current_step]);
                                // Log.debug(stepQuerys[current_step].toString());
                                socket.send(req);

                                // long start = System.currentTimeMillis();
                                res = socket.receive();
                                // System.out.println(res.getResponseSize() /
                                // (System.currentTimeMillis() - start));

                                // receive data
                                if (res != null) {
                                    // update optimal package size
                                    flowControl();

                                    // downgrade if necessary
                                    if (downgradeNecessary && res.getResponseSize() > 0) {
                                        switch (strategy) {
                                        case CURRENTFRAMEONLY:
                                        case CURRENTFRAMEFIRST:
                                            for (int i = 0; i < num_layers; i++) {
                                                cacheStatusRef.downgradeImageStatus(i);
                                            }
                                            break;

                                        default:
                                            for (int i = 0; i < stepQuerys.length; i++) {
                                                if (stepQuerys[i] == null) {
                                                    continue;
                                                }
                                                for (int j = i * JPIPConstants.MAX_REQ_LAYERS; j < Math.min((i + 1) * JPIPConstants.MAX_REQ_LAYERS, num_layers); j++) {
                                                    cacheStatusRef.downgradeImageStatus(j);
                                                }
                                            }
                                        }
                                        MoviePanel.cacheStatusChanged();

                                        downgradeNecessary = false;
                                    }

                                    // add response to cache - react if query complete
                                    if (cacheRef.addJPIPResponseData(res, cacheStatusRef)) {
                                        // mark query as complete
                                        complete_steps++;
                                        stepQuerys[current_step] = null;

                                        // tell the cache status
                                        switch (strategy) {
                                        case CURRENTFRAMEONLY:
                                        case CURRENTFRAMEFIRST:
                                            cacheStatusRef.setImageStatus(curLayer, CacheStatus.COMPLETE);
                                            break;

                                        default:
                                            for (int j = Math.min((current_step + 1) * JPIPConstants.MAX_REQ_LAYERS, num_layers) - 1; j >= current_step * JPIPConstants.MAX_REQ_LAYERS; j--) {
                                                cacheStatusRef.setImageStatus(j, CacheStatus.COMPLETE);
                                            }
                                        }
                                    }
                                    MoviePanel.cacheStatusChanged();

                                    if ((readerMode == ReaderMode.ONLYFIREONCOMPLETE && stepQuerys[current_step] == null) || readerMode == ReaderMode.ALWAYSFIREONNEWDATA) {
                                        // if package belongs to current frame tell the render-thread
                                        switch (strategy) {
                                        case CURRENTFRAMEONLY:
                                        case CURRENTFRAMEFIRST:
                                            signalRender();
                                            break;
                                        default:
                                            /*! not good for on the fly resolution update
                                                if (curLayer / JPIPConstants.MAX_REQ_LAYERS == current_step) {
                                                signalRender();
                                            } */
                                        }
                                    }
                                }

                                // select next query based on strategy
                                switch (strategy) {
                                case MISSINGFRAMESFIRST:
                                    current_step++;
                                    break;
                                case ALLFRAMESEQUALLY:
                                    current_step++;
                                    break;
                                default:
                                    break;
                                }

                                // let others do their work, too
                                Thread.yield();

                                // check whether caching has to be interrupted
                                if (readerSignal.isSignaled() || Thread.interrupted()) {
                                    stopReading = true;
                                }
                            }

                            // check whether all queries are complete
                            complete = (complete_steps >= stepQuerys.length) && strategy != CacheStrategy.CURRENTFRAMEFIRST;
                            // if current frame first -> signal again, to go on reading
                            if (strategy == CacheStrategy.CURRENTFRAMEFIRST) {
                                readerSignal.signal(currParams);
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
                        // send signal to try again
                        readerSignal.signal(currParams);
                    } catch (JHV_KduException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
