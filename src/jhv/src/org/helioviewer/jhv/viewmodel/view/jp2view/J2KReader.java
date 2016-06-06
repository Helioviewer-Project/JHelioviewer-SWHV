package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.jhv.viewmodel.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequestField;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;

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
        CURRENTFRAMEFIRST, MISSINGFRAMESFIRST, ALLFRAMESEQUALLY
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
    private int jpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

    private final ImageCacheStatus cacheStatusRef;

    private final BooleanSignal readerSignal = new BooleanSignal(false);

    private final int num_layers;

    J2KReader(JP2View _imageViewRef, JP2Image _jp2ImageRef) throws IOException {
        parentViewRef = _imageViewRef;
        parentImageRef = _jp2ImageRef;

        num_layers = parentImageRef.getMaximumFrameNumber() + 1;
        cacheRef = parentImageRef.getCacheRef();
        cacheStatusRef = parentImageRef.getImageCacheStatus();

        if ((socket = parentImageRef.getSocket()) == null)
            reconnect();

        myThread = null;
        stop = false;
    }

    private void reconnect() throws IOException {
        try {
            // System.out.println(">>> reconnect");
            socket = new JPIPSocket();
            JPIPResponse res = (JPIPResponse) socket.connect(parentImageRef.getURI());
            cacheRef.addJPIPResponseData(res, cacheStatusRef);
        } catch (JHV_KduException e) {
            e.printStackTrace();
        }
    }

    void start() {
        if (myThread != null)
            stop();
        myThread = new Thread(this, "Reader " + parentImageRef.getName());
        stop = false;
        myThread.start();
    }

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

    // Release the resources associated with this object
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

    private void signalRender(final double factor) {
        if (stop)
            return;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                parentViewRef.signalRenderFromReader(parentImageRef, factor);
            }
        });
    }

    void signalReader(JP2ImageParameter params) {
        readerSignal.signal(params);
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

        if ((receivedBytes - jpipRequestLen) < (jpipRequestLen >> 1) && receivedBytes > (jpipRequestLen >> 1)) {
            if (tdat > 10000)
                adjust = -1;
            else if (lastResponseTime > 0) {
                long tgap = replyTextTime - lastResponseTime;

                if ((tgap + tdat) < 1000)
                    adjust = +1;
                else {
                    double gapRatio = tgap / (double) (tgap + tdat);
                    double targetRatio = (tdat + tgap) / 40000.; // 10000.0;

                    if (gapRatio > targetRatio)
                        adjust = +1;
                    else
                        adjust = -1;
                }
            }
        }

        jpipRequestLen += (jpipRequestLen >> 2) * adjust;

        if (jpipRequestLen > JPIPConstants.MAX_REQUEST_LEN)
            jpipRequestLen = JPIPConstants.MAX_REQUEST_LEN;
        if (jpipRequestLen < JPIPConstants.MIN_REQUEST_LEN)
            jpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

        lastResponseTime = replyDataTime;
    }

    private JPIPQuery createQuery(JP2ImageParameter currParams, int iniLayer, int endLayer) {
        JPIPQuery query = new JPIPQuery();
        query.setField(JPIPRequestField.CONTEXT.toString(), "jpxl<" + iniLayer + "-" + endLayer + ">");
        query.setField(JPIPRequestField.FSIZ.toString(), Integer.toString(currParams.resolution.width) + "," + Integer.toString(currParams.resolution.height) + "," + "closest");
        query.setField(JPIPRequestField.ROFF.toString(), Integer.toString(currParams.subImage.x) + "," + Integer.toString(currParams.subImage.y));
        query.setField(JPIPRequestField.RSIZ.toString(), Integer.toString(currParams.subImage.width) + "," + Integer.toString(currParams.subImage.height));

        return query;
    }

    private JPIPQuery[] createSingleQuery(JP2ImageParameter currParams) {
        return new JPIPQuery[] { createQuery(currParams, currParams.compositionLayer, currParams.compositionLayer) };
    }

    private JPIPQuery[] createMultiQuery(JP2ImageParameter currParams) {
        int num_steps = num_layers / JPIPConstants.MAX_REQ_LAYERS;
        if ((num_layers % JPIPConstants.MAX_REQ_LAYERS) != 0)
            num_steps++;

        int lpf = 0, lpi = 0, max_layers = num_layers - 1;
        JPIPQuery[] stepQuerys = new JPIPQuery[num_steps];

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
        return stepQuerys;
    }

    @Override
    public void run() {
        boolean complete = false;
        boolean downgradeNecessary = false;
        JP2ImageParameter currParams = null;

        while (!stop) {
            JP2ImageParameter prevParams = currParams;
            // wait for signal
            try {
                currParams = readerSignal.waitForSignal();
            } catch (InterruptedException e) {
                continue;
            }

            // check whether view parameters have changed
            boolean viewChanged = prevParams == null || !(currParams.subImage.equals(prevParams.subImage) && currParams.resolution.equals(prevParams.resolution));
            // if view has changed downgrade caching status
            if (viewChanged) {
                complete = false;
                downgradeNecessary = true;
            }

            if (!complete) {
                try {
                    if (socket.isClosed())
                        reconnect();

                    boolean stopReading = false;
                    ReaderMode readerMode = parentImageRef.getReaderMode();

                    lastResponseTime = -1;

                    int complete_steps = 0;
                    int current_step;

                    // Decide what cache strategy to use:
                    // - If this is not the main view, choose FIRSTFRAMEONLY
                    // - If this is not a movie, choose FIRSTFRAMEONLY
                    // - If the image has been zoomed, choose CURRENTFRAMEFIRST
                    // - If the meta data is not complete yet, choose MISSINGFRAMESFIRST
                    // - In any other case, choose ALLFRAMESEQUALLY
                    CacheStrategy strategy;
                    if (num_layers <= 1 /* one frame */ ||
                       (!Layers.isMoviePlaying() /*! */ && cacheStatusRef.getImageStatus(currParams.compositionLayer) != CacheStatus.COMPLETE)) {
                        strategy = CacheStrategy.CURRENTFRAMEFIRST;
                    } else if (cacheStatusRef.getImageCachedPartiallyUntil() < num_layers - 1) {
                        strategy = CacheStrategy.MISSINGFRAMESFIRST;
                    } else {
                        strategy = CacheStrategy.ALLFRAMESEQUALLY;
                    }

                    // build query based on strategy
                    JPIPQuery[] stepQuerys;
                    switch (strategy) {
                    case CURRENTFRAMEFIRST:
                        stepQuerys = createSingleQuery(currParams);
                        current_step = 0;
                        break;
                    default:
                        stepQuerys = createMultiQuery(currParams);
                        // select current step based on strategy
                        if (strategy == CacheStrategy.MISSINGFRAMESFIRST) {
                            current_step = cacheStatusRef.getImageCachedPartiallyUntil() / JPIPConstants.MAX_REQ_LAYERS;
                        } else {
                            current_step = currParams.compositionLayer / JPIPConstants.MAX_REQ_LAYERS;
                        }
                    }

                    //int idx = 0;
                    JPIPRequest req = new JPIPRequest(HTTPRequest.Method.GET);
                    // send queries until everything is complete or caching is interrupted
                    while (!stopReading && complete_steps < stepQuerys.length) {
                        if (current_step >= stepQuerys.length)
                            current_step = 0;

                        // if query is already complete, go to next step
                        if (stepQuerys[current_step] == null) {
                            current_step++;
                            continue;
                        }

                        // update requested package size
                        stepQuerys[current_step].setField(JPIPRequestField.LEN.toString(), Integer.toString(jpipRequestLen));

                        req.setQuery(stepQuerys[current_step]);
                        // Log.debug(stepQuerys[current_step].toString());
                        socket.send(req);

                        // long start = System.currentTimeMillis();
                        JPIPResponse res = socket.receive();
                        // System.out.println(res.getResponseSize() / (System.currentTimeMillis() - start));

                        // receive data
                        if (res.getResponseSize() > 0) {
                            //System.out.println(">>> request " + (idx++) + " " + jpipRequestLen + " " + res.getResponseSize());
                            // update optimal package size
                            flowControl();

                            // downgrade if necessary
                            if (downgradeNecessary /*&& res.getResponseSize() > 0*/) {
                                switch (strategy) {
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
                                downgradeNecessary = false;
                            }

                            // add response to cache - react if query complete
                            if (cacheRef.addJPIPResponseData(res, cacheStatusRef)) {
                                // mark query as complete
                                complete_steps++;
                                stepQuerys[current_step] = null;

                                // tell the cache status
                                switch (strategy) {
                                case CURRENTFRAMEFIRST:
                                    cacheStatusRef.setImageStatus(currParams.compositionLayer, CacheStatus.COMPLETE);
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
                                if (strategy == CacheStrategy.CURRENTFRAMEFIRST)
                                    signalRender(currParams.factor);
                            }
                        }

                        // select next query based on strategy
                        if (strategy != CacheStrategy.CURRENTFRAMEFIRST)
                            current_step++;

                        // check whether caching has to be interrupted
                        if (readerSignal.isSignaled() || Thread.interrupted()) {
                            stopReading = true;
                        }
                    }

                    int completed = 0;
                    for (; completed < num_layers; completed++) {
                        if (cacheStatusRef.getImageStatus(completed) != CacheStatus.COMPLETE)
                            break;
                    }
                    complete = completed == num_layers;
                    //if (complete)
                    //   System.out.println(">> COMPLETE");

                    // if incomplete && not interrupted && current frame first -> signal again to go on reading
                    if (!complete && !stopReading && strategy == CacheStrategy.CURRENTFRAMEFIRST) {
                        readerSignal.signal(currParams);
                    }
                 } catch (IOException e) {
                    if (verbose) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException ioe) {
                        Log.error("J2KReader.run() > Error closing socket", ioe);
                    }
                    // Send signal to try again
                    readerSignal.signal(currParams);
                } catch (JHV_KduException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
