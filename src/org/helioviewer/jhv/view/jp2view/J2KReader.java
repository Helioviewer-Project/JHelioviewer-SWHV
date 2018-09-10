package org.helioviewer.jhv.view.jp2view;

import java.io.IOException;

import kdu_jni.KduException;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.jp2view.cache.CacheStatus;
import org.helioviewer.jhv.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.io.jpip.DatabinMap;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPCache;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPStream;

class J2KReader implements Runnable {

    private final BooleanSignal readerSignal = new BooleanSignal(false);

    private final J2KView view;
    private final JPIPCache cache;
    private final Thread myThread;

    // A boolean flag used for stopping the thread
    private volatile boolean isAbolished;

    private JPIPSocket socket;

    J2KReader(J2KView _view) throws KduException, IOException {
        view = _view;

        cache = view.getJPIPCache();
        socket = new JPIPSocket(view.getURI(), cache);
        initJPIP();

        myThread = new Thread(this, "Reader " + view.getName());
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
                e.printStackTrace();
            }
        }
    }

    void signalReader(ImageParams params) {
        readerSignal.signal(params);
    }

    private static final int mainHeaderKlass = DatabinMap.getKlass(JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS);

    private void initJPIP() throws IOException {
        try {
            JPIPResponse res;
            String req = JPIPQuery.create(JPIPConstants.META_REQUEST_LEN, "stream", "0", "metareq", "[*]!!");
            do {
                res = socket.send(req, cache, 0);
            } while (!res.isResponseComplete());

            // prime first image
            req = JPIPQuery.create(JPIPConstants.MAX_REQUEST_LEN, "stream", "0", "fsiz", "64,64,closest", "rsiz", "64,64", "roff", "0,0");
            do {
                res = socket.send(req, cache, 0);
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
                Log.error("J2KReader.initJPIP() > Error closing socket.", e);
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
        int numFrames = view.getMaximumFrameNumber() + 1;
        CacheStatus cacheStatus = view.getCacheStatus();

        while (!isAbolished) {
            ImageParams params;
            // wait for signal
            try {
                view.setDownloading(false);
                params = readerSignal.waitForSignal();
                view.setDownloading(true);
            } catch (InterruptedException e) {
                continue;
            }

            try {
                if (socket.isClosed()) {
                    // System.out.println(">>> reconnect");
                    socket = new JPIPSocket(view.getURI(), cache);
                }

                int frame = params.decodeParams.frame;
                int level = params.decodeParams.resolution.level;

                // choose cache strategy
                boolean singleFrame = false;
                if (numFrames <= 1 /* one frame */ || params.priority) {
                    singleFrame = true;
                }

                // build query based on strategy
                int currentStep;
                String[] stepQuerys;
                String fSiz = params.decodeParams.resolution.width + "," + params.decodeParams.resolution.height;
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

                    // receive and add data to cache
                    long key = view.getCacheKey(currentStep);
                    JPIPResponse res = null;
                    JPIPStream stream = JPIPCacheManager.get(key, level);
                    if (stream != null)
                        cache.put(currentStep, stream);
                    else
                        res = socket.send(stepQuerys[currentStep], cache, currentStep);

                    //if (res == null)
                    //    System.out.println(">> hit " + view.getURI() + " " + currentStep + " " + level);

                    // react if query complete
                    if (res == null || res.isResponseComplete()) {
                        // mark query as complete
                        completeSteps++;
                        stepQuerys[currentStep] = null;

                        if (res != null && res.isResponseComplete() && (stream = cache.get(currentStep)) != null) // downloaded
                            JPIPCacheManager.put(key, level, stream);
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
                // suicide if fully done
                if (cacheStatus.isComplete(0)) {
                    view.setDownloading(false);
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
            } catch (KduException | IOException e) {
                // e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ioe) {
                    Log.error("J2KReader.run() > Error closing socket", ioe);
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
