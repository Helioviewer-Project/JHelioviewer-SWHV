package org.helioviewer.jhv.view.jp2view;

import java.io.IOException;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.jp2view.cache.CacheStatus;
import org.helioviewer.jhv.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPCache;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPStream;

class J2KReader implements Runnable {

    // The thread that this object runs on
    private final Thread myThread;

    // A boolean flag used for stopping the thread
    private volatile boolean isAbolished;

    // A reference to the JP2View this object is owned by
    private final JP2View viewRef;

    private final JPIPCache cacheRef;
    private final CacheStatus cacheStatusRef;

    private JPIPSocket socket;

    private final BooleanSignal readerSignal = new BooleanSignal(false);

    private final int numFrames;

    J2KReader(JP2View _viewRef) {
        viewRef = _viewRef;

        numFrames = viewRef.getMaximumFrameNumber() + 1;
        cacheRef = viewRef.getJPIPCache();
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

    private static String createQuery(String fSiz, int layer) {
        return JPIPQuery.create(JPIPConstants.MAX_REQUEST_LEN, "stream", String.valueOf(layer), "fsiz", fSiz + ",closest", "rsiz", fSiz, "roff", "0,0");
    }

    private String[] createMultiQuery(String fSiz) {
        String[] stepQuerys = new String[numFrames];
        for (int lpi = 0; lpi < numFrames; lpi++) {
            stepQuerys[lpi] = createQuery(fSiz, lpi);
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
                    stepQuerys = new String[] { createQuery(fSiz, frame) };
                    currentStep = frame;
                } else {
                    stepQuerys = createMultiQuery(fSiz);

                    int partial = cacheStatusRef.getPartialUntil();
                    currentStep = partial < numFrames - 1 ? partial : frame;
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
                    long key = viewRef.getCacheKey(currentStep);
                    JPIPResponse res = null;
                    JPIPStream stream = JPIPCacheManager.get(key, level);
                    if (stream != null)
                        cacheRef.put(currentStep, stream);
                    else
                        res = socket.send(stepQuerys[currentStep], cacheRef, currentStep);

                    if (res == null)
                        System.out.println(">> hit " + viewRef.getURI() + " " + currentStep + " " + level);

                    // react if query complete
                    if (res == null || res.isResponseComplete()) {
                        // mark query as complete
                        completeSteps++;
                        stepQuerys[currentStep] = null;

                        if (res != null) // downloaded
                            JPIPCacheManager.put(key, level, cacheRef.get(currentStep));
                        cacheStatusRef.setFrameComplete(currentStep, level); // tell the cache status
                        if (singleFrame)
                            viewRef.signalRenderFromReader(params); // refresh current image
                    } else {
                        cacheStatusRef.setFramePartial(currentStep); // tell the cache status
                    }

                    UITimer.cacheStatusChanged();

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
