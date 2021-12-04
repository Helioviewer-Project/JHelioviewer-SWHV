package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.image.MappedImageFactory;
import org.helioviewer.jhv.base.image.NIOImageFactory;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.threads.JHVThread;

import com.jogamp.opengl.GL2;

public class ExportMovie implements Movie.Listener {

    private static final ExportMovie instance = new ExportMovie();
    private static final ExecutorService encodeExecutor = Executors.newSingleThreadExecutor(new JHVThread.NamedThreadFactory("Movie Encode"));

    private static MovieExporter exporter;
    private static GLGrab grabber;

    private static RecordMode mode;
    private static boolean shallStop;

    public static BufferedImage EVEImage = null;
    public static int EVEMovieLinePosition = -1;

    public static void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            if (keep) {
                encodeExecutor.execute(new CloseWriter(exporter));
            } else {
                encodeExecutor.shutdownNow();
            }
            exporter = null;
        }
    }

    public static void handleMovieExport(Camera camera, GL2 gl) {
        try {
            BufferedImage screen = MappedImageFactory.createCompatible(grabber.w, grabber.h, BufferedImage.TYPE_3BYTE_BGR);
            grabber.renderFrame(camera, gl, MappedImageFactory.getByteBuffer(screen));
            BufferedImage eve = EVEImage == null ? null : NIOImageFactory.copyImage(EVEImage);
            encodeExecutor.execute(new FrameConsumer(exporter, screen, eve, EVEMovieLinePosition));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Movie.grabDone();

        if (shallStop) {
            grabber.dispose(gl);
            stop();
        }
    }

    private static final int MACROBLOCK = 8;

    public static void start(int _w, int _h, boolean isInternal, int fps, RecordMode _mode) {
        Movie.startRecording();
        MoviePanel.setEnabledOptions(false);
        shallStop = false;

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
        grabber = new GLGrab(canvasWidth, canvasHeight);

        if (mode == RecordMode.SHOT) {
            exporter = new MovieExporter(VideoFormat.PNG, canvasWidth, exportHeight, fps);
            shallStop = true;
            MovieDisplay.render(1);
        } else {
            VideoFormat format = VideoFormat.H264;
            try {
                format = VideoFormat.valueOf(Settings.getProperty("video.format"));
            } catch (Exception ignore) {
            }
            exporter = new MovieExporter(format, canvasWidth, exportHeight, fps);

            if (mode == RecordMode.LOOP) {
                Movie.addFrameListener(instance);
                Movie.setFrame(0);
                Movie.play();
            }
        }
    }

    private static void stop() {
        Movie.stopRecording();
        MoviePanel.setEnabledOptions(true);
        MoviePanel.unselectRecordButton();
        if (mode == RecordMode.LOOP) {
            Movie.removeFrameListener(instance);
        }

        try {
            disposeMovieWriter(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // loop mode only
    @Override
    public void frameChanged(int frame, boolean last) {
        if (last)
            shallStop = true;
    }

    public static void shallStop() {
        shallStop = true;
        MovieDisplay.display(); // force detach
    }

    private record FrameConsumer(MovieExporter movieExporter, BufferedImage mainImage, BufferedImage eveImage,
                                 int movieLinePosition) implements Runnable {
        @Override
        public void run() {
            try {
                movieExporter.encode(mainImage, eveImage, movieLinePosition);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                NIOImageFactory.free(eveImage);
                MappedImageFactory.free(mainImage);
            }
        }
    }

    private record CloseWriter(MovieExporter movieExporter) implements Runnable {
        @Override
        public void run() {
            try {
                movieExporter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.gc();
        }
    }

    private ExportMovie() {
    }

}
