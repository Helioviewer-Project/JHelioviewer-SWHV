package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.source.ResolutionSet;

// Multi-file, duplicate, zip and failure loads through the layer loader, headless, on local FITS fixtures.
// Arguments: three FITS files with distinct times, a zip of two of them, a file that is not an image.
public final class FramesLoadTest {

    private static final LinkedBlockingQueue<ImageData> images = new LinkedBlockingQueue<>();

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        URI a = Path.of(arguments[0]).toUri(), b = Path.of(arguments[1]).toUri(), c = Path.of(arguments[2]).toUri();
        URI zip = Path.of(arguments[3]).toUri(), bogus = Path.of(arguments[4]).toUri();

        ImageLayerLoader.Result result = ImageLayerLoader.open(null, List.of(c, a, b));
        Frames frames = result.frames();
        try {
            if (frames.size() != 3 || !result.warnings().isEmpty() || result.baseName() != null)
                throw new AssertionError("Three files did not give three frames without warnings");
            for (int i = 1; i < 3; i++) {
                if (frames.time(i).milli <= frames.time(i - 1).milli)
                    throw new AssertionError("Frames not sorted by time");
            }
            decodeFirst(frames);
        } finally {
            EventQueue.invokeAndWait(frames::close);
        }
        System.out.println("PASS: three FITS files sorted into one timeline and decoded");

        result = ImageLayerLoader.open(null, List.of(a, b, a, c));
        frames = result.frames();
        try {
            if (frames.size() != 3 || result.warnings().size() != 1 || !result.warnings().getFirst().startsWith("Skipped 1 frame"))
                throw new AssertionError("Duplicate time was not dropped with one warning: " + result.warnings());
        } finally {
            EventQueue.invokeAndWait(frames::close);
        }
        System.out.println("PASS: duplicate time dropped with a warning");

        result = ImageLayerLoader.open(null, List.of(zip, c));
        frames = result.frames();
        try {
            if (frames.size() != 3 || !result.warnings().isEmpty())
                throw new AssertionError("Zip entries were not expanded into the timeline: " + frames.size() + " " + result.warnings());
        } finally {
            EventQueue.invokeAndWait(frames::close);
        }
        System.out.println("PASS: zip expanded alongside a file");

        result = ImageLayerLoader.open(null, List.of(a, bogus, b));
        frames = result.frames();
        try {
            if (frames.size() != 2 || result.warnings().size() != 1)
                throw new AssertionError("Unreadable file was not skipped with a warning: " + frames.size() + " " + result.warnings());
        } finally {
            EventQueue.invokeAndWait(frames::close);
        }
        System.out.println("PASS: unreadable file skipped with a warning in a multi-file load");

        try {
            ImageLayerLoader.open(null, List.of(bogus)).frames().close();
            throw new AssertionError("Single unreadable file loaded");
        } catch (Exception e) {
            if (!e.getMessage().contains(Path.of(arguments[4]).getFileName().toString()))
                throw new AssertionError("Failure does not name the file: " + e.getMessage(), e);
        }
        System.out.println("PASS: single unreadable file fails the load naming it");

        result = ImageLayerLoader.open(null, List.of(a));
        frames = result.frames();
        try {
            if (frames.size() != 1 || result.baseName() == null)
                throw new AssertionError("Single file did not keep its base name");
        } finally {
            EventQueue.invokeAndWait(frames::close);
        }
        System.out.println("PASS: single file keeps its base name");
        System.exit(0);
    }

    private static void decodeFirst(Frames frames) throws Exception {
        FrameDecoder decoder = new FrameDecoder(new ImageProcessingSettings(() -> {}), image -> {
            image.image().retain();
            images.add(image);
        });
        try {
            Frames.Frame first = frames.get(0);
            ResolutionSet.Level size = first.source().levels(0).getLevel(0);
            double scale = size.height() / first.metaData().getPhysicalRegion().height;
            EventQueue.invokeAndWait(() -> {
                decoder.reset(frames.serial());
                frames.select(frames.first());
                decoder.decode(frames, first.metaData().getViewpoint(), scale);
            });
            ImageData image = images.poll(60, TimeUnit.SECONDS);
            if (image == null || image.imageBuffer().width != size.width() || image.imageBuffer().height != size.height())
                throw new AssertionError("First frame was not decoded at its size");
            EventQueue.invokeAndWait(image.image()::release);
        } finally {
            EventQueue.invokeAndWait(decoder::dispose);
        }
    }

    private FramesLoadTest() {}
}
