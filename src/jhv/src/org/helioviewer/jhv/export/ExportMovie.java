package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.layers.FrameListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.threads.JHVThread;

import com.jogamp.opengl.GL2;

public class ExportMovie implements FrameListener {

    private static MovieExporter exporter;
    private static GLGrab grabber;

    private static int exportHeight;

    private static RecordMode mode;
    private static String moviePath;
    private static String imagePath;
    private static boolean inited = false;
    private static boolean stopped = false;

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(1024);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("Export Movie"), new ThreadPoolExecutor.DiscardPolicy());
    public static BufferedImage EVEImage;

    public void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            blockingQueue.poll();
            if (keep) {
                executor.submit(new CloseWriter(exporter, moviePath, keep));
            } else {
                while (blockingQueue.poll() != null) {
                }
                Future<?> f = executor.submit(new CloseWriter(exporter, moviePath, keep));
                try {
                    f.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            exporter = null;
        }
    }

    private void exportMovieFinish(GL2 gl) {
        ImageViewerGui.getMainComponent().detachExport();
        MoviePanel.recordPanelSetEnabled(true);

        try {
            grabber.dispose(gl);
            disposeMovieWriter(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMovieExport(GL2 gl) {
        if (!inited) {
            inited = true;
            grabber.init(gl);
        }

        if (stopped) {
            exportMovieFinish(gl);
            inited = false;
            return;
        }

        BufferedImage screenshot = grabber.renderFrame(gl);
        try {
            if (mode == RecordMode.SHOT) {
                ImageIO.write(ExportUtils.pasteCanvases(screenshot, EVEImage, exportHeight), "png", new File(imagePath));
                stop();
            } else {
                try {
                    executor.submit(new FrameConsumer(exporter, screenshot, EVEImage, exportHeight));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void start(int _w, int _h, boolean isInternal, int fps, RecordMode _mode) {
        int scrw = 1;
        int scrh = 0;
        if (EVEImage != null && ImageViewerGui.getMainContentPanel().mainContentPluginsActive()) {
            scrw = Math.max(1, EVEImage.getWidth());
            scrh = EVEImage.getHeight();
        }

        int canvasWidth, canvasHeight;

        mode = _mode;
        if (mode == RecordMode.SHOT)
            canvasWidth = _w;
        else
            canvasWidth = (_w / 2) * 2; // wiser for video formats

        int sh = (int) (scrh / (double) scrw * canvasWidth + .5);
        if (isInternal)
            canvasHeight = _h - sh;
        else
            canvasHeight = _h;

        if (mode == RecordMode.SHOT)
            exportHeight = canvasHeight + sh;
        else
            exportHeight = ((canvasHeight + sh) / 2) * 2; // wiser for video formats

        canvasHeight = exportHeight - sh;

        stopped = false;
        currentFrame = 0;

        String prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + TimeUtils.filenameDateFormat.format(new Date());
        moviePath = prefix + ".mp4";
        imagePath = prefix + ".png";

        MoviePanel.recordPanelSetEnabled(false);

        grabber = new GLGrab(canvasWidth, canvasHeight);
        ImageViewerGui.getMainComponent().attachExport(instance);

        if (mode == RecordMode.SHOT) {
            Displayer.display();
        } else {
            try {
                exporter = new XuggleExporter();
                //exporter = new HumbleExporter();
                exporter.open(moviePath, canvasWidth, exportHeight, fps);
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

    private static int currentFrame = 0;

    // loop mode only
    @Override
    public void frameChanged(int frame) {
        if (frame < currentFrame)
            stop();
        else
            currentFrame = frame;
    }

    private static class FrameConsumer implements Runnable {

        private final MovieExporter movieExporter;
        private final BufferedImage mainImage;
        private final BufferedImage eve;
        private final int height;

        public FrameConsumer(MovieExporter _movieExporter, BufferedImage _mainImage, BufferedImage _eve, int _height) {
            movieExporter = _movieExporter;
            mainImage = _mainImage;
            eve = _eve;
            height = _height;
        }

        @Override
        public void run() {
            try {
                movieExporter.encode(ExportUtils.pasteCanvases(mainImage, eve, height));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static class CloseWriter implements Runnable {

        private final MovieExporter movieExporter;
        private final String moviePath;
        private final boolean keep;

        public CloseWriter(MovieExporter _movieExporter, String _moviePath, boolean _keep) {
            movieExporter = _movieExporter;
            moviePath = _moviePath;
            keep = _keep;
        }

        @Override
        public void run() {
            boolean failed = false;
            try {
                if (movieExporter != null) {
                    movieExporter.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                failed = true;
            }
            if (moviePath != null && (!keep || failed)) {
                File f = new File(moviePath);
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
