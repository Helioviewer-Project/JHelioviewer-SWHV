package org.helioviewer.jhv.export;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.ImageUtils;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.layers.FrameListener;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.time.TimeUtils;

import com.jogamp.opengl.GL2;

public class ExportMovie implements FrameListener {

    private static MovieExporter exporter;
    private static GLGrab grabber;

    private static RecordMode mode;
    private static boolean stopped;
    private static boolean shallStop;

    private final ExecutorService pasteExecutor = Executors.newFixedThreadPool(1, new JHVThread.NamedThreadFactory("Movie Paste"));
    private final ExecutorService xformExecutor = Executors.newFixedThreadPool(1, new JHVThread.NamedThreadFactory("Movie Transform"));
    private final ExecutorService encodeExecutor = Executors.newFixedThreadPool(1, new JHVThread.NamedThreadFactory("Movie Encode"));

    public static BufferedImage EVEImage = null;
    public static int EVEMovieLinePosition = -1;

    public void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            if (keep) {
                pasteExecutor.execute(new CloseWriter1(exporter, true));
            } else {
                pasteExecutor.shutdownNow();
                xformExecutor.shutdownNow();
                encodeExecutor.shutdownNow();
                new CloseWriter3(exporter, false).run();
            }
            exporter = null;
        }
    }

    private void exportMovieFinish(GL2 gl) {
        ImageViewerGui.getGLListener().detachExport();
        MoviePanel.recordPanelSetEnabled(true);

        try {
            grabber.dispose(gl);
            disposeMovieWriter(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMovieExport(Camera camera, GL2 gl) {
        if (stopped) {
            exportMovieFinish(gl);
            return;
        }

        try {
            BufferedImage screenshot = grabber.renderFrame(camera, gl);
            pasteExecutor.execute(new Paster(exporter, screenshot, EVEImage, EVEMovieLinePosition));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mode == RecordMode.SHOT) {
            stop();
        }
    }

    private static final int MACROBLOCK = 8;

    public static void start(int _w, int _h, boolean isInternal, int fps, RecordMode _mode) {
        int scrw = 1;
        int scrh = 0;
        if (EVEImage != null) {
            scrw = Math.max(1, EVEImage.getWidth());
            scrh = EVEImage.getHeight();
        }

        mode = _mode;
        int canvasWidth = mode == RecordMode.SHOT ? _w : (_w / MACROBLOCK) * MACROBLOCK;
        int sh = (int) (scrh / (double) scrw * canvasWidth + .5);
        int canvasHeight = isInternal ? _h - sh : _h;
        int exportHeight = mode == RecordMode.SHOT ? canvasHeight + sh : ((canvasHeight + sh) / MACROBLOCK) * MACROBLOCK;

        canvasHeight = exportHeight - sh;

        stopped = false;

        MoviePanel.recordPanelSetEnabled(false);

        grabber = new GLGrab(canvasWidth, canvasHeight);
        ImageViewerGui.getGLListener().attachExport(instance);

        String prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + TimeUtils.formatFilename(System.currentTimeMillis());
        if (mode == RecordMode.SHOT) {
            exporter = new PNGExporter(prefix + ".png", exportHeight);
            Displayer.display();
        } else {
            exporter = new JCodecExporter(prefix + ".mp4", canvasWidth, exportHeight, fps);
            if (mode == RecordMode.LOOP) {
                Movie.addFrameListener(instance);
                Movie.setFrame(0);
                Movie.play();
            }
        }
    }

    public static void stop() {
        shallStop = false;
        if (!stopped) {
            stopped = true;

            if (mode == RecordMode.LOOP)
                Movie.removeFrameListener(instance);
            if (mode != RecordMode.FREE)
                MoviePanel.clickRecordButton();
            Displayer.display(); // force detach
        }
    }

    // loop mode only
    @Override
    public void frameChanged(int frame, boolean last) {
        if (shallStop)
            stop();
        if (last)
            shallStop = true;
    }

    private class Paster implements Runnable {

        private final MovieExporter movieExporter;
        private final SoftReference<Pair<BufferedImage, BufferedImage>> ref;
        private final int movieLinePosition;

        Paster(MovieExporter _movieExporter, BufferedImage mainImage, BufferedImage eveImage, int _movieLinePosition) {
            movieExporter = _movieExporter;
            ref = new SoftReference<>(new Pair<>(mainImage, eveImage == null ? null : ImageUtils.deepCopy(eveImage)));
            movieLinePosition = _movieLinePosition;
        }

        @Override
        public void run() {
            Pair<BufferedImage, BufferedImage> p = ref.get();
            if (p != null) {
                BufferedImage mainImage = p.a, eveImage = p.b;
                BufferedImage composite = ExportUtils.pasteCanvases(mainImage, eveImage, movieLinePosition, movieExporter.getHeight());
                xformExecutor.execute(new Transformer(movieExporter, composite));
            }
        }

    }

    private class Transformer implements Runnable {

        private final MovieExporter movieExporter;
        private final SoftReference<BufferedImage> ref;

        Transformer(MovieExporter _movieExporter, BufferedImage image) {
            movieExporter = _movieExporter;
            ref = new SoftReference<>(image);
        }

        @Override
        public void run() {
            BufferedImage image = ref.get();
            if (image != null) {
                encodeExecutor.execute(new Encoder(movieExporter, movieExporter.transform(image)));
            }
        }

    }

    private static class Encoder implements Runnable {

        private final MovieExporter movieExporter;
        private final SoftReference<Object> ref;

        Encoder(MovieExporter _movieExporter, Object frame) {
            movieExporter = _movieExporter;
            ref = new SoftReference<>(frame);
        }

        @Override
        public void run() {
            try {
                Object frame = ref.get();
                if (frame != null) {
                    movieExporter.encode(frame);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private class CloseWriter1 implements Runnable {

        private final MovieExporter movieExporter;
        private final boolean keep;

        CloseWriter1(MovieExporter _movieExporter, boolean _keep) {
            movieExporter = _movieExporter;
            keep = _keep;
        }

        @Override
        public void run() {
            xformExecutor.execute(new CloseWriter2(movieExporter, keep));
        }

    }

    private class CloseWriter2 implements Runnable {

        private final MovieExporter movieExporter;
        private final boolean keep;

        CloseWriter2(MovieExporter _movieExporter, boolean _keep) {
            movieExporter = _movieExporter;
            keep = _keep;
        }

        @Override
        public void run() {
            encodeExecutor.execute(new CloseWriter3(movieExporter, keep));
        }

    }

    private static class CloseWriter3 implements Runnable {

        private final MovieExporter movieExporter;
        private final boolean keep;

        CloseWriter3(MovieExporter _movieExporter, boolean _keep) {
            movieExporter = _movieExporter;
            keep = _keep;
        }

        @Override
        public void run() {
            boolean failed = false;
            try {
                if (keep) {
                    movieExporter.close();
                    EventQueue.invokeLater(() -> JHVGlobals.displayNotification(movieExporter.getPath()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                failed = true;
            }
            if (!keep || failed) {
                File f = new File(movieExporter.getPath());
                f.delete();
            }
        }
    }

    private static final ExportMovie instance = new ExportMovie();

    private ExportMovie() {
    }

    public static ExportMovie getInstance() {
        return instance;
    }

}
