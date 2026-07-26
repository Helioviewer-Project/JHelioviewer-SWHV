package org.helioviewer.jhv.movie;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.image.nio.MappedImageFactory;
import org.helioviewer.jhv.image.nio.NativeImageFactory;
import org.helioviewer.jhv.opengl.GLGrab;
import org.helioviewer.jhv.thread.AppThread;

public final class ExportMovie {

    public interface StatusListener {
        void recordingStatusChanged();
    }

    @FunctionalInterface
    public interface TimelineFrameSource {
        @Nullable
        TimelineFrame getFrame();
    }

    public record TimelineFrame(BufferedImage image, int movieLinePosition) {}

    private static final ExecutorService encodeExecutor = Executors.newSingleThreadExecutor(new AppThread.NamedThreadFactory("JHV-EncodeMovie"));
    private static final ArrayList<StatusListener> statusListeners = new ArrayList<>();

    private static ExportWriter exporter;
    private static GLGrab grabber;

    private static ViewState.RecordingMode mode;
    private static boolean recording;
    private static boolean shallStop;
    private static boolean mainCanvasVisible = true;
    private static @Nullable Commands.OperationContext operationContext;
    private static @Nullable TimelineFrameSource timelineFrameSource;

    public static void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            if (keep) {
                encodeExecutor.execute(new CloseWriter(exporter));
            } else {
                for (Runnable runnable : encodeExecutor.shutdownNow()) {
                    if (runnable instanceof FrameConsumer frameConsumer) {
                        NativeImageFactory.free(frameConsumer.timelineImage());
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

    public static void renderedFrame() {
        if (!isCanvasRecording())
            return;
        captureFrame();
        Player.grabDone();
    }

    private static void captureFrame() {
        BufferedImage screen = null;
        BufferedImage timeline = null;
        boolean submitted = false;
        try {
            TimelineFrame timelineFrame = getTimelineFrame();
            int timelineHeight = timelineFrame == null ? 0 : timelineHeight(timelineFrame.image());
            if (mainCanvasVisible) {
                int canvasHeight = exporter.height() - timelineHeight;
                ensureGrabber(canvasHeight);
                screen = MappedImageFactory.createRGBImage(grabber.w, grabber.h);
                grabber.renderFrame(MappedImageFactory.getByteBuffer(screen));
            } else if (timelineFrame == null) {
                throw new IllegalStateException("The timeline is not available.");
            }

            int movieLinePosition = -1;
            if (timelineFrame != null) {
                timeline = NativeImageFactory.copyImage(timelineFrame.image());
                movieLinePosition = timelineFrame.movieLinePosition();
            }
            encodeExecutor.execute(new FrameConsumer(exporter, screen, timeline, movieLinePosition));
            submitted = true;
        } catch (Exception e) {
            Log.error(e);
        } finally {
            if (!submitted) {
                NativeImageFactory.free(timeline);
                MappedImageFactory.free(screen);
            }
        }

        if (shallStop) {
            if (mainCanvasVisible && grabber != null) {
                grabber.dispose();
                grabber = null;
            }
            stop();
        }
    }

    private static void ensureGrabber(int height) {
        if (grabber != null && grabber.w == exporter.width() && grabber.h == height)
            return;
        if (grabber != null)
            grabber.dispose();
        grabber = new GLGrab(exporter.width(), height);
    }

    private static int timelineHeight(BufferedImage image) {
        int height = (int) (image.getHeight() / (double) image.getWidth() * exporter.width() + .5);
        return mainCanvasVisible ? Math.min(height, exporter.height() - 1) : exporter.height();
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

        ViewState.Size size = recordingData.size().getSize();
        int width = size.width();
        int height = size.height();
        boolean internal = size.internal();

        mode = recordingData.mode();
        int canvasWidth = mode == ViewState.RecordingMode.SHOT ? width : (width / MACROBLOCK) * MACROBLOCK;
        TimelineFrame timelineFrame = getTimelineFrame();
        int timelineHeight = timelineFrame == null ? 0 :
                (int) (timelineFrame.image().getHeight() / (double) timelineFrame.image().getWidth() * canvasWidth + .5);
        int exportHeight = internal ? height : mainCanvasVisible ? height + timelineHeight : timelineHeight;
        if (mode != ViewState.RecordingMode.SHOT)
            exportHeight = (exportHeight / MACROBLOCK) * MACROBLOCK;
        if (!mainCanvasVisible && timelineFrame == null)
            throw new IllegalStateException("The timeline is not available.");

        if (mode == ViewState.RecordingMode.SHOT) {
            exporter = new ExportWriter(ExportFormat.PNG, canvasWidth, exportHeight, fps);
            shallStop = true;

            recording = true;
            notifyStatusChanged();
            if (mainCanvasVisible)
                DisplayController.render(1);
            else
                captureFrame();
        } else {
            ExportFormat format = ExportFormat.H264;
            try {
                format = ExportFormat.valueOf(Settings.getProperty("video.format"));
            } catch (Exception ignore) {}
            exporter = new ExportWriter(format, canvasWidth, exportHeight, fps);

            recording = true;
            notifyStatusChanged();

            if (mode == ViewState.RecordingMode.LOOP) {
                Commands.seekFrame(0);
                Commands.play();
            }
        }
    }

    private static void stop() {
        recording = false;
        notifyStatusChanged();

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

    static void playerFrameChanged(boolean last) {
        if (!recording)
            return;
        if (mode == ViewState.RecordingMode.LOOP && last)
            shallStop = true;
        if (!mainCanvasVisible)
            captureFrame();
    }

    public static void shallStop() {
        if (!isRecording())
            return;
        shallStop = true;
        if (mainCanvasVisible)
            DisplayController.display(); // force detach
        else
            captureFrame();
    }

    public static boolean isRecording() {
        return recording;
    }

    static boolean isCanvasRecording() {
        return recording && mainCanvasVisible;
    }

    public static void setMainCanvasVisible(boolean visible) {
        if (mainCanvasVisible == visible)
            return;
        mainCanvasVisible = visible;
        if (!visible) {
            boolean pendingFrame = Player.grabDone();
            if (recording && (pendingFrame || shallStop))
                captureFrame();
        }
    }

    public static void setTimelineFrameSource(@Nullable TimelineFrameSource source) {
        timelineFrameSource = source;
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

    private static @Nullable TimelineFrame getTimelineFrame() {
        return timelineFrameSource == null ? null : timelineFrameSource.getFrame();
    }

    private record FrameConsumer(ExportWriter exportWriter, @Nullable BufferedImage mainImage,
                                 @Nullable BufferedImage timelineImage,
                                 int movieLinePosition) implements Runnable {
        @Override
        public void run() {
            try {
                exportWriter.encode(mainImage, timelineImage, movieLinePosition);
            } catch (Exception e) {
                Log.error(e);
            } finally {
                NativeImageFactory.free(timelineImage);
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
