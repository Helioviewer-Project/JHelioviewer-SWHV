package org.helioviewer.jhv.export;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.helioviewer.jhv.AppCommands;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.gui.ViewerState;
import org.helioviewer.jhv.imagedata.nio.MappedImageFactory;
import org.helioviewer.jhv.imagedata.nio.NativeImageFactory;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.threads.JHVThread;

public final class ExportMovie implements Movie.Listener {

    private static final ExportMovie instance = new ExportMovie();
    private static final ExecutorService encodeExecutor = Executors.newSingleThreadExecutor(new JHVThread.NamedThreadFactory("JHV-EncodeMovie"));

    private static MovieExporter exporter;
    private static GLGrab grabber;

    private static ViewerState.RecordingMode mode;
    private static boolean shallStop;
    private static @Nullable AppCommands.OperationContext operationContext;

    public static BufferedImage EVEImage = null;
    public static int EVEMovieLinePosition = -1;

    public static void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            if (keep) {
                encodeExecutor.execute(new CloseWriter(exporter));
            } else {
                for (Runnable runnable : encodeExecutor.shutdownNow()) {
                    if (runnable instanceof FrameConsumer frameConsumer) {
                        NativeImageFactory.free(frameConsumer.eveImage());
                        MappedImageFactory.free(frameConsumer.mainImage());
                    }
                }
            }
            exporter = null;
        }
    }

    public static void handleMovieExport(Camera camera) {
        BufferedImage screen = null;
        BufferedImage eve = null;
        boolean submitted = false;
        try {
            screen = MappedImageFactory.createRGBImage(grabber.w, grabber.h);
            grabber.renderFrame(camera, MappedImageFactory.getByteBuffer(screen));
            eve = EVEImage == null ? null : NativeImageFactory.copyImage(EVEImage);
            encodeExecutor.execute(new FrameConsumer(exporter, screen, eve, EVEMovieLinePosition));
            submitted = true;
        } catch (Exception e) {
            Log.error(e);
        } finally {
            if (!submitted) {
                NativeImageFactory.free(eve);
                MappedImageFactory.free(screen);
            }
        }
        Movie.grabDone();

        if (shallStop) {
            grabber.dispose();
            stop();
        }
    }

    private static final int MACROBLOCK = 8;

    public static void start(@Nullable AppCommands.OperationContext context, @Nullable AppCommands.RecordStartArgs input) {
        if (Movie.isRecording())
            return;
        operationContext = context;

        if (input != null) {
            if (input.mode() != null)
                ViewerState.setRecordingMode(input.mode());
            if (input.size() != null)
                ViewerState.setRecordingSize(input.size());
            if (input.advanceMode() != null)
                ViewerState.setPlaybackAdvanceMode(input.advanceMode());
            if (input.speed() != null || input.speedUnit() != null) {
                ViewerState.PlaybackData current = ViewerState.playbackData();
                int speed = input.speed() == null ? current.speed() : input.speed();
                ViewerState.PlaybackSpeedUnit speedUnit = input.speedUnit() == null ? current.speedUnit() : input.speedUnit();
                ViewerState.setPlaybackSpeed(speed, speedUnit);
            }
        }

        ViewerState.RecordingData recordingData = ViewerState.recordingData();
        ViewerState.PlaybackData playbackData = ViewerState.playbackData();
        Dimension size = recordingData.size().getSize();
        int fps = playbackData.speedUnit().isRelative() ? playbackData.speed() : Movie.FPS_ABSOLUTE;
        start(size.width, size.height, recordingData.size().isInternal(), fps, recordingData.mode());
    }

    public static void start(int _w, int _h, boolean isInternal, int fps, ViewerState.RecordingMode _mode) {
        Movie.startRecording();
        shallStop = false;

        int scrw = 1;
        int scrh = 0;
        if (EVEImage != null) {
            scrw = Math.max(1, EVEImage.getWidth());
            scrh = EVEImage.getHeight();
        }

        mode = _mode;
        int canvasWidth = mode == ViewerState.RecordingMode.SHOT ? _w : (_w / MACROBLOCK) * MACROBLOCK;
        int sh = (int) (scrh / (double) scrw * canvasWidth + .5);
        int canvasHeight = isInternal ? _h - sh : _h;
        int exportHeight = mode == ViewerState.RecordingMode.SHOT ? canvasHeight + sh : ((canvasHeight + sh) / MACROBLOCK) * MACROBLOCK;

        canvasHeight = exportHeight - sh;
        grabber = new GLGrab(canvasWidth, canvasHeight);

        if (mode == ViewerState.RecordingMode.SHOT) {
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

            if (mode == ViewerState.RecordingMode.LOOP) {
                Movie.addFrameListener(instance);
                AppCommands.seekFrame(0);
                AppCommands.play();
            }
        }
    }

    private static void stop() {
        Movie.stopRecording();
        if (mode == ViewerState.RecordingMode.LOOP) {
            Movie.removeFrameListener(instance);
        }

        try {
            disposeMovieWriter(true);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private static void recordingFinished(boolean success, String message, @Nullable String output) {
        AppCommands.notifyRecordingFinished(operationContext, success, message, output);
        operationContext = null;
    }

    // loop mode only
    @Override
    public void frameChanged(int frame, boolean last) {
        if (last)
            shallStop = true;
    }

    public static void shallStop() {
        if (!Movie.isRecording())
            return;
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
                Log.error(e);
            } finally {
                NativeImageFactory.free(eveImage);
                MappedImageFactory.free(mainImage);
            }
        }
    }

    private record CloseWriter(MovieExporter movieExporter) implements Runnable {
        @Override
        public void run() {
            try {
                String output = movieExporter.close();
                recordingFinished(true, "Recording finished.", output);
            } catch (Exception e) {
                Log.error(e);
                String message = e.getMessage() == null || e.getMessage().isBlank() ? "Recording failed." : e.getMessage();
                recordingFinished(false, message, null);
            }
            System.gc();
        }
    }

    private ExportMovie() {
    }

}
