package org.helioviewer.jhv.view.jp2view;

import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.view.jp2view.cache.CacheStatus;
import org.helioviewer.jhv.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.view.jp2view.kakadu.JHV_Kdu_cache;

class J2KReader implements Runnable {

    // The thread that this object runs on
    private final Thread myThread;

    // A boolean flag used for stopping the thread
    private volatile boolean isAbolished;

    // A reference to the JP2View this object is owned by
    private final JP2View viewRef;

    // The a reference to the cache object used by the run method
    private final JHV_Kdu_cache cacheRef;

    private final CacheStatus cacheStatusRef;

    private JPIPSocket socket;

    private final BooleanSignal readerSignal = new BooleanSignal(false);

    private final int numFrames;

    J2KReader(JP2View _viewRef) {
        viewRef = _viewRef;

        numFrames = viewRef.getMaximumFrameNumber() + 1;
        cacheRef = viewRef.getReaderCache();
        cacheStatusRef = viewRef.getCacheStatus();
        socket = viewRef.getSocket();

        myThread = new Thread(this, "Reader " + viewRef.getName());
        myThread.setDaemon(true);
        myThread.start();
    }

    // runs in abolish thread
    void abolish() {
        if (isAbolished)
            return;
        isAbolished = true;

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

    void signalReader(ImageParams params) {
        readerSignal.signal(params);
    }

    private static String createQuery(String fSiz, int iniLayer, int endLayer) {
        return JPIPQuery.create(JPIPConstants.MAX_REQUEST_LEN, "context", "jpxl<" + iniLayer + '-' + endLayer + '>', "fsiz", fSiz + ",closest", "rsiz", fSiz, "roff", "0,0");
    }

    private String[] createMultiQuery(String fSiz) {
        int numSteps = numFrames / JPIPConstants.MAX_REQ_LAYERS;
        if ((numFrames % JPIPConstants.MAX_REQ_LAYERS) != 0)
            numSteps++;

        String[] stepQuerys = new String[numSteps];
        int lpf = -1, lpi = 0, maxFrame = numFrames - 1;
        for (int i = 0; i < numSteps; i++) {
            lpf += JPIPConstants.MAX_REQ_LAYERS;
            if (lpf > maxFrame)
                lpf = maxFrame;

            stepQuerys[i] = createQuery(fSiz, lpi, lpf);

            lpi = lpf + 1;
            if (lpi > maxFrame)
                lpi = 0;
        }
        return stepQuerys;
    }

    @Override
    public void run() {
        while (!isAbolished) {
            ImageParams params;
            // wait for signal
            try {
                viewRef.setDownloading(false);
                params = readerSignal.waitForSignal();
                viewRef.setDownloading(true);
            } catch (InterruptedException e) {
                continue;
            }

            try {
                if (socket.isClosed()) {
                    // System.out.println(">>> reconnect");
                    socket = new JPIPSocket(viewRef.getURI(), cacheRef);
                }

                int frame = params.frame;
                int level = params.resolution.level;

                // choose cache strategy
                boolean singleFrame = false;
                if (numFrames <= 1 /* one frame */ || params.priority) {
                    singleFrame = true;
                }

                // build query based on strategy
                int currentStep;
                String[] stepQuerys;
                String fSiz = params.resolution.width + "," + params.resolution.height;
                if (singleFrame) {
                    stepQuerys = new String[] { createQuery(fSiz, frame, frame) };
                    currentStep = 0;
                } else {
                    stepQuerys = createMultiQuery(fSiz);

                    int partial = cacheStatusRef.getPartialUntil();
                    if (partial < numFrames - 1)
                        currentStep = partial / JPIPConstants.MAX_REQ_LAYERS;
                    else
                        currentStep = frame / JPIPConstants.MAX_REQ_LAYERS;
                }

                // send queries until everything is complete or caching is interrupted
                int completeSteps = 0;
                boolean stopReading = false;
                while (!stopReading && completeSteps < stepQuerys.length) {
                    if (currentStep >= stepQuerys.length)
                        currentStep = 0;

                    // if query is already complete, go to next step
                    if (stepQuerys[currentStep] == null) {
                        currentStep++;
                        continue;
                    }

                    // receive and add data to cache
                    JPIPResponse res = socket.send(stepQuerys[currentStep], cacheRef);
                    // react if query complete
                    if (res.isResponseComplete()) {
                        // mark query as complete
                        completeSteps++;
                        stepQuerys[currentStep] = null;

                        // tell the cache status
                        if (singleFrame) {
                            cacheStatusRef.setFrameComplete(frame, level);
                            viewRef.signalRenderFromReader(params); // refresh current image
                        } else {
                            for (int j = currentStep * JPIPConstants.MAX_REQ_LAYERS; j < Math.min((currentStep + 1) * JPIPConstants.MAX_REQ_LAYERS, numFrames); j++) {
                                cacheStatusRef.setFrameComplete(j, level);
                            }
                        }
                    } else {
                        // tell the cache status
                        if (singleFrame) {
                            cacheStatusRef.setFramePartial(frame);
                        } else {
                            for (int j = currentStep * JPIPConstants.MAX_REQ_LAYERS; j < Math.min((currentStep + 1) * JPIPConstants.MAX_REQ_LAYERS, numFrames); j++) {
                                cacheStatusRef.setFramePartial(j);
                            }
                        }
                    }

                    MoviePanel.cacheStatusChanged();

                    // select next query based on strategy
                    if (!singleFrame)
                        currentStep++;

                    // check whether caching has to be interrupted
                    if (readerSignal.isSignaled() || Thread.interrupted()) {
                        stopReading = true;
                    }
                }
                // suicide if fully done
                if (cacheStatusRef.isComplete(0)) {
                    viewRef.setDownloading(false);
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                    return;
                }

                // if single frame & not interrupted & incomplete -> signal again to go on reading
                if (singleFrame && !stopReading && !cacheStatusRef.isComplete(level)) {
                    params.priority = false;
                    readerSignal.signal(params);
                }
             } catch (IOException e) {
                // e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ioe) {
                    Log.error("J2KReader.run() > Error closing socket", ioe);
                }

                if (retries++ < 13)
                    readerSignal.signal(params); // signal to retry
                else
                    Log.error("Retry limit reached: " + viewRef.getURI()); // something may be terribly wrong
            }
        }
    }

    private int retries = 0;

}
