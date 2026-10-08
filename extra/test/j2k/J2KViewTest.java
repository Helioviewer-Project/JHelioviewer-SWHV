package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.EDTTimer;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

// The real view and reader thread on a JPIP movie, headless.
// Arguments: Kakadu library, bridge library, JPIP URI of a movie.
public final class J2KViewTest {

    private static final LinkedBlockingQueue<View.ImageData> images = new LinkedBlockingQueue<>();
    private static final LatestWorker<DecodedImage> worker = new LatestWorker<>("Test-Decoder");

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        System.load(arguments[0]);
        System.load(arguments[1]);
        URI uri = URI.create(arguments[2]);

        Field timerField = Player.class.getDeclaredField("movieTimer");
        timerField.setAccessible(true);
        EDTTimer playback = (EDTTimer) timerField.get(null);

        // Paused, first shown at its coarsest level, which opening already made complete: the movie still downloads.
        J2KView view = open(uri);
        try {
            int frames = view.getMaximumFrameNumber() + 1;
            if (frames < 4)
                throw new AssertionError("Expected a movie with at least four frames");
            show(view, 0, Integer.MAX_VALUE);
            await(view, frames - 1);
            checkRefresh(view);
            // A finer level of the shown frame is fetched and delivered without another request to decode.
            ResolutionSet.Level finer = view.getResolutionLevel(0, 3);
            show(view, 0, 3);
            View.ImageData image;
            do {
                image = images.poll(60, TimeUnit.SECONDS);
                if (image == null)
                    throw new AssertionError("Finer level was not delivered");
                boolean ready = image.imageBuffer().width == finer.width() && image.imageBuffer().height == finer.height();
                EventQueue.invokeAndWait(image.image()::close);
                if (ready)
                    break;
            } while (true);
        } finally {
            close(view);
        }

