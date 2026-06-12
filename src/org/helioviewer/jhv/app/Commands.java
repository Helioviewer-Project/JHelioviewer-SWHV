package org.helioviewer.jhv.app;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.view.uri.FITSViewState;

import org.json.JSONObject;

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
                                   @Nullable String mtype, @Nullable Completion completion) {
        public OperationContext(Class<?> owner, @Nullable String clientId, @Nullable String requestId,
                                @Nullable String mtype) {
            this(owner, clientId, requestId, mtype, null);
        }

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

    public static void setFITSViewState(JSONObject json) {
        FITSViewState.fromJson(json);
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

    public static CompletableFuture<ImageLayer> loadImage(URI uri) {
        return loadImage(List.of(uri));
    }

    public static CompletableFuture<ImageLayer> loadImage(List<URI> uris) {
        return loadImage(uris, null);
    }

    public static CompletableFuture<ImageLayer> loadImage(List<URI> uris, @Nullable JSONObject imageParams) {
        CompletableFuture<ImageLayer> future = new CompletableFuture<>();
        if (uris.isEmpty()) {
            future.complete(null);
            return future;
        }

        FileUtils.resolveURIListOffEDT(uris, "JHV-LoadDirectory", resolved -> {
            if (resolved.isEmpty()) {
                future.complete(null);
                return;
            }

            try {
                ImageLayer layer = ImageLayer.create(null);
                layer.applyImageParams(imageParams);
                layer.load(resolved);
                future.complete(layer);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
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

}
