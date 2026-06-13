package org.helioviewer.jhv.movie;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.image.nio.MappedImageFactory;
import org.helioviewer.jhv.image.nio.NativeImageFactory;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.thread.JHVThread;

public final class ExportMovie implements Player.Listener {

    public interface StatusListener {
        void recordingStatusChanged();
    }

    private static final ExportMovie instance = new ExportMovie();
    private static final ExecutorService encodeExecutor = Executors.newSingleThreadExecutor(new JHVThread.NamedThreadFactory("JHV-EncodeMovie"));
    private static final ArrayList<StatusListener> statusListeners = new ArrayList<>();

    private static ExportWriter exporter;
    private static GLGrab grabber;

    private static ViewState.RecordingMode mode;
    private static boolean recording;
    private static boolean shallStop;
    private static @Nullable Commands.OperationContext operationContext;

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

    public static void dispose() {
        if (grabber != null)
            grabber.dispose();
    }

    public static void handleMovieExport() {
        BufferedImage screen = null;
        BufferedImage eve = null;
        boolean submitted = false;
        try {
            screen = MappedImageFactory.createRGBImage(grabber.w, grabber.h);
            grabber.renderFrame(MappedImageFactory.getByteBuffer(screen));
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
        Player.grabDone();

        if (shallStop) {
            grabber.dispose();
            stop();
        }
    }

    private static final int MACROBLOCK = 8;

    public static void start(@Nullable Commands.OperationContext context, @Nullable Commands.RecordStartInput input) {
        if (isRecording()) {
            if (context != null)
                Commands.notifyRecordingFinished(context, false, "Recording already in progress.", null);
            return;
        }

        operationContext = context;
        try {
            if (input != null)
                ViewState.applyRecordStartUpdate(input.mode(), input.size(), input.advanceMode(), input.speed(), input.speedUnit());

            ViewState.PlaybackData playbackData = ViewState.playbackData();
            int fps = playbackData.speedUnit().isRelative() ? playbackData.speed() : Player.FPS_ABSOLUTE;
            startRecording(ViewState.recordingData(), fps);
        } catch (Exception e) {
            Log.error(e);
            recording = false;
            shallStop = false;
            Player.removeFrameListener(instance);
            if (grabber != null) {
                grabber.dispose();
                grabber = null;
            }
            exporter = null;
            notifyStatusChanged();
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "Recording failed." : e.getMessage();
            recordingFinished(false, message, null);
        }
    }

    private static void startRecording(ViewState.RecordingData recordingData, int fps) {
        shallStop = false;

        int scrw = 1;
        int scrh = 0;
        if (EVEImage != null) {
            scrw = Math.max(1, EVEImage.getWidth());
            scrh = EVEImage.getHeight();
        }

        ViewState.Size size = recordingData.size().getSize();
        int width = size.width();
        int height = size.height();
        boolean internal = size.internal();

        mode = recordingData.mode();
        int canvasWidth = mode == ViewState.RecordingMode.SHOT ? width : (width / MACROBLOCK) * MACROBLOCK;
        int sh = (int) (scrh / (double) scrw * canvasWidth + .5);
        int canvasHeight = internal ? height - sh : height;
        int exportHeight = mode == ViewState.RecordingMode.SHOT ? canvasHeight + sh : ((canvasHeight + sh) / MACROBLOCK) * MACROBLOCK;

        canvasHeight = exportHeight - sh;
        grabber = new GLGrab(canvasWidth, canvasHeight);

        if (mode == ViewState.RecordingMode.SHOT) {
            exporter = new ExportWriter(ExportFormat.PNG, canvasWidth, exportHeight, fps);
            shallStop = true;

            recording = true;
            notifyStatusChanged();
            DisplayController.render(1);
        } else {
            ExportFormat format = ExportFormat.H264;
            try {
                format = ExportFormat.valueOf(Settings.getProperty("video.format"));
            } catch (Exception ignore) {}
            exporter = new ExportWriter(format, canvasWidth, exportHeight, fps);

            recording = true;
            notifyStatusChanged();

            if (mode == ViewState.RecordingMode.LOOP) {
                Player.addFrameListener(instance);
                Commands.seekFrame(0);
                Commands.play();
            }
        }
    }

    private static void stop() {
        recording = false;
        notifyStatusChanged();
        if (mode == ViewState.RecordingMode.LOOP) {
            Player.removeFrameListener(instance);
        }

        try {
            disposeMovieWriter(true);
        } catch (Exception e) {
            Log.error(e);
            exporter = null;
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "Recording failed." : e.getMessage();
            recordingFinished(false, message, null);
        }
    }

    private static void recordingFinished(boolean success, String message, @Nullable String output) {
        Commands.notifyRecordingFinished(operationContext, success, message, output);
        operationContext = null;
    }

    // loop mode only
    @Override
    public void frameChanged(int frame, boolean last) {
        if (last)
            shallStop = true;
    }

    public static void shallStop() {
        if (!isRecording())
            return;
        shallStop = true;
        DisplayController.display(); // force detach
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void addStatusListener(StatusListener listener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener);
            listener.recordingStatusChanged();
        }
    }

    public static void removeStatusListener(StatusListener listener) {
        statusListeners.remove(listener);
    }

    private static void notifyStatusChanged() {
        statusListeners.forEach(StatusListener::recordingStatusChanged);
    }

    private record FrameConsumer(ExportWriter exportWriter, BufferedImage mainImage, BufferedImage eveImage,
                                 int movieLinePosition) implements Runnable {
        @Override
        public void run() {
            try {
                exportWriter.encode(mainImage, eveImage, movieLinePosition);
            } catch (Exception e) {
                Log.error(e);
            } finally {
                NativeImageFactory.free(eveImage);
                MappedImageFactory.free(mainImage);
            }
        }
    }

    private record CloseWriter(ExportWriter exportWriter) implements Runnable {
        @Override
        public void run() {
            try {
                String output = exportWriter.close();
                recordingFinished(true, "Recording finished.", output);
            } catch (Exception e) {
                Log.error(e);
                String message = e.getMessage() == null || e.getMessage().isBlank() ? "Recording failed." : e.getMessage();
                recordingFinished(false, message, null);
            }
            System.gc();
        }
    }

    private ExportMovie() {}
}
