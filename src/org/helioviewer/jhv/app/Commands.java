package org.helioviewer.jhv.app;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.JHVTime;

public final class Commands {

    public record PlaybackInput(
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit,
            @Nullable String firstFrame,
            @Nullable String lastFrame) {}

    public record RecordStartInput(
            @Nullable String mode,
            @Nullable String size,
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit) {}

    public record OperationContext(@Nullable String clientId, @Nullable String requestId,
                                   @Nullable String mtype, @Nullable Completion completion) {
        public void complete(boolean success, String message, @Nullable String output) {
            if (completion != null)
                completion.finished(this, success, message, output);
        }
    }

    @FunctionalInterface
    public interface Completion {
        void finished(OperationContext context, boolean success, String message, @Nullable String output);
    }

    public static void setViewStateRaw(
            @Nullable String projection,
            @Nullable String annotationMode,
            @Nullable String multiview,
            @Nullable String tracking,
            @Nullable String refresh,
            @Nullable String showCorona,
            @Nullable String differentialRotation) {
        ViewState.applyModeUpdateRaw(projection, annotationMode, multiview, tracking, refresh, showCorona,
                differentialRotation);
    }

    public static void setPlayback(@Nullable PlaybackInput input) {
        if (input == null)
            return;
        ViewState.applyPlaybackUpdateRaw(
                input.advanceMode(),
                input.speed(),
                input.speedUnit(),
                input.firstFrame(),
                input.lastFrame());
    }

    public static void setPlaybackRange(int firstFrame, int lastFrame) {
        ViewState.setPlaybackRange(firstFrame, lastFrame);
    }

    public static void play() {
        Player.play();
    }

    public static void pause() {
        Player.pause();
    }

    public static void togglePlayback() {
        Player.toggle();
    }

    public static void seekFrame(int frame) {
        Player.setFrame(frame);
    }

    public static void seekTime(JHVTime time) {
        Player.setTime(time);
    }

    public static void nextFrame() {
        Player.nextFrame();
    }

    public static void previousFrame() {
        Player.previousFrame();
    }

    public static void setRecordingRaw(@Nullable String mode, @Nullable String size) {
        ViewState.applyRecordingUpdateRaw(mode, size);
    }

    public static void recordStart(@Nullable OperationContext context, @Nullable RecordStartInput input) {
        ExportMovie.start(context, input);
    }

    public static void recordStop() {
        ExportMovie.shallStop();
    }

    public static void zoomIn() {
        DisplayController.zoomIn();
    }

    public static void zoomOut() {
        DisplayController.zoomOut();
    }

    public static void zoomFit() {
        DisplayController.zoomFit();
    }

    public static void zoomOneToOne() {
        DisplayController.zoomOneToOne();
    }

    public static void resetView() {
        DisplayController.resetView();
    }

    public static void resetViewAxis() {
        DisplayController.resetViewAxis();
    }

    public static void rotateView90(@Nullable String axis) {
        DisplayController.rotateView90(axis);
    }

    public static void notifyLoadStateFinished(@Nullable OperationContext context, boolean success, String message) {
        if (context != null)
            context.complete(success, message, null);
    }

    public static void notifyRecordingFinished(@Nullable OperationContext context, boolean success, String message,
                                               @Nullable String output) {
        if (context != null)
            context.complete(success, message, output);
    }

    private Commands() {}
}
