package org.helioviewer.jhv.app;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONObject;

public final class Commands {

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

    public static void setViewState(JSONObject input) {
        boolean wasRefresh = ViewState.isRefresh();
        ViewState.applyMode(ViewState.readModeJson(input));
        if (!wasRefresh && ViewState.isRefresh())
            ImageLayers.refreshLayersSpan();
    }

    public static void setRefresh(boolean refresh) {
        boolean wasRefresh = ViewState.isRefresh();
        ViewState.setRefresh(refresh);
        if (!wasRefresh && refresh)
            ImageLayers.refreshLayersSpan();
    }

    public static void setPlayback(JSONObject input) {
        ViewState.applyPlayback(input);
    }

    public static void setPlaybackRange(int firstFrame, int lastFrame) {
        Player.setPlaybackRange(firstFrame, lastFrame);
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

    public static void setRecording(JSONObject input) {
        ViewState.applyRecording(input);
    }

    public static void recordStart(@Nullable OperationContext context, @Nullable JSONObject input) {
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
