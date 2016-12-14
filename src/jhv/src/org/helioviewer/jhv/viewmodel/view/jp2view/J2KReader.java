package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;

class J2KReader implements Runnable {

    // Whether IOExceptions should be shown on System.err or not
    private static final boolean verbose = false;

    // The thread that this object runs on
    private final Thread myThread;

    // A boolean flag used for stopping the thread
    private volatile boolean stop;

    // A reference to the JP2Image this object is owned by
    private final JP2Image parentImageRef;

    // A reference to the JP2View this object is owned by
    private final JP2View parentViewRef;

    /// The JPIPSocket used to connect to the server
    private JPIPSocket socket;

    // The a reference to the cache object used by the run method
    private final JHV_Kdu_cache cacheRef;

    /**
     * The time when the last response was received. It is used for performing
     * the flow control. A negative value means that there is not a previous
     * valid response to take into account.
     */
    private long lastResponseTime = -1;

    /** The current length in bytes to use for requests */
    private int jpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

    private final JP2ImageCacheStatus cacheStatusRef;

    private final BooleanSignal readerSignal = new BooleanSignal(false);

    private final int num_layers;

    J2KReader(JP2View _imageViewRef, JP2Image _jp2ImageRef) throws IOException {
        parentViewRef = _imageViewRef;
        parentImageRef = _jp2ImageRef;

        num_layers = parentImageRef.getMaximumFrameNumber() + 1;
        cacheRef = parentImageRef.getReaderCache();
        cacheStatusRef = parentImageRef.getImageCacheStatus();

        if ((socket = parentImageRef.getSocket()) == null)
            reconnect();

        stop = false;
        myThread = new Thread(this, "Reader " + parentImageRef.getName());
        myThread.setDaemon(true);
        myThread.start();
    }

    private void reconnect() throws IOException {
        // System.out.println(">>> reconnect");
        socket = new JPIPSocket(parentImageRef.getURI(), cacheRef, cacheStatusRef);
    }

    // runs in abolish thread
    void abolish() {
        stop = true;

        if (myThread == null) // paranoia
            return;

        while (myThread.isAlive()) {
            try {
                if (socket != null)
                    socket.close(); // try to unblock i/o
                myThread.interrupt();
                myThread.join(100);
            } catch (Exception e) { // avoid exit from loop
                e.printStackTrace();
            }
        }
    }

