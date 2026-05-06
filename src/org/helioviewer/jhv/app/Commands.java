package org.helioviewer.jhv.app;

import java.awt.EventQueue;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.camera.ViewActions;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

public final class Commands {

    private Commands() {}

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

    public record OperationContext(Class<?> owner, @Nullable String clientId, @Nullable String requestId,
                                   @Nullable String mtype) {}

    public interface CompletionListener {
        void loadStateFinished(@Nullable OperationContext context, boolean success, String message);

        void recordingFinished(@Nullable OperationContext context, boolean success, String message,
                               @Nullable String output);
    }

    private static final ArrayList<CompletionListener> completionListeners = new ArrayList<>();

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
        Movie.play();
    }

    public static void pause() {
        Movie.pause();
    }

    public static void togglePlayback() {
        Movie.toggle();
    }

    public static void seekFrame(int frame) {
        Movie.setFrame(frame);
    }

    public static void seekTime(JHVTime time) {
        Movie.setTime(time);
    }

    public static void nextFrame() {
        Movie.nextFrame();
    }

    public static void previousFrame() {
        Movie.previousFrame();
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

    public static void loadState(URI uri) {
        Load.state(uri);
    }

    public static void loadState(@Nullable OperationContext context, URI uri) {
        Load.state(context, uri);
    }

    public static void loadState(@Nullable OperationContext context, String json) {
        Load.state(context, json);
    }

    public static void loadRequest(URI uri) {
        Load.request(uri);
    }

    public static void loadRequest(String json) {
        Load.request(json);
    }

    public static void loadSunJSON(URI uri) {
        Load.sunJSON(uri);
    }

    public static void loadSunJSON(String json) {
        Load.sunJSON(json);
    }

    public static void loadImage(URI uri) {
        Load.image(uri);
    }

    public static void loadImage(List<URI> uris) {
        Load.image(uris);
    }

    public static void loadCDF(URI uri) {
        Load.cdf(uri);
    }

    public static void loadCDF(List<URI> uris) {
        Load.cdf(uris);
    }

    public static void loadVOTable(URI uri) {
        Load.votable(uri);
    }

    public static void loadHapi(URI uri) {
        Load.hapi(uri);
    }

    public static void loadHapi(List<URI> uris) {
        Load.hapi(uris);
    }

    public static void zoomIn() {
        ViewActions.zoomIn();
    }

    public static void zoomOut() {
        ViewActions.zoomOut();
    }

    public static void zoomFit() {
        ViewActions.zoomFit();
    }

    public static void zoomOneToOne() {
        ViewActions.zoomOneToOne();
    }

    public static void resetView() {
        ViewActions.resetView();
    }

    public static void resetViewAxis() {
        ViewActions.resetViewAxis();
    }

    public static void rotateView90(@Nullable String axis) {
        ViewActions.rotateView90(axis);
    }

    public static void addCompletionListener(CompletionListener listener) {
        if (!completionListeners.contains(listener))
            completionListeners.add(listener);
    }

    public static void removeCompletionListener(CompletionListener listener) {
        completionListeners.remove(listener);
    }

    public static void notifyLoadStateFinished(@Nullable OperationContext context, boolean success, String message) {
        EventQueue.invokeLater(() -> completionListeners.forEach(listener -> listener.loadStateFinished(context, success, message)));
    }

    public static void notifyRecordingFinished(@Nullable OperationContext context, boolean success, String message,
                                               @Nullable String output) {
        EventQueue.invokeLater(() -> completionListeners.forEach(listener -> listener.recordingFinished(context, success, message, output)));
    }

}
