package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.net.URI;

import kdu_jni.KduException;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.view.j2k.cache.CacheStatus;
import org.helioviewer.jhv.view.j2k.image.ReadParams;
import org.helioviewer.jhv.view.j2k.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.view.j2k.jpip.DatabinMap;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPConstants;
import org.helioviewer.jhv.view.j2k.jpip.JPIPQuery;
import org.helioviewer.jhv.view.j2k.jpip.JPIPResponse;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;
import org.helioviewer.jhv.view.j2k.jpip.JPIPStream;

class J2KReader implements Runnable {

    private final BooleanSignal readerSignal = new BooleanSignal();

    private final Thread myThread;
    private volatile boolean isAbolished;
    private JPIPSocket socket;

    J2KReader(URI uri, JPIPCache cache) throws KduException, IOException {
        socket = new JPIPSocket(uri, cache);
        initJPIP(cache);

        myThread = new Thread(this, "Reader " + uri);
        myThread.setDaemon(true);
    }

    void start() {
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
                Log.error(e);
            }
        }
    }

    void signalReader(ReadParams params) {
        readerSignal.signal(params);
    }

    private static final int mainHeaderKlass = DatabinMap.getKlass(JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS);

    private void initJPIP(JPIPCache cache) throws IOException {
        try {
            JPIPResponse res;
            String req = JPIPQuery.create(JPIPConstants.META_REQUEST_LEN, "stream", "0", "metareq", "[*]!!");
            do {
                res = socket.request(req, cache, 0);
            } while (!res.isResponseComplete());

            // prime first image
            req = JPIPQuery.create(JPIPConstants.MAX_REQUEST_LEN, "stream", "0", "fsiz", "64,64,closest", "rsiz", "64,64", "roff", "0,0");
            do {
                res = socket.request(req, cache, 0);
            } while (!res.isResponseComplete() && !cache.isDataBinCompleted(mainHeaderKlass, 0, 0));
        } catch (Exception e) {
            initCloseSocket();
            throw new IOException("Error in the server communication: " + e.getMessage(), e);
        }
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

    private static String createQuery(String fSiz, int layer) {
        return JPIPQuery.create(JPIPConstants.MAX_REQUEST_LEN, "stream", String.valueOf(layer), "fsiz", fSiz + ",closest", "rsiz", fSiz, "roff", "0,0");
    }

    private static String[] createMultiQuery(String fSiz, int numFrames) {
        String[] stepQuerys = new String[numFrames];
        for (int lpi = 0; lpi < numFrames; lpi++) {
            stepQuerys[lpi] = createQuery(fSiz, lpi);
        }
        return stepQuerys;
    }

    @Override
    public void run() {
        while (!isAbolished) {
            ReadParams params;
            // wait for signal
            try {
                params = readerSignal.waitForSignal();
            } catch (InterruptedException e) {
                continue;
            }

            J2KView view = params.view;
            JPIPCache cache = view.getJPIPCache();
            CacheStatus cacheStatus = view.getCacheStatus();
            int numFrames = view.getMaximumFrameNumber() + 1;

            int frame = params.decodeParams.frame;
            int level = params.decodeParams.level;

            ResolutionLevel resolution = view.getResolutionLevel(frame, level);
            int width = resolution.width;
            int height = resolution.height;

            view.setDownloading(true);

            try {
                if (socket.isClosed()) {
                    // System.out.println(">>> reconnect");
                    socket = new JPIPSocket(view.getURI(), cache);
                }
                // choose cache strategy
                boolean singleFrame = numFrames <= 1 /* one frame */ || params.priority;

                // build query based on strategy
                int currentStep;
                String[] stepQuerys;
                String fSiz = width + "," + height;
                if (singleFrame) {
                    stepQuerys = new String[]{createQuery(fSiz, frame)};
                    currentStep = frame;
                } else {
                    stepQuerys = createMultiQuery(fSiz, numFrames);

                    int partial = cacheStatus.getPartialUntil();
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

                        cacheStatus.setFrameComplete(view.getSource(), currentStep, level); // tell the cache status
                        if (singleFrame)
                            view.signalDecoderFromReader(params); // refresh current image
                    } else {
                        cacheStatus.setFramePartial(view.getSource(), currentStep); // tell the cache status
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

                view.setDownloading(false);

                // suicide if fully done
                if (cacheStatus.isComplete(0)) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                    return;
                }
                // if single frame & not interrupted & incomplete -> signal again to go on reading
                if (singleFrame && !stopReading && !cacheStatus.isComplete(level)) {
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
                    Log.error("Retry limit reached: " + view.getURI()); // something may be terribly wrong
            }
        }
    }

    private int retries = 0;

}