        // Playing: the whole movie at one level, undisturbed and with the connection lost once.
        EventQueue.invokeAndWait(() -> {
            playback.setInitialDelay(Integer.MAX_VALUE); // playing, without advancing the timeline
            playback.start();
        });
        try {
            String[] reference = play(uri, false);
            if (!Arrays.equals(reference, play(uri, true)))
                throw new AssertionError("Movie pixels differ after a lost connection");
        } finally {
            EventQueue.invokeAndWait(playback::stop);
        }
        System.out.println("PASS: download start, latest refresh viewpoint, detached refresh, priority refresh, playing download and connection recovery");
        System.exit(0);
    }

    private static void checkRefresh(J2KView view) throws Exception {
        ResolutionSet.Level size = view.getResolutionLevel(0, Integer.MAX_VALUE);
        MetaData metadata = view.getMetaData(view.getFrameTime(0));
        Position latest = Position.toFixedDistance(metadata.getViewpoint(), metadata.getViewpoint().distance);
        double scale = size.height() / metadata.getPhysicalRegion().height;
        clearImages();
        EventQueue.invokeAndWait(() -> view.decode(latest, scale, null));
        View.ImageData image = images.poll(60, TimeUnit.SECONDS);
        if (image == null || image.viewpoint() != latest)
            throw new AssertionError("Latest request viewpoint was not delivered");
        EventQueue.invokeAndWait(image.image()::close);

        clearImages();
        EventQueue.invokeAndWait(() -> view.refreshDecodeFromReader(new J2KParams.Decode(0, size.level())));
        image = images.poll(60, TimeUnit.SECONDS);
        if (image == null || image.viewpoint() != latest)
            throw new AssertionError("Reader refresh used an obsolete viewpoint");
        EventQueue.invokeAndWait(image.image()::close);

        // The replacement view uses the same worker. An old view's cached refresh
        // must not invalidate its decode or drop its pending task.
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Boolean> fresh = new CompletableFuture<>();
        try {
            EventQueue.invokeAndWait(() -> {
                view.setDataHandler(null);
                worker.submit(() -> {
                    release.await();
                    return null;
                }, (result, current) -> fresh.complete(current));
                view.refreshDecodeFromReader(new J2KParams.Decode(0, size.level()));
            });
            EventQueue.invokeAndWait(() -> {}); // the queued refresh has run
            release.countDown();
            if (!fresh.get(10, TimeUnit.SECONDS))
                throw new AssertionError("Detached refresh invalidated the replacement decode");
        } finally {
            release.countDown();
            EventQueue.invokeAndWait(() -> view.setDataHandler(J2KViewTest::queueImage));
        }
    }

    private static String[] play(URI uri, boolean drop) throws Exception {
        J2KView view = open(uri);
        try {
            int frames = view.getMaximumFrameNumber() + 1;
            show(view, 0, 4);
            if (drop) {
                await(view, frames / 3);
                Field readerField = J2KView.class.getDeclaredField("reader");
                readerField.setAccessible(true);
                Object reader = readerField.get(view);
                Field socketField = J2KReader.class.getDeclaredField("socket");
                socketField.setAccessible(true);
                ((JPIPSocket) socketField.get(reader)).abort();
            }
            long deadline = System.nanoTime() + 120_000_000_000L;
            while (!view.isComplete()) {
                if (System.nanoTime() > deadline)
                    throw new AssertionError("Movie did not complete");
                Thread.sleep(5);
            }

            String[] hashes = new String[frames];
            for (int frame = 0; frame < frames; frame++) {
                ResolutionSet.Level size = view.getResolutionLevel(frame, 4);
                show(view, frame, 4);
                View.ImageData image = images.poll(60, TimeUnit.SECONDS);
                if (image == null || image.imageBuffer().width != size.width() || image.imageBuffer().height != size.height())
                    throw new AssertionError("Complete frame " + frame + " was not delivered at its level");
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(((ByteBuffer) image.imageBuffer().buffer).duplicate());
                hashes[frame] = HexFormat.of().formatHex(hash.digest());
                EventQueue.invokeAndWait(image.image()::close);
            }
            return hashes;
        } finally {
            close(view);
        }
    }

    private static J2KView open(URI uri) throws Exception {
        Constructor<DataUri> constructor = DataUri.class.getDeclaredConstructor(URI.class, URI.class, File.class);
        constructor.setAccessible(true);
        J2KView view = new J2KView(worker, null, constructor.newInstance(uri, uri, null), new ImageProcessingSettings(() -> {}));
        EventQueue.invokeAndWait(() -> view.setDataHandler(J2KViewTest::queueImage));
        return view;
    }

    private static void close(J2KView view) throws Exception {
        EventQueue.invokeAndWait(() -> {
            view.setDataHandler(null);
            view.abolish();
        });
        clearImages();
    }

    private static void queueImage(View.ImageData image) {
        image.image().retain();
        images.add(image);
    }

    private static void clearImages() throws Exception {
        EventQueue.invokeAndWait(() -> {
            View.ImageData image;
            while ((image = images.poll()) != null)
                image.image().close();
        });
    }

    // Asks for a frame at the level a display of that level's height would want.
    private static void show(J2KView view, int frame, int level) throws Exception {
        MetaData metadata = view.getMetaData(view.getFrameTime(frame));
        double scale = view.getResolutionLevel(frame, level).height() / metadata.getPhysicalRegion().height;
        AtomicBoolean selected = new AtomicBoolean();
        clearImages();
        EventQueue.invokeAndWait(() -> {
            selected.set(view.setNearestFrame(view.getFrameTime(frame)));
            if (selected.get())
                view.decode(metadata.getViewpoint(), scale, null);
        });
        if (!selected.get())
            throw new AssertionError("Frame " + frame + " cannot be selected");
    }

    private static void await(J2KView view, int frame) throws Exception {
        long deadline = System.nanoTime() + 120_000_000_000L;
        while (view.getFrameCompletion(frame) == null) {
            if (System.nanoTime() > deadline)
                throw new AssertionError("Frame " + frame + " did not become displayable");
            Thread.sleep(1);
        }
    }

    private J2KViewTest() {}
}
