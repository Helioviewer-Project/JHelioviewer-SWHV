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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
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
        J2KNative.init();
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
            // A finer level of the shown frame is fetched and delivered without another request to decode.
            ResolutionSet.Level finer = view.getResolutionLevel(0, 3);
            show(view, 0, 3);
            View.ImageData image;
            do {
                image = images.poll(60, TimeUnit.SECONDS);
                if (image == null)
                    throw new AssertionError("Finer level was not delivered");
            } while (image.imageBuffer().width != finer.width() || image.imageBuffer().height != finer.height());
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
        System.out.println("PASS: download started by a first view at a complete level, priority refresh, playing download, recovery from a lost connection");
        System.exit(0);
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
        EventQueue.invokeAndWait(() -> view.setDataHandler(images::add));
        return view;
    }

    private static void close(J2KView view) throws Exception {
        EventQueue.invokeAndWait(() -> {
            view.setDataHandler(null);
            view.abolish();
        });
    }

    // Asks for a frame at the level a display of that level's height would want.
    private static void show(J2KView view, int frame, int level) throws Exception {
        MetaData metadata = view.getMetaData(view.getFrameTime(frame));
        double scale = view.getResolutionLevel(frame, level).height() / metadata.getPhysicalRegion().height;
        AtomicBoolean selected = new AtomicBoolean();
        images.clear();
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
