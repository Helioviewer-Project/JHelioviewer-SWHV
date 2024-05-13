package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.net.URI;

import kdu_jni.KduException;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPResponse;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;
import org.helioviewer.jhv.view.j2k.jpip.JPIPStream;

class J2KReader implements Runnable {

    private final BooleanSignal readerSignal = new BooleanSignal();
    private final JPIPCache cache = new JPIPCache();
    private final URI uri;
    private final Thread myThread;

    private volatile boolean isAbolished;
    private JPIPSocket socket;

    J2KReader(URI _uri) throws KduException, IOException {
        uri = _uri;

        socket = new JPIPSocket(uri, cache);
        try {
            socket.init(cache);
        } catch (Exception e) {
            initCloseSocket();
            throw new IOException("Error in the server communication: " + e.getMessage(), e);
        }

        myThread = new Thread(this, "Reader " + uri);
        myThread.setDaemon(true);
        myThread.start();
    }

    JPIPCache getCache() {
        return cache;
    }

    // runs in abolish thread
    void abolish() throws KduException {
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
                Log.error(e);
            }
        }

        cache.Close();
        cache.Native_destroy();
    }

    void signal(J2KParams.Read params) {
        readerSignal.signal(params);
    }

    private void initCloseSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.error(e);
            }
            socket = null;
        }
    }

    private static String[] createMultiQuery(int numFrames, String fSiz) {
        String[] stepQuerys = new String[numFrames];
        for (int lpi = 0; lpi < numFrames; lpi++) {
            stepQuerys[lpi] = JPIPSocket.createLayerQuery(lpi, fSiz);
        }
        return stepQuerys;
    }

    @Override
    public void run() {
        while (!isAbolished) {
            J2KParams.Read params;
            // wait for signal
            try {
                params = readerSignal.waitForSignal();
            } catch (InterruptedException e) {
                continue;
            }

            J2KView view = params.view;
            CompletionLevel completionLevel = view.completionLevel();
            int numFrames = view.getMaximumFrameNumber() + 1;

            int frame = params.decodeParams.frame;
            int level = params.decodeParams.level;

            ResolutionSet.Level resLevel = view.getResolutionLevel(frame, level);
            int width = resLevel.width;
            int height = resLevel.height;

            view.setDownloading(true);

            try {
                if (socket.isClosed()) {
                    // System.out.println(">>> reconnect");
                    socket = new JPIPSocket(uri, cache);
                }
                // choose cache strategy
                boolean singleFrame = numFrames <= 1 /* one frame */ || params.priority;

                // build query based on strategy
                int currentStep;
                String[] stepQuerys;
                String fSiz = width + "," + height;
                if (singleFrame) {
                    stepQuerys = new String[]{JPIPSocket.createLayerQuery(frame, fSiz)};
                    currentStep = frame;
                } else {
                    stepQuerys = createMultiQuery(numFrames, fSiz);

                    int partial = completionLevel.getPartialUntil();
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

                    boolean downloadComplete = false;
                    {
                        String key = view.getCacheKey(currentStep);
                        JPIPStream stream = key == null ? null : JPIPCacheManager.get(key, level);
                        if (stream == null) { // not in JPIP cache
                            JPIPResponse res = socket.request(stepQuerys[currentStep], cache, currentStep);
                            if (res.isResponseComplete()) { // downloaded
                                downloadComplete = true;
                                if (key != null && (stream = cache.get(currentStep)) != null)
                                    JPIPCacheManager.put(key, level, stream);
                            }
                        } else {
                            downloadComplete = true;
                            cache.put(currentStep, stream);
                        }
                    }

                    if (downloadComplete) {
                        // mark query as complete
                        completeSteps++;
                        stepQuerys[currentStep] = null;

                        completionLevel.setFrameComplete(currentStep, level); // tell the completion level
                        if (singleFrame)
                            view.signalDecoderFromReader(params); // refresh current image
                    } else {
                        completionLevel.setFramePartial(currentStep); // tell the completion level
                    }

                    UITimer.completionChanged();

                    // select next query based on strategy
                    if (!singleFrame)
                        currentStep++;
                    // check whether caching has to be interrupted
                    if (readerSignal.isSignaled() || Thread.interrupted()) {
                        stopReading = true;
                    }
                }

                view.setDownloading(false);

                // suicide if fully done
                if (completionLevel.isComplete(0)) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                    return;
                }
                // if single frame & not interrupted & incomplete -> signal again to go on reading
                if (singleFrame && !stopReading && !completionLevel.isComplete(level)) {
                    params.priority = false;
                    readerSignal.signal(params);
                }
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    Log.error("Error closing JPIPSocket", ioe);
                }

                if (retries++ < 13)
                    readerSignal.signal(params); // signal to retry
                else
                    Log.error("Retry limit reached: " + uri); // something may be terribly wrong
            }
        }
    }

    private int retries = 0;

}
