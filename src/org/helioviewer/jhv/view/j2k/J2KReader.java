package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.app.Log;
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

    private synchronized void queueIfEmpty(J2KParams.Read params) {
        if (!isAbolished)
            signalQueue.offer(params); // newer pending work takes precedence
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

    @SuppressWarnings("try")
    private boolean readFrame(J2KSource.Remote source, int frame, int level, String query) throws KduException, IOException {
        String key = cacheKey[frame];
        boolean complete;
        try (J2KSource.Use ignored = source.use()) {
            AtomicBoolean status = source.getFrameStatus(frame, level);
            if (status != null && status.get())
                return true;

            JPIPCache cache = source.cache();
            JPIPCacheManager.Entry entry = key == null ? null : JPIPCacheManager.get(key, level);
            if (entry == null) {
                JPIPResponse response = socket.request(query, cache, frame);
                complete = response.isResponseComplete();
                if (complete && key != null)
                    JPIPCacheManager.store(key, level, cache, frame);
            } else {
                cache.put(frame, entry.stream());
                level = entry.level();
                complete = true;
            }
        }
        if (complete)
            source.setFrameComplete(frame, level);
        else
            source.setFramePartial(frame);
        return complete;
    }

    private boolean readingInterrupted() {
        return !signalQueue.isEmpty() || Thread.interrupted();
    }

    // Finish the selected frame before spending bandwidth on the rest of the movie.
    private boolean readPriorityFrame(J2KParams.Read params, String size) throws KduException, IOException {
        J2KParams.Decode decode = params.decodeParams();
        String query = JPIPSocket.createLayerQuery(decode.frame, size);
        while (true) {
            boolean complete = readFrame(params.source(), decode.frame, decode.level, query);
            if (complete)
                params.view().refreshDecodeFromReader(decode, params.viewpoint());
            UITimer.completionChanged();
            if (readingInterrupted())
                return false;
            if (complete)
                return true;
        }
    }

    // Advance even after partial responses so the movie becomes usable progressively.
    private boolean prefetchMovie(J2KParams.Read params, String size) throws KduException, IOException {
        J2KSource.Remote source = params.source();
        J2KParams.Decode decode = params.decodeParams();
        String[] queries = new String[cacheKey.length];
        for (int frame = 0; frame < queries.length; frame++)
            queries[frame] = JPIPSocket.createLayerQuery(frame, size);

        int partial = source.getPartialUntil();
        int frame = partial < queries.length - 1 ? partial : decode.frame;
        int remaining = queries.length;
        while (remaining > 0) {
            if (frame >= queries.length)
                frame = 0;
            if (queries[frame] == null) {
                frame++;
                continue;
            }
            if (readFrame(source, frame, decode.level, queries[frame])) {
                queries[frame] = null;
                remaining--;
            }
            UITimer.completionChanged();
            frame++;
            if (readingInterrupted())
                return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings("try")
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
            J2KParams.Decode decodeParams = params.decodeParams();

            int frame = decodeParams.frame;
            int level = decodeParams.level;
            ResolutionSet.Level resLevel = source.resolutionSet(frame).getLevel(level);
            int width = resLevel.width();
            int height = resLevel.height();

            view.setDownloading(true);

            try {
                if (socket.isClosed()) {
                    // System.out.println(">>> reconnect");
                    try (J2KSource.Use ignored = source.use()) {
                        socket = new JPIPSocket(uri, source.cache());
                    }
                }

                boolean singleFrame = cacheKey.length <= 1 || params.priority();
                String size = width + "," + height;
                boolean finished = singleFrame ? readPriorityFrame(params, size) : prefetchMovie(params, size);

                view.setDownloading(false);

                // suicide if fully done
                if (source.isComplete(0)) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {}
                    return;
                }
                // if single frame & not interrupted & incomplete -> signal again to go on reading
                if (singleFrame && finished && !source.isComplete(level)) {
                    queueIfEmpty(new J2KParams.Read(params.view(), params.source(), params.decodeParams(), params.viewpoint(), false));
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
                    queueIfEmpty(params); // retry unless newer work is pending
                else
                    Log.error("Retry limit reached: " + uri); // something may be terribly wrong
            }
        }
    }

    private int retries = 0;
}
