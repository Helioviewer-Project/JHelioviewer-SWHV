package org.helioviewer.jhv.export;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.image.MappedImageFactory;
import org.helioviewer.jhv.base.image.NIOImageFactory;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.layers.FrameListener;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.time.TimeUtils;

import com.jogamp.opengl.GL2;

public class ExportMovie implements FrameListener {

    private static final ExportMovie instance = new ExportMovie();
    private static final ExecutorService encodeExecutor = Executors.newSingleThreadExecutor(new JHVThread.NamedThreadFactory("Movie Encode"));

    private static MovieExporter exporter;

    private static RecordMode mode;
    private static boolean stopped;
    private static boolean shallStop;

    public static BufferedImage EVEImage = null;
    public static int EVEMovieLinePosition = -1;

    public static void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            if (keep) {
                encodeExecutor.execute(new CloseWriter(exporter, true));
            } else {
                encodeExecutor.shutdownNow();
                new CloseWriter(exporter, false).run();
            }
            exporter = null;
        }
    }

    private static void exportMovieFinish() {
        GLGrab.detach();
        JHVFrame.getGLListener().detachExport();
        MoviePanel.setEnabledOptions(true);
        if (mode == RecordMode.LOOP) {
            Movie.removeFrameListener(instance);
        }

        try {
            disposeMovieWriter(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleMovieExport(Camera camera, GL2 gl) {
        if (stopped) {
            exportMovieFinish();
            return;
        }

        try {
            BufferedImage screen = MappedImageFactory.createCompatible(GLGrab.getWidth(), exporter.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            GLGrab.renderFrame(camera, gl, MappedImageFactory.getByteBuffer(screen));
            BufferedImage eve = EVEImage == null ? null : NIOImageFactory.copyImage(EVEImage);
            encodeExecutor.execute(new FrameConsumer(exporter, screen, GLGrab.getHeight(), eve, EVEMovieLinePosition));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Movie.grabDone();

        if (mode == RecordMode.SHOT) {
            stop();
        }
    }

    private static final int MACROBLOCK = 8;

    public static void start(int _w, int _h, boolean isInternal, int fps, RecordMode _mode) {
        Movie.startRecording();

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

        MoviePanel.setEnabledOptions(false);

        GLGrab.attach(canvasWidth, canvasHeight);
        JHVFrame.getGLListener().attachExport(instance);

        String prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + TimeUtils.formatFilename(System.currentTimeMillis());
        if (mode == RecordMode.SHOT) {
            try {
                exporter = new PNGExporter();
                exporter.open(prefix + ".png", canvasWidth, exportHeight, fps);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MovieDisplay.render(1);
        } else {
            try {
                exporter = new JCodecExporter();
                exporter.open(prefix + ".mp4", canvasWidth, exportHeight, fps);
            } catch (Exception e) {
                e.printStackTrace();
            }

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

            if (mode != RecordMode.FREE)
                MoviePanel.clickRecordButton();
            MovieDisplay.display(); // force detach
        }
        Movie.stopRecording();
    }

    // loop mode only
    @Override
    public void frameChanged(int frame, boolean last) {
        if (shallStop)
            stop();
        if (last)
            shallStop = true;
    }

    private static class FrameConsumer implements Runnable {

        private final MovieExporter movieExporter;
        private final BufferedImage mainImage;
        private final BufferedImage eveImage;
        private final int frameH;
        private final int movieLinePosition;

        FrameConsumer(MovieExporter _movieExporter, BufferedImage _mainImage, int _frameH, BufferedImage _eveImage, int _movieLinePosition) {
            movieExporter = _movieExporter;
            mainImage = _mainImage;
            eveImage = _eveImage;
            frameH = _frameH;
            movieLinePosition = _movieLinePosition;
        }

        @Override
        public void run() {
            try {
                ExportUtils.pasteCanvases(mainImage, frameH, eveImage, movieLinePosition, movieExporter.getHeight());
                if (eveImage != null)
                    NIOImageFactory.free(eveImage);
                movieExporter.encode(mainImage);
                MappedImageFactory.free(mainImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static class CloseWriter implements Runnable {

        private final MovieExporter movieExporter;
        private final boolean keep;

        CloseWriter(MovieExporter _movieExporter, boolean _keep) {
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
            System.gc();
        }
    }

    private ExportMovie() {
    }

}