    private void signalRender(double factor) {
        if (stop)
            return;
        EventQueue.invokeLater(() -> parentViewRef.signalRenderFromReader(parentImageRef, factor));
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

        if ((receivedBytes - jpipRequestLen) < (jpipRequestLen >> 1) && receivedBytes > (jpipRequestLen >> 1)) {
            long tdat = replyDataTime - replyTextTime;
            if (tdat > 10000)
                adjust = -1;
            else if (lastResponseTime > 0) {
                long tgap = replyTextTime - lastResponseTime;

                if ((tgap + tdat) < 1000)
                    adjust = +2;
                else {
                    double gapRatio = tgap / (double) (tgap + tdat);
                    double targetRatio = (tdat + tgap) / 10000.;

                    if (gapRatio > targetRatio)
                        adjust = +2;
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

    private static String createQuery(String fSiz, int iniLayer, int endLayer) {
        return JPIPQuery.create("context", "jpxl<" + iniLayer + '-' + endLayer + '>', "fsiz", fSiz + ",closest", "rsiz", fSiz, "roff", "0,0");
    }

    private static String[] createSingleQuery(JP2ImageParameter currParams) {
        String fSiz = Integer.toString(currParams.resolution.width) + ',' + Integer.toString(currParams.resolution.height);
        return new String[] { createQuery(fSiz, currParams.compositionLayer, currParams.compositionLayer) };
    }

    private String[] createMultiQuery(JP2ImageParameter currParams) {
        int num_steps = num_layers / JPIPConstants.MAX_REQ_LAYERS;
        if ((num_layers % JPIPConstants.MAX_REQ_LAYERS) != 0)
            num_steps++;

        String[] stepQuerys = new String[num_steps];
        String fSiz = Integer.toString(currParams.resolution.width) + ',' + Integer.toString(currParams.resolution.height);

        int lpf = 0, lpi = 0, max_layers = num_layers - 1;
        for (int i = 0; i < num_steps; i++) {
            lpf += JPIPConstants.MAX_REQ_LAYERS;
            if (lpf > max_layers)
                lpf = max_layers;

            stepQuerys[i] = createQuery(fSiz, lpi, lpf);

            lpi = lpf + 1;
            if (lpi > max_layers)
                lpi = 0;
        }
        return stepQuerys;
    }

    @Override
    public void run() {
        boolean complete = false;

        while (!stop) {
            JP2ImageParameter currParams;
            // wait for signal
            try {
                currParams = readerSignal.waitForSignal();
            } catch (InterruptedException e) {
                continue;
            }

            if (!complete || currParams.downgrade) {
                try {
                    if (socket.isClosed())
                        reconnect();

                    int frame = currParams.compositionLayer;
                    int level = currParams.resolution.level;

                    // choose cache strategy
                    boolean singleFrame = false;
                    if (num_layers <= 1 /* one frame */ ||
                       (!Layers.isMoviePlaying() /*! */ && cacheStatusRef.getImageStatus(frame) != CacheStatus.COMPLETE)) {
                        singleFrame = true;
                    }

                    // build query based on strategy
                    int current_step;
                    String[] stepQuerys;
                    if (singleFrame) {
                        stepQuerys = createSingleQuery(currParams);
                        current_step = 0;
                    } else {
                        stepQuerys = createMultiQuery(currParams);

                        int partial = cacheStatusRef.getImageCachedPartiallyUntil();
                        if (partial < num_layers - 1)
                            current_step = partial / JPIPConstants.MAX_REQ_LAYERS;
                        else
                            current_step = frame / JPIPConstants.MAX_REQ_LAYERS;
                    }

                    lastResponseTime = -1;

                    // send queries until everything is complete or caching is interrupted
                    int complete_steps = 0;
                    boolean stopReading = false;
                    while (!stopReading && complete_steps < stepQuerys.length) {
                        if (current_step >= stepQuerys.length)
                            current_step = 0;

                        // if query is already complete, go to next step
                        if (stepQuerys[current_step] == null) {
                            current_step++;
                            continue;
                        }

                        // update requested package size
                        socket.send(stepQuerys[current_step] + "len=" + jpipRequestLen);
                        // receive and add data to cache
                        JPIPResponse res = socket.receive(cacheRef, cacheStatusRef);
                        // update optimal package size
                        flowControl();
                        // react if query complete
                        if (res.isResponseComplete()) {
                            // mark query as complete
                            complete_steps++;
                            stepQuerys[current_step] = null;

                            // tell the cache status
                            if (singleFrame) {
                                cacheStatusRef.setImageStatus(frame, level, CacheStatus.COMPLETE);
                                signalRender(currParams.factor);
                            } else {
                                for (int j = current_step * JPIPConstants.MAX_REQ_LAYERS; j < Math.min((current_step + 1) * JPIPConstants.MAX_REQ_LAYERS, num_layers); j++)
                                    cacheStatusRef.setImageStatus(j, level, CacheStatus.COMPLETE);
                            }
                        }
                        MoviePanel.cacheStatusChanged();

                        // select next query based on strategy
                        if (!singleFrame)
                            current_step++;

                        // check whether caching has to be interrupted
                        if (readerSignal.isSignaled() || Thread.interrupted()) {
                            stopReading = true;
                        }
                    }

                    complete = cacheStatusRef.levelComplete(level);
                    // if incomplete && not interrupted && single frame -> signal again to go on reading
                    if (!complete && !stopReading && singleFrame) {
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
                }
            }
        }
    }

}
