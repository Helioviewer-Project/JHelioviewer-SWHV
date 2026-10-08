package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

class J2KReader implements Runnable {

    private static final int FAILED = Integer.MAX_VALUE;

    // A request in flight; own tells whether it was sized by the frame's own header.
    private record Sent(int frame, boolean own) {}

    private final ArrayBlockingQueue<J2KParams.Read> signalQueue = new ArrayBlockingQueue<>(1);
    private final URI uri;
    private final J2KSource source;
    private final J2KNative client;
    private final Thread myThread;
    private final long[] stream;
    // Per frame: the coarsest level the server ended without completing, FAILED for a frame given up, else -1.
    private final int[] stuck;

    private volatile boolean isAbolished;
    private volatile JPIPSocket socket;
    private String[] cacheKey;
    private int retries;

    J2KReader(URI _uri, J2KSource _source) throws IOException {
        uri = _uri;
        source = _source;
        client = _source.client();

        try {
            connect();
            source.loadFrames();
            stream = new long[source.frames()];
            for (int i = 0; i < stream.length; i++)
                stream[i] = client.frame(i).stream();
            prime();
        } catch (Exception e) {
            closeSocket();
            throw new IOException("Error in the server communication: " + e.getMessage(), e);
        }
        stuck = new int[stream.length];
        Arrays.fill(stuck, -1);

        // Virtual-thread interruption also closes a socket still inside its constructor.
        myThread = Thread.ofVirtual().name("Reader " + uri).unstarted(this);
    }

    // Start only after the view has finished parsing metadata and building cache keys.
    void start(String[] _cacheKey) {
        cacheKey = _cacheKey;
        myThread.start();
    }

