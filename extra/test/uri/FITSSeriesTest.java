package org.helioviewer.jhv.layers;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.source.FITSSource;
import org.helioviewer.jhv.thread.UiThread;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

public final class FITSSeriesTest {

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        Path directory = Files.createTempDirectory("jhv-fits-series-");
        Path first = directory.resolve("first.fits");
        Path second = directory.resolve("second.fits");
        try {
            Header template;
            try (Fits fits = new Fits("extra/test/data/CCOR2_1A_20260902T211514_V00_NC.fits")) {
                template = fits.read()[1].getHeader();
            }
            write(first, template, "2026-09-02T21:15:14", 0);
            write(second, template, "2026-09-02T21:16:14", 20);
            for (int plane = 0; plane < 2; plane++) {
                ImageProcessingSettings.FITSSelection selection = new ImageProcessingSettings.FITSSelection(1, plane);
                ImageLayerLoader.Result result = ImageLayerLoader.open(null, List.of(second.toUri(), first.toUri()), selection);
                Frames frames = result.frames();
                try {
                    if (frames.size() != 2 || !result.preserveSettings() || result.uris().size() != 2)
                        throw new AssertionError("Series selection or frame count");
                    if (frames.lastTime().milli - frames.firstTime().milli != 60_000)
                        throw new AssertionError("Cube planes changed the series timestamps");
                    ImageProcessingSettings.FITSParameters settings = new ImageProcessingSettings(() -> {}).fitsParameters();
                    for (int i = 0; i < frames.size(); i++) {
                        Frames.Frame frame = frames.valueAt(i);
                        if (!(frame.source() instanceof FITSSource source) || !source.selection().equals(selection))
                            throw new AssertionError("Selection was not applied to every file");
                        ImageBuffer image = frame.source().decode(0, 0, ImageFilter.NONE, settings, null);
                        if (((ByteBuffer) image.buffer).get(0) != 3 + plane * 10 + i * 20)
                            throw new AssertionError("Series delivered wrong selected plane");
                    }
                } finally {
                    frames.close();
                }
            }
            checkSelectionReload(first, second);
            UiThread.postAndWait(FITSSeriesTest::checkPlayerReload);
        } finally {
            Files.deleteIfExists(first);
            Files.deleteIfExists(second);
            Files.delete(directory);
        }
        System.out.println("PASS FITS series selection, pixels, chronological order and timestamps");
    }

    private static void checkSelectionReload(Path first, Path second) throws Exception {
        LinkedBlockingQueue<ImageLayerLoader.Result> results = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Boolean> failures = new LinkedBlockingQueue<>();
        ImageLayerLoader loader = UiThread.postAndWait(() -> new ImageLayerLoader(results::add, () -> failures.add(true)));
        Frames previous = null;
        try {
            UiThread.postAndWait(() -> loader.load(List.of(first.toUri(), second.toUri())));
            ImageLayerLoader.Result initial = results.poll(10, TimeUnit.SECONDS);
            if (initial == null || initial.preserveSettings())
                throw new AssertionError("Initial series did not load normally");
            previous = initial.frames();
            if (!initial.selection().equals(new ImageProcessingSettings.FITSSelection(1, 0)))
                throw new AssertionError("Initial source selection was not reported");

            ImageProcessingSettings.FITSSelection selected = new ImageProcessingSettings.FITSSelection(1, 1);
            UiThread.postAndWait(() -> {
                loader.select(selected);
                loader.select(new ImageProcessingSettings.FITSSelection(1, 99));
            });
            ImageLayerLoader.Result replacement = results.poll(10, TimeUnit.SECONDS);
            if (replacement == null || !replacement.preserveSettings() || replacement.frames().size() != 2)
                throw new AssertionError("Selection did not reload the retained series");
            previous.close();
            previous = replacement.frames();
            for (int i = 0; i < previous.size(); i++) {
                if (!(previous.valueAt(i).source() instanceof FITSSource fits) || !fits.selection().equals(selected))
                    throw new AssertionError("Reload did not apply settings to every source");
            }
            UiThread.postAndWait(() -> {
                loader.select(selected);
                if (loader.isLoading())
                    throw new AssertionError("Unchanged selection reloaded the source");
                loader.select(new ImageProcessingSettings.FITSSelection(1, 99));
            });
            if (failures.poll(10, TimeUnit.SECONDS) == null)
                throw new AssertionError("Invalid selection did not fail");
            UiThread.postAndWait(() -> {
                loader.select(selected);
                if (loader.isLoading())
                    throw new AssertionError("Failed selection did not retain the current selection");
            });
            UiThread.postAndWait(() -> loader.load(List.of(first.toUri())));
            ImageLayerLoader.Result next = results.poll(10, TimeUnit.SECONDS);
            if (next == null || next.preserveSettings() || next.frames().size() != 1)
                throw new AssertionError("A new file inherited selection reload behavior");
            previous.close();
            previous = next.frames();
            if (!next.selection().equals(new ImageProcessingSettings.FITSSelection(1, 0)))
                throw new AssertionError("New source did not reset selection");
        } finally {
            UiThread.postAndWait(loader::abolish);
            if (previous != null)
                previous.close();
        }
        System.out.println("PASS direct series reload, busy/unchanged-selection no-op, failure retention and new-source reset");
    }

    private static void checkPlayerReload() {
        ImageLayer previous = Layers.getActiveImageLayer();
        long start = Instant.parse("2026-09-02T21:15:14Z").toEpochMilli();
        ImageLayer layer = new ImageLayer(Frames.synthetic(start, start + 4000, 1));
        try {
            Layers.setActiveImageLayer(layer);
            Player.setFrame(2);
            layer.setFrames(Frames.synthetic(start + 1000, start + 3000, 1));
            Layers.imageLayerLoaded(layer, false, true);
            if (Player.getStartTime() != start + 1000 || Player.getEndTime() != start + 3000
                    || Player.getPlaybackLastFrame() != 2 || Player.getTime().milli != start + 2000)
                throw new AssertionError("Selection reload did not refresh playback bounds and preserve time");
            layer.setFrames(Frames.synthetic(start + 5000, start + 5000, 1));
            Layers.imageLayerLoaded(layer, false, true);
            if (Player.getStartTime() != start + 5000 || Player.getEndTime() != start + 5000
                    || Player.getPlaybackLastFrame() != 0 || Player.getTime().milli != start + 5000)
                throw new AssertionError("Selection reload did not clamp playback to changed timestamps");
        } finally {
            Layers.setActiveImageLayer(previous);
            layer.detach();
        }
        System.out.println("PASS selection reload resynchronizes active playback bounds and time");
    }

    private static void write(Path path, Header template, String time, int offset) throws Exception {
        byte[][][] pixels = new byte[2][2][2];
        for (int plane = 0; plane < 2; plane++)
            for (int y = 0; y < 2; y++)
                for (int x = 0; x < 2; x++)
                    pixels[plane][y][x] = (byte) (offset + plane * 10 + y * 2 + x + 1);
        BasicHDU<?> image = Fits.makeHDU(pixels);
        for (Cursor<String, HeaderCard> cards = template.iterator(); cards.hasNext(); ) {
            HeaderCard card = cards.next();
            String key = card.getKey();
            if (!key.startsWith("NAXIS") && !key.startsWith("Z") && !key.startsWith("T")
                    && !key.equals("BITPIX") && !key.equals("XTENSION") && !key.equals("PCOUNT")
                    && !key.equals("GCOUNT") && !key.equals("END"))
                image.getHeader().addLine(card.copy());
        }
        for (String key : new String[]{"DATE-AVG", "DATE_AVG", "DATE_OBS", "DATE-OBS"})
            image.getHeader().addValue(key, time, null);
        try (Fits fits = new Fits()) {
            fits.addHDU(BasicHDU.getDummyHDU());
            fits.addHDU(image);
            fits.write(path.toFile());
        }
    }
}
