package org.helioviewer.jhv.app;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.camera.ViewActions;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVTime;

public final class Commands {

    private Commands() {
    }

    public static final String SET_VIEW_STATE = "set-view-state";
    public static final String SET_PLAYBACK = "set-playback";
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String TOGGLE_PLAYBACK = "toggle-playback";
    public static final String SEEK_FRAME = "seek-frame";
    public static final String SEEK_TIME = "seek-time";
    public static final String NEXT_FRAME = "next-frame";
    public static final String PREVIOUS_FRAME = "previous-frame";
    public static final String SET_RECORDING = "set-recording";
    public static final String RECORD_START = "record-start";
    public static final String RECORD_STOP = "record-stop";
    public static final String LOAD_STATE = "load-state";
    public static final String LOAD_REQUEST = "load-request";
    public static final String LOAD_SUN_JSON = "load-sunjson";
    public static final String LOAD_IMAGE = "load-image";
    public static final String LOAD_CDF = "load-cdf";
    public static final String LOAD_VOTABLE = "load-votable";
    public static final String LOAD_HAPI = "load-hapi";
    public static final String ZOOM_IN = "zoom-in";
    public static final String ZOOM_OUT = "zoom-out";
    public static final String ZOOM_FIT = "zoom-fit";
    public static final String ZOOM_ONE_TO_ONE = "zoom-one-to-one";
    public static final String RESET_VIEW = "reset-view";
    public static final String RESET_VIEW_AXIS = "reset-view-axis";
    public static final String ROTATE_VIEW_90 = "rotate-view-90";

    public record SetPlaybackArgs(
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit,
            @Nullable String firstFrame,
            @Nullable String lastFrame) {
    }

    public record RecordStartArgs(
            @Nullable String mode,
            @Nullable String size,
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit) {
    }

    public record OperationContext(Class<?> owner, @Nullable String clientId, @Nullable String requestId,
                                   @Nullable String mtype) {
    }

    public interface CompletionListener {
        void loadStateFinished(@Nullable OperationContext context, boolean success, String message);

        void recordingFinished(@Nullable OperationContext context, boolean success, String message,
                               @Nullable String output);
    }

    private static final ArrayList<CompletionListener> completionListeners = new ArrayList<>();

    public static final class Registry {
        private static final LinkedHashMap<String, Consumer<Object>> commands = new LinkedHashMap<>();

        static {
            commands.put(LOAD_STATE, Load::state);
            commands.put(LOAD_REQUEST, Load::request);
            commands.put(LOAD_SUN_JSON, Load::sunJSON);
            commands.put(LOAD_IMAGE, Load::image);
            commands.put(LOAD_CDF, Load::cdf);
            commands.put(LOAD_VOTABLE, input -> {
                if (!(input instanceof URI uri))
                    throw new IllegalArgumentException("load-votable accepts URI");
                Load.votable(uri);
            });
            commands.put(LOAD_HAPI, Load::hapi);
        }

        private Registry() {
        }

        private static Consumer<Object> require(String id) {
            Consumer<Object> command = commands.get(id);
            if (command == null)
                throw new IllegalArgumentException("Unknown command: " + id);
            return command;
        }

        public static void run(String id, @Nullable Object input) {
            require(id).accept(input);
        }
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

    public static void setPlayback(@Nullable SetPlaybackArgs args) {
        if (args == null)
            return;
        ViewState.applyPlaybackUpdateRaw(
                args.advanceMode(),
                args.speed(),
                args.speedUnit(),
                args.firstFrame(),
                args.lastFrame());
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

    public static void seekTime(@Nullable JHVTime time) {
        if (time == null)
            return;
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

    public static void recordStart(@Nullable OperationContext context, @Nullable RecordStartArgs args) {
        ExportMovie.start(context, args);
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

    public static void loadState(String json) {
        Load.state(json);
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

    public static void rotateView90(@Nullable Quat rotation) {
        ViewActions.rotateView90(rotation);
    }

    public static void addCompletionListener(CompletionListener listener) {
        if (!completionListeners.contains(listener))
            completionListeners.add(listener);
    }

    public static void removeCompletionListener(CompletionListener listener) {
        completionListeners.remove(listener);
    }

    public static void notifyLoadStateFinished(@Nullable OperationContext context, boolean success, String message) {
        completionListeners.forEach(listener -> listener.loadStateFinished(context, success, message));
    }

    public static void notifyRecordingFinished(@Nullable OperationContext context, boolean success, String message,
                                               @Nullable String output) {
        completionListeners.forEach(listener -> listener.recordingFinished(context, success, message, output));
    }

}
