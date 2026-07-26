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
        @Nullable TimelineFrame getFrame();
    }

    public record TimelineFrame(BufferedImage image, int movieLinePosition) {}

    private static final int MACROBLOCK = 8;
    private static final ExecutorService encodeExecutor = Executors.newSingleThreadExecutor(
            new AppThread.NamedThreadFactory("JHV-EncodeMovie"));
    private static final ArrayList<StatusListener> statusListeners = new ArrayList<>();

    private static @Nullable RecordingSession recordingSession;
    private static @Nullable TimelineFrameSource timelineFrameSource;
    private static boolean mainCanvasVisible = true;

    public static void start(@Nullable Commands.OperationContext context, @Nullable Commands.RecordStartInput input) {
        if (isRecording()) {
            if (context != null)
                Commands.notifyRecordingFinished(context, false, "Recording already in progress.", null);
            return;
        }

        try {
            if (input != null)
                ViewState.applyRecordStartUpdate(input.mode(), input.size(), input.advanceMode(), input.speed(), input.speedUnit());

            ViewState.PlaybackData playbackData = ViewState.playbackData();
            int fps = playbackData.speedUnit().isRelative() ? playbackData.speed() : Player.FPS_ABSOLUTE;
            recordingSession = new RecordingSession(context, ViewState.recordingData(), fps);
            notifyStatusChanged();
            recordingSession.start();
        } catch (Exception e) {
            Log.error(e);
            RecordingSession failedSession = recordingSession;
            recordingSession = null;
            if (failedSession != null)
                failedSession.disposeGrabber();
            notifyStatusChanged();
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "Recording failed." : e.getMessage();
            Commands.notifyRecordingFinished(context, false, message, null);
        }
    }

    public static void shallStop() {
        RecordingSession session = recordingSession;
        if (session != null)
            session.requestStop();
    }

    public static boolean isRecording() {
        return recordingSession != null;
    }

    static boolean beginPlaybackFrame() {
        RecordingSession session = recordingSession;
        return session == null || session.beginPlaybackFrame();
    }

    static void playbackFrameReady(boolean last) {
        RecordingSession session = recordingSession;
        if (session != null)
            session.playbackFrameReady(last);
    }

    public static void renderedFrame() {
        RecordingSession session = recordingSession;
        if (session != null)
            session.renderedFrame();
    }

    public static void setMainCanvasVisible(boolean visible) {
        if (mainCanvasVisible == visible)
            return;
        mainCanvasVisible = visible;
        RecordingSession session = recordingSession;
        if (session != null)
            session.canvasVisibilityChanged();
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

    public static void dispose() {
        RecordingSession session = recordingSession;
        if (session != null)
            session.disposeGrabber();
    }

    public static void shutdown() {
        recordingSession = null;
        for (Runnable runnable : encodeExecutor.shutdownNow()) {
            if (runnable instanceof FrameConsumer frameConsumer) {
                NativeImageFactory.free(frameConsumer.timelineImage());
                MappedImageFactory.free(frameConsumer.mainImage());
            }
        }
    }

    private static final class RecordingSession {
        private final @Nullable Commands.OperationContext operationContext;
        private final ViewState.RecordingMode mode;
        private final int width;
        private final int height;
        private final ExportWriter writer;

        private @Nullable GLGrab grabber;
        private boolean canvasFramePending;
        private boolean stopAfterFrame;

        RecordingSession(@Nullable Commands.OperationContext _operationContext,
                         ViewState.RecordingData recordingData, int fps) {
            operationContext = _operationContext;
            mode = recordingData.mode();
            if (mode == ViewState.RecordingMode.LOOP && !Player.hasActiveImage())
                throw new IllegalStateException("Loop recording requires an active image.");

            ViewState.Size size = recordingData.size().getSize();
            width = mode == ViewState.RecordingMode.SHOT ?
                    size.width() : size.width() / MACROBLOCK * MACROBLOCK;

            TimelineFrame timelineFrame = getTimelineFrame();
            if (!mainCanvasVisible && timelineFrame == null)
                throw new IllegalStateException("The timeline is not available.");

            int timelineHeight = timelineFrame == null ? 0 :
                    scaledHeight(timelineFrame.image(), width);
            int outputHeight = size.internal() ? size.height() :
                    mainCanvasVisible ? size.height() + timelineHeight : timelineHeight;
            if (mode != ViewState.RecordingMode.SHOT)
                outputHeight = outputHeight / MACROBLOCK * MACROBLOCK;
            height = outputHeight;

            writer = new ExportWriter(mode == ViewState.RecordingMode.SHOT ?
                    ExportFormat.PNG : videoFormat(), width, height, fps);
        }

        private static ExportFormat videoFormat() {
            try {
                return ExportFormat.valueOf(Settings.getProperty("video.format"));
            } catch (Exception ignore) {
                return ExportFormat.H264;
            }
        }

        void start() {
            if (mode == ViewState.RecordingMode.SHOT) {
                stopAfterFrame = true;
                if (mainCanvasVisible) {
                    canvasFramePending = true;
                    DisplayController.render(1);
                } else {
                    captureFrame();
                }
            } else if (mode == ViewState.RecordingMode.LOOP) {
                Commands.seekFrame(0);
                Commands.play();
            }
        }

        boolean beginPlaybackFrame() {
            if (canvasFramePending)
                return false;
            canvasFramePending = mainCanvasVisible;
            return true;
        }

        void playbackFrameReady(boolean last) {
            if (mode == ViewState.RecordingMode.LOOP && last)
                stopAfterFrame = true;
            if (!mainCanvasVisible)
                captureFrame();
        }

        void renderedFrame() {
            if (!mainCanvasVisible || (mode != ViewState.RecordingMode.FREE && !canvasFramePending))
                return;
            canvasFramePending = false;
            captureFrame();
        }

        void canvasVisibilityChanged() {
            if (!mainCanvasVisible && (canvasFramePending || stopAfterFrame)) {
                canvasFramePending = false;
                captureFrame();
            }
        }

        void requestStop() {
            stopAfterFrame = true;
            if (mainCanvasVisible) {
                canvasFramePending = true;
                DisplayController.display();
            } else {
                captureFrame();
            }
        }

        private void captureFrame() {
            if (recordingSession != this)
                return;

            BufferedImage mainImage = null;
            BufferedImage timelineImage = null;
            boolean submitted = false;
            try {
                TimelineFrame timelineFrame = getTimelineFrame();
                int timelineHeight = timelineFrame == null ? 0 : timelineHeight(timelineFrame.image());
                if (mainCanvasVisible) {
                    int mainHeight = height - timelineHeight;
                    ensureGrabber(mainHeight);
                    mainImage = MappedImageFactory.createRGBImage(grabber.w, grabber.h);
                    grabber.renderFrame(MappedImageFactory.getByteBuffer(mainImage));
                } else if (timelineFrame == null) {
                    throw new IllegalStateException("The timeline is not available.");
                }

                int movieLinePosition = -1;
                if (timelineFrame != null) {
                    timelineImage = NativeImageFactory.copyImage(timelineFrame.image());
                    movieLinePosition = timelineFrame.movieLinePosition();
                }
                encodeExecutor.execute(new FrameConsumer(writer, mainImage, timelineImage, movieLinePosition));
                submitted = true;
            } catch (Exception e) {
                Log.error(e);
                writer.recordFailure(e);
            } finally {
                if (!submitted) {
                    NativeImageFactory.free(timelineImage);
                    MappedImageFactory.free(mainImage);
                }
            }

            if (stopAfterFrame)
                finish();
        }

        private void ensureGrabber(int grabberHeight) {
            if (grabber != null && grabber.w == width && grabber.h == grabberHeight)
                return;
            if (grabber != null)
                grabber.dispose();
            grabber = new GLGrab(width, grabberHeight);
        }

        private int timelineHeight(BufferedImage image) {
            if (!mainCanvasVisible)
                return height;
            return Math.min(scaledHeight(image, width), height - 1);
        }

        private static int scaledHeight(BufferedImage image, int width) {
            return (int) (image.getHeight() / (double) image.getWidth() * width + .5);
        }

        private void finish() {
            if (recordingSession != this)
                return;
            recordingSession = null;
            disposeGrabber();
            notifyStatusChanged();
            encodeExecutor.execute(new CloseWriter(writer, operationContext));
        }

        void disposeGrabber() {
            if (grabber != null) {
                grabber.dispose();
                grabber = null;
            }
        }
    }

    private record FrameConsumer(ExportWriter writer, @Nullable BufferedImage mainImage,
                                 @Nullable BufferedImage timelineImage,
                                 int movieLinePosition) implements Runnable {
        @Override
        public void run() {
            try {
                writer.encode(mainImage, timelineImage, movieLinePosition);
            } catch (Exception e) {
                Log.error(e);
                writer.recordFailure(e);
            } finally {
                NativeImageFactory.free(timelineImage);
                MappedImageFactory.free(mainImage);
            }
        }
    }

    private record CloseWriter(ExportWriter writer,
                               @Nullable Commands.OperationContext operationContext) implements Runnable {
        @Override
        public void run() {
            try {
                String output = writer.close();
                Commands.notifyRecordingFinished(operationContext, true, "Recording finished.", output);
            } catch (Exception e) {
                Log.error(e);
                String message = e.getMessage() == null || e.getMessage().isBlank() ?
                        "Recording failed." : e.getMessage();
                Commands.notifyRecordingFinished(operationContext, false, message, null);
            }
            System.gc();
        }
    }

    private ExportMovie() {}
}
