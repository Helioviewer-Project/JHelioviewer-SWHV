package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.LongUnaryOperator;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.image.nio.NativeImageFactory;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

// Opt-in headful benchmark of the production renderer.
public final class TimelineRenderingBenchmark {
    private static final long START = 1_700_006_400_000L;
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int STEPS = 12;
    private static final int WARMUP = 3;
    private static final int RUNS = 7;
    private static final Method BUILD = method(Band.class, "buildPolylines", List.class,
            TimeAxis.Mapper.class, YAxis.Mapper.class, LongUnaryOperator.class, boolean.class);
    private static final Field GRAPH = field(Band.class, "graphData");
    private static final Method SET_AXIS = method(TimeAxis.class, "set", long.class, long.class);
    private static final Method STACKED = method(GraphGeometry.class, "setStacked", boolean.class);
    private static final Field CURRENT_TIME = field(DrawController.class, "currentTime");

    private static Method method(Class<?> type, String name, Class<?>... parameters) {
        try {
            Method result = type.getDeclaredMethod(name, parameters);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field result = type.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        Path ffmpeg = Path.of(Directories.libCacheDir, "ffmpeg");
        Files.copy(Path.of(args[1]), ffmpeg, StandardCopyOption.REPLACE_EXISTING);
        if (!ffmpeg.toFile().setExecutable(true))
            throw new IllegalStateException("Cannot make benchmark FFmpeg executable");
        UIGlobals.uiFontSmall = new Font("SansSerif", Font.PLAIN, 12);
        UIGlobals.uiFontSmallBold = UIGlobals.uiFontSmall.deriveFont(Font.BOLD);
        UIGlobals.TL_LABEL_TEXT_COLOR = Color.WHITE;
        UIGlobals.TL_TICK_LINE_COLOR = Color.DARK_GRAY;
        UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR = Color.BLACK;
        UIGlobals.TL_MOVIE_FRAME_COLOR = Color.WHITE;
        ExportMovie.setMovieLineColor(Color.WHITE);
        System.out.printf("JVM=%s OS=%s %s arch=%s%n", System.getProperty("java.runtime.version"),
                System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    run(output);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            // AWT and application executors may otherwise retain the benchmark process.
            System.out.flush();
        }
        System.exit(0);
    }

    private static void run(Path output) throws Exception {
        GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        System.out.println("configuration=" + configuration + " deviceTransform=" + configuration.getDefaultTransform());
        checkCache(configuration);
        for (String workload : List.of("reduced", "full", "multicolor")) {
            String selected = System.getProperty("jhv.benchmark.workload");
            if (selected != null && !selected.equals(workload))
                continue;
            List<Band> bands = fixture(workload);
            try {
                for (boolean stacked : new boolean[]{false, true}) {
                    int scale = (int) configuration.getDefaultTransform().getScaleX();
                    String name = workload + "-" + (stacked ? "stacked" : "overlay") + "-" + scale + "x";
                    measure(configuration, bands, stacked, scale, name, output);
                }
            } finally {
                bands.forEach(Band::remove);
            }
        }
    }

    private static void checkCache(GraphicsConfiguration configuration) throws Exception {
        int scale = (int) configuration.getDefaultTransform().getScaleX();
        Band band = new Band(new BandType(new JSONObject().put("name", "cache-check")
                .put("range", new JSONArray(List.of(0, 100)))), true);
        VolatileImage target = configuration.createCompatibleVolatileImage(WIDTH, HEIGHT, Transparency.OPAQUE);
        Graphics2D graphics = target.createGraphics();
        Class<?> type = Class.forName("org.helioviewer.jhv.timelines.chart.ChartDrawGraphPane");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        JComponent chart = (JComponent) constructor.newInstance();
        Method paint = method(type, "paintComponent", java.awt.Graphics.class);
        Method recording = method(type, "recordingFrame");
        Field dirty = field(type, "redrawGraphArea");
        Field screen = field(type, "screenImage");
        try {
            band.setDataColor(Color.RED);
            BandCache cache = (BandCache) field(Band.class, "bandCache").get(band);
            cache.addToCache(band.getYAxis(), new float[]{50, 50, 50, Float.NaN, 50, 50},
                    new long[]{START, START + 20_000, START + 40_000, START + 50_000, START + 60_000, START + 100_000});
            field(type, "layers").set(chart, List.of(band));
            chart.setSize(WIDTH, HEIGHT);
            GraphGeometry geometry = DrawController.getGeometry();
            geometry.setSize(WIDTH, HEIGHT);
            STACKED.invoke(geometry, false);
            geometry.layout(List.of(band));
            SET_AXIS.invoke(DrawController.selectedAxis, START, START + 100_000);
            CURRENT_TIME.setLong(null, START - 1000);
            prepare(List.of(band), geometry, scale);
            dirty.setBoolean(chart, true);
            paint.invoke(chart, graphics);
            BufferedImage frame = ((ExportMovie.TimelineFrame) recording.invoke(chart)).image();
            if (frame.getWidth() != WIDTH * scale || frame.getHeight() != HEIGHT * scale)
                throw new AssertionError("Recording lost device resolution");
            int x = scale * geometry.xMapper(DrawController.selectedAxis).toPixel(START + 30_000);
            int gap = scale * geometry.xMapper(DrawController.selectedAxis).toPixel(START + 50_000);
            int y = scale * geometry.yMapper(band.getYAxis()).dataToPixel(50);
            int red = 0;
            for (int dy = -3; dy <= 3; dy++) {
                if (frame.getRGB(x, y + dy) == Color.RED.getRGB())
                    red++;
                if (frame.getRGB(gap, y + dy) == Color.RED.getRGB())
                    throw new AssertionError("Chart cache bridges a data gap");
            }
            if (red != 1)
                throw new AssertionError("Chart cache has " + red + " red pixels across a one-device-pixel stroke");
            if (frame.getRGB(x, y) != Color.RED.getRGB())
                throw new AssertionError("Three-vertex trace misses its exact device-pixel row");
            JSONArray levels = new JSONArray()
                    .put(new JSONObject().put("range", new JSONArray(List.of(0, 49))).put("color", "#ff0000"))
                    .put(new JSONObject().put("range", new JSONArray(List.of(50, 100))).put("color", "#00ff00"));
            Band colored = new Band(new BandType(new JSONObject().put("name", "color-check")
                    .put("range", new JSONArray(List.of(0, 100))).put("levels", levels)), true);
            try {
                colored.setDataColor(Color.BLUE);
                BandCache colors = (BandCache) field(Band.class, "bandCache").get(colored);
                colors.addToCache(colored.getYAxis(), new float[]{25, 25, 75, 75, Float.NaN, 25, 25},
                        new long[]{START, START + 20_000, START + 40_000, START + 60_000,
                                START + 70_000, START + 80_000, START + 100_000});
                field(type, "layers").set(chart, List.of(colored));
                geometry.layout(List.of(colored));
                prepare(List.of(colored), geometry, scale);
                dirty.setBoolean(chart, true);
                paint.invoke(chart, graphics);
                frame = ((ExportMovie.TimelineFrame) recording.invoke(chart)).image();
                for (int i = 0; i < 2; i++) {
                    int cx = scale * geometry.xMapper(DrawController.selectedAxis).toPixel(START + (i == 0 ? 10_000 : 50_000));
                    int cy = scale * geometry.yMapper(colored.getYAxis()).dataToPixel(i == 0 ? 25 : 75);
                    int expected = (i == 0 ? Color.RED : Color.GREEN).getRGB();
                    int pixels = 0;
                    for (int dy = -3; dy <= 3; dy++)
                        if (frame.getRGB(cx, cy + dy) == expected)
                            pixels++;
                    if (pixels != 1)
                        throw new AssertionError("Multicolor run lost its color or one-device-pixel stroke");
                    if (frame.getRGB(cx, cy) != expected)
                        throw new AssertionError("Multicolor run misses its exact device-pixel row");
                }
                int gapX = scale * geometry.xMapper(DrawController.selectedAxis).toPixel(START + 75_000);
                for (int row = scale * geometry.area().y; row < scale * geometry.graphBottom(); row++) {
                    int rgb = frame.getRGB(gapX, row);
                    if (rgb == Color.RED.getRGB() || rgb == Color.GREEN.getRGB())
                        throw new AssertionError("Multicolor runs bridge a data gap");
                }
            } finally {
                colored.remove();
                field(type, "layers").set(chart, List.of(band));
                geometry.layout(List.of(band));
            }
            System.out.println("CHECK multicolor runs, one-pixel strokes and gaps passed");
            chart.setSize(WIDTH - 100, HEIGHT - 100);
            geometry.setSize(WIDTH - 100, HEIGHT - 100);
            geometry.layout(List.of(band));
            prepare(List.of(band), geometry, scale);
            dirty.setBoolean(chart, true);
            paint.invoke(chart, graphics);
            frame = ((ExportMovie.TimelineFrame) recording.invoke(chart)).image();
            if (frame.getWidth() != (WIDTH - 100) * scale || frame.getHeight() != (HEIGHT - 100) * scale)
                throw new AssertionError("Recording dimensions did not follow resize");
            System.out.println("CHECK cache resolution, one-pixel stroke, exact placement, gaps and resize passed");
        } finally {
            graphics.dispose();
            target.flush();
            java.awt.Image image = (java.awt.Image) screen.get(chart);
            if (image != null)
                image.flush();
            band.remove();
        }
    }

    private static List<Band> fixture(String workload) throws Exception {
        List<Band> bands = new ArrayList<>();
        Color[] colors = {Color.WHITE, Color.CYAN, Color.YELLOW, Color.GREEN, Color.MAGENTA};
        int count = "reduced".equals(workload) ? 4320 : 100_000;
        long cadence = "reduced".equals(workload) ? 60_000 : 1000;
        for (int layer = 0; layer < 5; layer++) {
            JSONObject type = new JSONObject().put("name", "trace-" + layer).put("unitLabel", "counts")
                    .put("range", new JSONArray(List.of(0, 100)));
            if ("multicolor".equals(workload)) {
                JSONArray levels = new JSONArray();
                String[] names = {"green", "yellow", "red"};
                for (int i = 0; i < names.length; i++)
                    levels.put(new JSONObject().put("range", new JSONArray(List.of(i * 34, (i + 1) * 34))).put("color", names[i]));
                type.put("levels", levels);
            }
            Band band = new Band(new BandType(type), !"reduced".equals(workload));
            band.setDataColor(colors[layer]);
            long[] dates = new long[count];
            float[] values = new float[count];
            for (int i = 0; i < count; i++) {
                dates[i] = START + cadence * i;
                values[i] = i % 997 == 0 ? Float.NaN : (float) (50 + 30 * Math.sin(i * .019 + layer)
                        + 15 * Math.sin(i * .0013 + layer));
            }
            BandCache cache = (BandCache) field(Band.class, "bandCache").get(band);
            cache.addToCache(band.getYAxis(), values, dates);
            bands.add(band);
        }
        System.out.printf("FIXTURE %s layers=5 samples/layer=%d cadence=%d ms fullResolution=%s%n",
                workload, count, cadence, !"reduced".equals(workload));
        return bands;
    }

    private static void prepare(List<Band> bands, GraphGeometry geometry, int scale) throws Exception {
        for (Band band : bands) {
            Rectangle area = geometry.getLayerArea(band);
            BandCache cache = (BandCache) field(Band.class, "bandCache").get(band);
            List<List<BandCache.DateValue>> raw = cache.getValues(scale * area.width,
                    DrawController.selectedAxis.start(), DrawController.selectedAxis.end());
            GRAPH.set(band, BUILD.invoke(null, raw, geometry.xMapper(DrawController.selectedAxis),
                    geometry.yMapper(band.getYAxis(), area), LongUnaryOperator.identity(), band.isMulticolor()));
        }
    }

    private static void measure(GraphicsConfiguration configuration, List<Band> bands, boolean stacked,
                                int scale, String name, Path output) throws Exception {
        Class<?> chartType = Class.forName("org.helioviewer.jhv.timelines.chart.ChartDrawGraphPane");
        Constructor<?> constructor = chartType.getDeclaredConstructor();
        constructor.setAccessible(true);
        JComponent chart = (JComponent) constructor.newInstance();
        field(chartType, "layers").set(chart, new ArrayList<TimelineLayer>(bands));
        Method paint = method(chartType, "paintComponent", java.awt.Graphics.class);
        Method recording = method(chartType, "recordingFrame");
        Field dirty = field(chartType, "redrawGraphArea");
        Field screen = field(chartType, "screenImage");
        GraphGeometry geometry = DrawController.getGeometry();
        geometry.setSize(WIDTH, HEIGHT);
        STACKED.invoke(geometry, stacked);
        geometry.layout(new ArrayList<TimelineLayer>(bands));
        chart.setSize(WIDTH, HEIGHT);
        VolatileImage target = configuration.createCompatibleVolatileImage(WIDTH, HEIGHT, Transparency.OPAQUE);
        if (!target.getCapabilities().isAccelerated())
            throw new AssertionError("Presentation target is not accelerated");
        long duration = "reduced".equals(name.split("-")[0]) ? 86_400_000 : 50_000_000;
        Graphics2D g = target.createGraphics();
        if (g.getTransform().getScaleX() != scale || g.getTransform().getScaleY() != scale)
            throw new AssertionError("Unexpected accelerated surface scale");
        try {
            for (String action : List.of("pan", "zoom", "cached", "export-cached", "export-redraw")) {
                double[][] values = new double[3][RUNS];
                BufferedImage completed = null;
                for (int run = -WARMUP; run < RUNS; run++) {
                    double prep = 0, capture = 0;
                    SET_AXIS.invoke(DrawController.selectedAxis, START, START + duration);
                    prepare(bands, geometry, scale);
                    dirty.setBoolean(chart, true);
                    paint.invoke(chart, g);
                    Toolkit.getDefaultToolkit().sync();
                    long before = System.nanoTime();
                    for (int step = 0; step < STEPS; step++) {
                        boolean redraw = !"cached".equals(action) && !"export-cached".equals(action);
                        long begin = System.nanoTime();
                        if (redraw) {
                            long shift = duration * step / 100;
                            long span = "zoom".equals(action) ? (long) (duration * Math.pow(.9, step)) : duration;
                            SET_AXIS.invoke(DrawController.selectedAxis, START + shift, START + shift + span);
                            prepare(bands, geometry, scale);
                            dirty.setBoolean(chart, true);
                        }
                        long prepared = System.nanoTime();
                        CURRENT_TIME.setLong(null, DrawController.selectedAxis.start() + duration * step / STEPS);
                        paint.invoke(chart, g);
                        long painted = System.nanoTime();
                        if (action.startsWith("export")) {
                            ExportMovie.TimelineFrame frame = (ExportMovie.TimelineFrame) recording.invoke(chart);
                            BufferedImage copy = NativeImageFactory.copyImage(frame.image());
                            NativeImageFactory.free(copy);
                        }
                        long captured = System.nanoTime();
                        prep += prepared - begin;
                        capture += captured - painted;
                    }
                    Toolkit.getDefaultToolkit().sync();
                    // Readback fences queued GPU work; one common readback amortized over the sequence.
                    completed = snapshot(target, configuration, scale);
                    if (target.contentsLost())
                        throw new AssertionError("Presentation surface lost during measurement");
                    if (run >= 0) {
                        values[0][run] = (System.nanoTime() - before) / 1e6 / STEPS;
                        values[1][run] = prep / 1e6 / STEPS;
                        values[2][run] = capture / 1e6 / STEPS;
                    }
                }
                String key = name + "-" + action;
                System.out.printf("RESULT %s total=%.3f prep=%.3f capture=%.3f ms/frame%n",
                        key, median(values[0]), median(values[1]), median(values[2]));
                Path image = output.resolve(key + ".png");
                ImageIO.write(completed, "png", image.toFile());
            }
            if (scale == 2 && !stacked)
                movie(chart, recording, paint, dirty, g, bands, geometry, scale, name, output);
        } finally {
            g.dispose();
            target.flush();
            java.awt.Image image = (java.awt.Image) screen.get(chart);
            if (image != null)
                image.flush();
        }
    }

    private static void movie(Object chart, Method recording, Method paint, Field dirty, Graphics2D g,
                              List<Band> bands, GraphGeometry geometry, int scale, String name, Path output) throws Exception {
        Class<?> writerType = Class.forName("org.helioviewer.jhv.movie.ExportWriter");
        Class<?> formatType = Class.forName("org.helioviewer.jhv.movie.ExportFormat");
        Constructor<?> constructor = writerType.getDeclaredConstructor(formatType, int.class, int.class, int.class);
        constructor.setAccessible(true);
        Method encode = method(writerType, "encode", BufferedImage.class, BufferedImage.class, int.class);
        Method close = method(writerType, "close");
        for (boolean redraw : new boolean[]{false, true}) {
            Object writer = constructor.newInstance(formatType.getField("H264").get(null), WIDTH, 696, 30);
            double captured = 0, encoded = 0;
            long before = System.nanoTime();
            for (int i = 0; i < 60; i++) {
                long frameStart = System.nanoTime();
                if (redraw) {
                    long start = START + 100_000 * i;
                    SET_AXIS.invoke(DrawController.selectedAxis, start, start + 50_000_000);
                    prepare(bands, geometry, scale);
                    dirty.setBoolean(chart, true);
                }
                CURRENT_TIME.setLong(null, DrawController.selectedAxis.start() + 800_000 * i);
                paint.invoke(chart, g);
                ExportMovie.TimelineFrame frame = (ExportMovie.TimelineFrame) recording.invoke(chart);
                BufferedImage copy = NativeImageFactory.copyImage(frame.image());
                long captureEnd = System.nanoTime();
                try {
                    encode.invoke(writer, null, copy, frame.movieLinePosition());
                } finally {
                    NativeImageFactory.free(copy);
                }
                captured += captureEnd - frameStart;
                encoded += System.nanoTime() - captureEnd;
            }
            long closeStart = System.nanoTime();
            Path movie = Path.of((String) close.invoke(writer));
            double closeMillis = (System.nanoTime() - closeStart) / 1e6;
            Files.move(movie, output.resolve(name + (redraw ? "-redraw.mp4" : "-cached.mp4")), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("MOVIE %s %s capture=%.3f write=%.3f ms/frame ffmpeg=%.1f total=%.1f ms frames=60%n",
                    name, redraw ? "redraw" : "cached", captured / 1e6 / 60, encoded / 1e6 / 60,
                    closeMillis, (System.nanoTime() - before) / 1e6);
        }
    }

    private static BufferedImage snapshot(VolatileImage image, GraphicsConfiguration configuration, int scale) {
        BufferedImage result = configuration.createCompatibleImage(image.getWidth() * scale, image.getHeight() * scale, Transparency.OPAQUE);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, result.getWidth(), result.getHeight(), null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static double median(double[] values) {
        Arrays.sort(values);
        return values[values.length / 2];
    }
}
