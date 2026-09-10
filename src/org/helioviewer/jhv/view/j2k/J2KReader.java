package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCache;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

import kdu_jni.KduException;

class J2KReader implements Runnable {

    private final ArrayBlockingQueue<J2KParams.Read> signalQueue = new ArrayBlockingQueue<>(1);
    private final URI uri;
    private final Thread myThread;

    private volatile boolean isAbolished;
    private volatile JPIPSocket socket;
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

        // Virtual-thread interruption also closes a socket still inside its constructor.
        myThread = Thread.ofVirtual().name("Reader " + uri).unstarted(this);
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
                JPIPSocket currentSocket = socket;
                if (currentSocket != null)
                    currentSocket.abort();
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
                socket.abort();
            } catch (IOException e) {
                Log.error(e);
            }
            socket = null;
        }
    }

    @SuppressWarnings("try")
    private boolean restoreFrame(J2KSource.Remote source, int frame, int level) throws KduException {
        try (J2KSource.Use ignored = source.use()) {
            AtomicBoolean status = source.getFrameStatus(frame, level);
            if (status != null && status.get())
                return true;

            String key = cacheKey[frame];
            JPIPCacheManager.Entry entry = key == null ? null : JPIPCacheManager.get(key, level);
            if (entry == null)
                return false;
            source.cache().put(frame, entry.stream());
            level = entry.level();
        }
        source.setFrameComplete(frame, level);
        return true;
    }

    @SuppressWarnings("try")
    private JPIPSocket.FrameResponse receiveFrame(J2KSource.Remote source, int level) throws KduException, IOException {
        JPIPSocket.FrameResponse response;
        try (J2KSource.Use ignored = source.use()) {
            JPIPCache cache = source.cache();
            response = socket.receiveFrame(cache);
            String key = cacheKey[response.frame()];
            if (response.complete() && key != null)
                JPIPCacheManager.store(key, level, cache, response.frame());
        }
        if (response.complete())
            source.setFrameComplete(response.frame(), level);
        else
            source.setFramePartial(response.frame());
        return response;
    }

    private boolean readingInterrupted() {
        return isAbolished || !signalQueue.isEmpty() || Thread.interrupted();
    }

    private boolean readFrames(J2KParams.Read params, String size, boolean singleFrame) throws KduException, IOException {
        J2KSource.Remote source = params.source();
        J2KParams.Decode decode = params.decodeParams();
        ArrayDeque<Integer> remaining = new ArrayDeque<>();
        if (singleFrame) {
            remaining.add(decode.frame);
        } else {
            int partial = source.getPartialUntil();
            int first = partial < cacheKey.length - 1 ? partial : decode.frame;
            for (int i = 0; i < cacheKey.length; i++)
                remaining.add((first + i) % cacheKey.length);
        }

        // The source and resolution stay fixed until all sent responses have been consumed.
        int limit = singleFrame ? 1 : 2;
        boolean draining = false;
        while (true) {
            // On newer work, drain sent responses without issuing any more requests.
            draining |= readingInterrupted();
            int frame;
            boolean complete;
            if (!draining && socket.pendingCount() < limit && !remaining.isEmpty()) {
                frame = remaining.removeFirst();
                if (!restoreFrame(source, frame, decode.level)) {
                    socket.sendFrame(frame, size);
                    continue;
                }
                complete = true;
            } else if (socket.pendingCount() > 0) {
                JPIPSocket.FrameResponse response = receiveFrame(source, decode.level);
                frame = response.frame();
                complete = response.complete();
            } else {
                return !draining;
            }

            if (!complete)
                remaining.addLast(frame); // Revisit partial frames after the rest of the movie.
            else if (singleFrame)
                params.view().refreshDecodeFromReader(decode, params.viewpoint());
            UITimer.completionChanged();
        }
    }

    @Override
    @SuppressWarnings("try")
    public void run() {
        int retries = 0;
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
                boolean finished = readFrames(params, size, singleFrame);

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
                try {
                    socket.abort();
                } catch (IOException ioe) {
                    Log.error("Error closing JPIPSocket", ioe);
                }

                if (retries++ < 13)
                    queueIfEmpty(params); // retry unless newer work is pending
                else
                    Log.error("Retry limit reached: " + uri); // something may be terribly wrong
            } finally {
                view.setDownloading(false);
            }
        }
    }

}