    // runs in abolish thread
    void stop() {
        synchronized (this) {
            if (isAbolished)
                return;
            isAbolished = true;
        }

        closeSocket(); // also release the connection if initialization failed before start()
        while (myThread.isAlive()) {
            try {
                closeSocket();
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

    // Interrupts pending I/O without writing another request.
    private void closeSocket() {
        JPIPSocket current = socket;
        if (current != null) {
            try {
                current.abort();
            } catch (IOException e) {
                Log.error("Error closing JPIPSocket", e);
            }
        }
    }

    private static boolean isComplete(int reason) throws IOException {
        return switch (reason) {
            case 1, 2 -> true; // image done, window done
            case 4, 7 -> false; // byte limit, response limit
            default -> throw new IOException("Unexpected JPIP end of response: " + reason);
        };
    }

    // A new channel and its metadata.
    private void connect() throws IOException {
        JPIPSocket opened = new JPIPSocket(uri);
        socket = opened;
        client.response(opened.receive());
        do {
            opened.sendMetadata();
        } while (!isComplete(client.response(opened.receive())));
    }

    private void fetchFirst(int width, int height, int pad) throws IOException {
        do {
            socket.sendFrame(stream[0], width, height, pad);
        } while (!isComplete(client.response(socket.receive())));
        source.update(0);
    }

    // First open: a coarse level of the first frame.
    private void prime() throws IOException {
        fetchFirst(64, 64, 0);
        ResolutionSet set = source.geometry(0);
        if (set != null && !set.isDisplayable()) { // the window did not cover a level
            ResolutionSet.Level res = set.getClosestLevel(64, 64);
            // Pad rsiz for the classical esajpip issue documented beside request()'s pad below.
            fetchFirst(res.width(), res.height(), 1);
        }
        if (set == null || !set.isDisplayable())
            throw new IOException("The server did not send a complete level of the first frame");
    }

    // No request can make the frame more complete at the level this size needs.
    private boolean isSettled(int frame, int width, int height) {
        ResolutionSet set = source.geometry(frame);
        if (set == null)
            return stuck[frame] == FAILED;
        int level = set.getNextLevel(width, height).level();
        return stuck[frame] >= level || set.getComplete(level);
    }

    private boolean isSettled(int width, int height) {
        for (int i = 0; i < stuck.length; i++) {
            if (!isSettled(i, width, height))
                return false;
        }
        return true;
    }

    private void update(int frame) {
        try {
            source.update(frame);
        } catch (IOException e) {
            fail(frame, e.getMessage());
        }
    }

    private void fail(int frame, String reason) {
        if (stuck[frame] != FAILED)
            Log.error(uri + ": frame " + frame + " abandoned: " + reason);
        stuck[frame] = FAILED;
    }

    // Imports the frame's disk entry if it is complete at the level or finer.
    private boolean restore(int frame, int level) {
        String key = cacheKey[frame];
        JPIPCacheManager.Entry entry = key == null ? null : JPIPCacheManager.get(key, level);
        if (entry == null)
            return false;
        try {
            client.importFrame(frame, entry.block());
        } catch (IOException e) { // refused: the source is unchanged
            JPIPCacheManager.remove(key);
            return false;
        }
        update(frame);
        retries = 0;
        return true;
    }

    // Sends the request a frame needs in this pass; null when it needs none.
    @Nullable
    private Sent request(int frame, ResolutionSet.Level wanted) throws IOException {
        int width = wanted.width(), height = wanted.height();
        if (isSettled(frame, width, height))
            return null;

        // Any entry will do while the frame's own levels are unknown.
        ResolutionSet set = source.geometry(frame);
        if (restore(frame, set == null ? Integer.MAX_VALUE : set.getNextLevel(width, height).level())) {
            if (isSettled(frame, width, height))
                return null;
            set = source.geometry(frame);
        }

        // The signalled frame's size while the header is unknown; else the frame's own, the region padded.
        ResolutionSet.Level res = set == null ? wanted : set.getNextLevel(width, height);
        // Classical esajpip server issue: precinct selection can omit the last precinct
        // when the last image coordinate is exactly a precinct boundary. Request rsiz=W+1,H+1
        // while keeping fsiz=W,H. The new server clips rsiz to the image. Pad only exact
        // frame dimensions: padding an approximate window can break the classical server.
        // Details are in client/CLASSICAL.md in the esajpip repository.
        // Classical servers also lack the full-frame default for omitted rsiz.
        // TODO: Once all supported servers handle omitted rsiz as a full frame, remove
        // rsiz and the pad argument from JPIPSocket.sendFrame(), including prime()'s padding.
        int pad = set == null ? 0 : 1;
        socket.sendFrame(stream[frame], res.width(), res.height(), pad);
        return new Sent(frame, set != null);
    }

    // Feeds the next response to the source. False when its frame needs another request in this pass.
    private boolean receive(Sent sent, ResolutionSet.Level wanted) throws IOException {
        int frame = sent.frame;
        boolean complete = isComplete(client.response(socket.receive()));
        update(frame);

        ResolutionSet set = source.geometry(frame);
        if (set == null) {
            if (complete)
                fail(frame, "no header in a complete response");
            return stuck[frame] == FAILED;
        }

        int level = set.getNextLevel(wanted.width(), wanted.height()).level();
        if (set.getComplete(level)) {
            retries = 0;
            String key = cacheKey[frame];
            if (key != null)
                JPIPCacheManager.store(key, level, () -> client.exportFrame(frame));
            return true;
        }
        if (!complete || !sent.own) // cut by the response limit, or sized by another frame
            return false;

        Log.warn(uri + ": frame " + frame + " is incomplete at level " + level + " after a complete response");
        stuck[frame] = Math.max(stuck[frame], level);
        return true;
    }

    private boolean readFrames(J2KParams.Read params, ResolutionSet.Level wanted, boolean singleFrame) throws IOException {
        J2KParams.Decode decode = params.decodeParams();
        ArrayDeque<Integer> remaining = new ArrayDeque<>();
        if (singleFrame) {
            remaining.add(decode.frame());
        } else {
            int partial = source.getPartialUntil();
            int first = partial < cacheKey.length - 1 ? partial : decode.frame();
            for (int i = 0; i < cacheKey.length; i++)
                remaining.add((first + i) % cacheKey.length);
        }

        // The size stays fixed until all sent responses have been consumed.
        ArrayDeque<Sent> sent = new ArrayDeque<>();
        int limit = singleFrame ? 1 : 2;
        boolean draining = false;
        while (true) {
            // On newer work, drain sent responses without issuing any more requests.
            draining |= isAbolished || !signalQueue.isEmpty() || Thread.interrupted();
            if (!draining && sent.size() < limit && !remaining.isEmpty()) {
                Sent request = request(remaining.removeFirst(), wanted);
                if (request != null) {
                    sent.addLast(request);
                    continue;
                }
            } else if (!sent.isEmpty()) {
                Sent first = sent.removeFirst();
                if (!receive(first, wanted)) {
                    remaining.addFirst(first.frame); // finish this frame first
                    continue;
                }
            } else {
                return !draining;
            }

            if (singleFrame)
                params.view().refreshDecodeFromReader(decode);
            UITimer.completionChanged();
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
            J2KParams.Decode decode = params.decodeParams();
            ResolutionSet.Level wanted = source.resolutionSet(decode.frame()).getLevel(decode.level());

            view.setDownloading(true);

            try {
                if (retries > 1)
                    Thread.sleep(1000);
                if (socket.isClosed())
                    connect();

                boolean singleFrame = cacheKey.length <= 1 || params.priority();
                boolean finished = readFrames(params, wanted, singleFrame);

                // Do not discard a queued display request when the download finishes.
                if (signalQueue.isEmpty() && source.isComplete(0)) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {}
                    return;
                }
                // if single frame & not interrupted & incomplete -> signal again to go on reading
                if (singleFrame && finished && !isSettled(wanted.width(), wanted.height())) {
                    queueIfEmpty(new J2KParams.Read(view, decode, false));
                }
                // retry limit applies to consecutive failures only
                retries = 0;
            } catch (InterruptedException ignore) {
                return; // Only stop() interrupts the reader.
            } catch (J2KNative.Refused e) {
                // The source takes no more data; what is complete stays viewable.
                Log.error(uri + ": " + e.getMessage());
                closeSocket();
                for (String key : cacheKey) {
                    if (key != null)
                        JPIPCacheManager.remove(key);
                }
                return;
            } catch (Exception e) {
                closeSocket();

                if (retries++ < 13)
                    queueIfEmpty(params); // retry unless newer work is pending
                else
                    Log.error("Retry limit reached: " + uri, e);
            } finally {
                view.setDownloading(false);
            }
        }
    }

}
