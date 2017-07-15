package org.helioviewer.jhv.export;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.ImageUtils;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.layers.FrameListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.time.TimeUtils;

import com.jogamp.opengl.GL2;

public class ExportMovie implements FrameListener {

    private static MovieExporter exporter;
    private static GLGrab grabber;

    private static RecordMode mode;
    private static boolean stopped = false;

    private static final int NUM_FRAMES = 512;
    private final ArrayBlockingQueue<Runnable> frameQueue = new ArrayBlockingQueue<>(2 * NUM_FRAMES);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, frameQueue, new JHVThread.NamedThreadFactory("Export Movie"), new ThreadPoolExecutor.DiscardPolicy()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            JHVThread.afterExecute(r, t);
        }
    };

    public static BufferedImage EVEImage = null;
    public static int EVEMovieLinePosition = -1;

    public void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            Runnable runnable = new CloseWriter(exporter, keep);
            if (keep) {
                executor.execute(runnable);
            } else {
                executor.shutdownNow();
                runnable.run();
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

        BufferedImage screenshot = grabber.renderFrame(camera, gl);
        try {
            if (mode == RecordMode.SHOT || frameQueue.size() <= NUM_FRAMES)
                executor.execute(new FrameConsumer(exporter, screenshot, EVEImage, EVEMovieLinePosition));
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
            try {
                exporter = new PNGExporter();
                exporter.open(prefix + ".png", canvasWidth, exportHeight, fps);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Displayer.display();
        } else {
            try {
                exporter = new JCodecExporter();
                exporter.open(prefix + ".mp4", canvasWidth, exportHeight, fps);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mode == RecordMode.LOOP) {
                Layers.addFrameListener(instance);
                Layers.setFrame(0);
                Layers.playMovie();
            }
        }
    }

    public static void stop() {
        if (!stopped) {
            stopped = true;

            if (mode == RecordMode.LOOP)
                Layers.removeFrameListener(instance);
            if (mode != RecordMode.FREE)
                MoviePanel.clickRecordButton();
            Displayer.display(); // force detach
        }
    }

    // loop mode only
    @Override
    public void frameChanged(int frame, boolean last) {
        if (last) // last frame missed, to be fixed with layers refactor
            stop();
    }

    private static class FrameConsumer implements Runnable {

        private final MovieExporter movieExporter;
        private BufferedImage mainImage;
        private BufferedImage eveImage;
        private final int movieLinePosition;

        private FrameConsumer(MovieExporter _movieExporter, BufferedImage _mainImage, BufferedImage _eveImage, int _movieLinePosition) {
            movieExporter = _movieExporter;
            mainImage = _mainImage;
            eveImage = _eveImage == null ? null : ImageUtils.deepCopy(_eveImage);
            movieLinePosition = _movieLinePosition;
        }

        @Override
        public void run() {
            try {
                BufferedImage composite = ExportUtils.pasteCanvases(mainImage, eveImage, movieLinePosition, movieExporter.getHeight());
                mainImage = null;
                eveImage = null;
                movieExporter.encode(composite);
                composite = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static class CloseWriter implements Runnable {

        private final MovieExporter movieExporter;
        private final boolean keep;

        private CloseWriter(MovieExporter _movieExporter, boolean _keep) {
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
