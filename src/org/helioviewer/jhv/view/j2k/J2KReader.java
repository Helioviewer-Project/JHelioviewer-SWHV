package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPResponse;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

import kdu_jni.KduException;

class J2KReader implements Runnable {

    private final ArrayBlockingQueue<J2KParams.Read> signalQueue = new ArrayBlockingQueue<>(1);
    private final URI uri;
    private final Thread myThread;

    private volatile boolean isAbolished;
    private JPIPSocket socket;
    private String[] cacheKey;

    J2KReader(URI _uri, J2KSource.Remote source) throws KduException, IOException {
        uri = _uri;

        JPIPCache cache = source.cache();
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

    void setCacheKey(String[] _cacheKey) {
        cacheKey = _cacheKey;
    }

    // runs in abolish thread
    void stop() {
        synchronized (this) {
            if (isAbolished)
                return;
            isAbolished = true;
        }

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

    synchronized void signal(J2KParams.Read params) {
        if (isAbolished) // ignore new work when we're closing down
            return;
        signalQueue.poll(); // latest wins
        signalQueue.offer(params);
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
        String[] stepQueries = new String[numFrames];
        for (int lpi = 0; lpi < numFrames; lpi++) {
            stepQueries[lpi] = JPIPSocket.createLayerQuery(lpi, fSiz);
        }
        return stepQueries;
    }

    private boolean readStep(J2KSource.Remote source, String query, String key, int level, int frame) throws KduException, IOException {
        if (!source.beginUse())
            throw new CancellationException("Read cancelled after source close");
        try {
            JPIPCache cache = source.cache();
            if (key != null && JPIPCacheManager.restore(key, level, cache, frame))
                return true;

            try (JPIPCacheManager.Writer writer = JPIPCacheManager.writer(key, level)) {
                JPIPResponse res = socket.request(query, frame, cache, writer);
                boolean complete = res.isResponseComplete();
                if (complete)
                    writer.commit();
                return complete;
            }
        } finally {
            source.endUse();
        }
    }

    @Override
    public void run() {
        while (!isAbolished) {
            J2KParams.Read params;
            // wait for signal
            try {
                params = signalQueue.take();
            } catch (InterruptedException e) {
                continue;
            }

            J2KView view = params.view();
            J2KSource.Remote source = params.source();

            int frame = params.decodeParams().frame();
            int level = params.decodeParams().level();
            ResolutionSet.Level resLevel = source.completionLevel().getResolutionSet(frame).getLevel(level);
            int width = resLevel.width();
            int height = resLevel.height();

            view.setDownloading(true);

            try {
                if (socket.isClosed()) {
                    // System.out.println(">>> reconnect");
                    if (!source.beginUse())
                        throw new CancellationException("Read cancelled after source close");
                    try {
                        socket = new JPIPSocket(uri, source.cache());
                    } finally {
                        source.endUse();
                    }
                }

                // choose cache strategy
                int numFrames = cacheKey.length;
                boolean singleFrame = numFrames <= 1 /* one frame */ || params.priority();

                // build query based on strategy
                int currentStep;
                String[] stepQueries;
                String fSiz = width + "," + height;
                if (singleFrame) {
                    stepQueries = new String[]{JPIPSocket.createLayerQuery(frame, fSiz)};
                    currentStep = frame;
                } else {
                    stepQueries = createMultiQuery(numFrames, fSiz);

                    int partial = source.completionLevel().getPartialUntil();
                    currentStep = partial < numFrames - 1 ? partial : frame;
                }

                // send queries until everything is complete or caching is interrupted
                int completeSteps = 0;
                boolean stopReading = false;
                while (!stopReading && completeSteps < stepQueries.length) {
                    if (currentStep >= stepQueries.length)
                        currentStep = 0;

                    // if query is already complete, go to next step
                    if (stepQueries[currentStep] == null) {
                        currentStep++;
                        continue;
                    }

                    boolean downloadComplete = readStep(source, stepQueries[currentStep], cacheKey[currentStep], level, currentStep);
                    if (downloadComplete) {
                        // mark query as complete
                        completeSteps++;
                        stepQueries[currentStep] = null;

                        source.completionLevel().setFrameComplete(source, currentStep, level); // tell the completion level
                        if (singleFrame)
                            view.refreshDecodeFromReader(params.decodeParams(), params.viewpoint()); // refresh current image
                    } else {
                        source.completionLevel().setFramePartial(source, currentStep); // tell the completion level
                    }

                    UITimer.completionChanged();

                    // select next query based on strategy
                    if (!singleFrame)
                        currentStep++;
                    // check whether caching has to be interrupted
                    if (!signalQueue.isEmpty() || Thread.interrupted()) {
                        stopReading = true;
                    }
                }

                view.setDownloading(false);

                // suicide if fully done
                if (source.completionLevel().isComplete(0)) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {}
                    return;
                }
                // if single frame & not interrupted & incomplete -> signal again to go on reading
                if (singleFrame && !stopReading && !source.completionLevel().isComplete(level)) {
                    signal(new J2KParams.Read(params.view(), params.source(), params.decodeParams(), params.viewpoint(), false));
                }
                // retry limit applies to consecutive failures only
                retries = 0;
            } catch (Exception e) {
                view.setDownloading(false);
                try {
                    socket.close();
                } catch (IOException ioe) {
                    Log.error("Error closing JPIPSocket", ioe);
                }

                if (retries++ < 13)
                    signal(params); // signal to retry
                else
                    Log.error("Retry limit reached: " + uri); // something may be terribly wrong
            }
        }
    }

    private int retries = 0;
}
