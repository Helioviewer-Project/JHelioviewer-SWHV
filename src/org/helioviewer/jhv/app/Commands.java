package org.helioviewer.jhv.app;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.camera.ViewActions;
import org.helioviewer.jhv.display.ProjectionMode;
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

    public interface RegisteredCommand {
        String id();
    }

    public interface Command<I> extends RegisteredCommand {
        void run(@Nullable I input);
    }

    private interface IntCommand extends RegisteredCommand {
        void run(int input);
    }

    private interface TimeCommand extends RegisteredCommand {
        void run(@Nullable JHVTime input);
    }

    private interface QuatCommand extends RegisteredCommand {
        void run(@Nullable Quat input);
    }

    private record BasicCommand<I>(String id, Consumer<I> runner) implements Command<I> {
        @Override
        public void run(@Nullable I input) {
            runner.accept(input);
        }
    }

    private record BasicIntCommand(String id, IntConsumer runner) implements IntCommand {
        @Override
        public void run(int input) {
            runner.accept(input);
        }
    }

    private record BasicTimeCommand(String id, Consumer<JHVTime> runner) implements TimeCommand {
        @Override
        public void run(@Nullable JHVTime input) {
            runner.accept(input);
        }
    }

    private record BasicQuatCommand(String id, Consumer<Quat> runner) implements QuatCommand {
        @Override
        public void run(@Nullable Quat input) {
            runner.accept(input);
        }
    }

    public record SetViewStateArgs(
            @Nullable ProjectionMode projection,
            @Nullable Interaction.AnnotationMode annotationMode,
            @Nullable Boolean multiview,
            @Nullable Boolean tracking,
            @Nullable Boolean refresh,
            @Nullable Boolean showCorona,
            @Nullable Boolean differentialRotation) {
    }

    public record SetPlaybackArgs(
            @Nullable Movie.AdvanceMode advanceMode,
            @Nullable Integer speed,
            @Nullable ViewState.PlaybackSpeedUnit speedUnit,
            @Nullable Integer firstFrame,
            @Nullable Integer lastFrame) {
    }

    public record SetRecordingArgs(
            @Nullable ViewState.RecordingMode mode,
            @Nullable ViewState.RecordingSize size) {
    }

    public record RecordStartArgs(
            @Nullable String mode,
            @Nullable String size,
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit) {
    }

    public record OperationContext(
            Class<?> owner,
            @Nullable String clientId,
            @Nullable String requestId,
            @Nullable String mtype) {
    }

    public interface CompletionListener {
        void loadStateFinished(@Nullable OperationContext context, boolean success, String message);

        void recordingFinished(
                @Nullable OperationContext context,
                boolean success,
                String message,
                @Nullable String output);
    }

    private static final ArrayList<CompletionListener> completionListeners = new ArrayList<>();

    private static final Command<SetViewStateArgs> setViewStateCommand = new BasicCommand<>(SET_VIEW_STATE, input -> {
        if (input == null)
            return;
        ViewState.applyModeUpdate(
                input.projection(),
                input.annotationMode(),
                input.multiview(),
                input.tracking(),
                input.refresh(),
                input.showCorona(),
                input.differentialRotation());
    });
    private static final Command<SetPlaybackArgs> setPlaybackCommand = new BasicCommand<>(SET_PLAYBACK, input -> {
        if (input == null)
            return;
        ViewState.applyPlaybackUpdate(
                input.advanceMode(),
                input.speed(),
                input.speedUnit(),
                input.firstFrame(),
                input.lastFrame());
    });
    private static final Command<Void> playCommand = new BasicCommand<>(PLAY, input -> Movie.play());
    private static final Command<Void> pauseCommand = new BasicCommand<>(PAUSE, input -> Movie.pause());
    private static final Command<Void> togglePlaybackCommand = new BasicCommand<>(TOGGLE_PLAYBACK, input -> Movie.toggle());
    private static final IntCommand seekFrameCommand = new BasicIntCommand(SEEK_FRAME, Movie::setFrame);
    private static final TimeCommand seekTimeCommand = new BasicTimeCommand(SEEK_TIME, input -> {
        if (input == null)
            return;
        Movie.setTime(input);
    });
    private static final Command<Void> nextFrameCommand = new BasicCommand<>(NEXT_FRAME, input -> Movie.nextFrame());
    private static final Command<Void> previousFrameCommand = new BasicCommand<>(PREVIOUS_FRAME, input -> Movie.previousFrame());
    private static final Command<SetRecordingArgs> setRecordingCommand = new BasicCommand<>(SET_RECORDING, input -> {
        if (input == null)
            return;
        ViewState.applyRecordingUpdate(input.mode(), input.size());
    });
    private static final Command<RecordStartArgs> recordStartCommand = new BasicCommand<>(RECORD_START, input -> ExportMovie.start(null, input));
    private static final Command<Void> recordStopCommand = new BasicCommand<>(RECORD_STOP, input -> ExportMovie.shallStop());
    private static final Command<Object> loadStateCommand = new BasicCommand<>(LOAD_STATE, Load::state);
    private static final Command<Object> loadRequestCommand = new BasicCommand<>(LOAD_REQUEST, Load::request);
    private static final Command<Object> loadSunJSONCommand = new BasicCommand<>(LOAD_SUN_JSON, Load::sunJSON);
    private static final Command<Object> loadImageCommand = new BasicCommand<>(LOAD_IMAGE, Load::image);
    private static final Command<Object> loadCDFCommand = new BasicCommand<>(LOAD_CDF, Load::cdf);
    private static final Command<URI> loadVOTableCommand = new BasicCommand<>(LOAD_VOTABLE, input -> {
        if (input == null)
            return;
        Load.votable(input);
    });
    private static final Command<Object> loadHapiCommand = new BasicCommand<>(LOAD_HAPI, Load::hapi);
    private static final Command<Void> zoomInCommand = new BasicCommand<>(ZOOM_IN, input -> ViewActions.zoomIn());
    private static final Command<Void> zoomOutCommand = new BasicCommand<>(ZOOM_OUT, input -> ViewActions.zoomOut());
    private static final Command<Void> zoomFitCommand = new BasicCommand<>(ZOOM_FIT, input -> ViewActions.zoomFit());
    private static final Command<Void> zoomOneToOneCommand = new BasicCommand<>(ZOOM_ONE_TO_ONE, input -> ViewActions.zoomOneToOne());
    private static final Command<Void> resetViewCommand = new BasicCommand<>(RESET_VIEW, input -> ViewActions.resetView());
    private static final Command<Void> resetViewAxisCommand = new BasicCommand<>(RESET_VIEW_AXIS, input -> ViewActions.resetViewAxis());
    private static final QuatCommand rotateView90Command = new BasicQuatCommand(ROTATE_VIEW_90, ViewActions::rotateView90);

    public static final class Registry {
        private static final LinkedHashMap<String, RegisteredCommand> commands = new LinkedHashMap<>();

        static {
            register(setViewStateCommand);
            register(setPlaybackCommand);
            register(playCommand);
            register(pauseCommand);
            register(togglePlaybackCommand);
            register(seekFrameCommand);
            register(seekTimeCommand);
            register(nextFrameCommand);
            register(previousFrameCommand);
            register(setRecordingCommand);
            register(recordStartCommand);
            register(recordStopCommand);
            register(loadStateCommand);
            register(loadRequestCommand);
            register(loadSunJSONCommand);
            register(loadImageCommand);
            register(loadCDFCommand);
            register(loadVOTableCommand);
            register(loadHapiCommand);
            register(zoomInCommand);
            register(zoomOutCommand);
            register(zoomFitCommand);
            register(zoomOneToOneCommand);
            register(resetViewCommand);
            register(resetViewAxisCommand);
            register(rotateView90Command);
        }

        private Registry() {
        }

        private static void register(RegisteredCommand command) {
            commands.put(command.id(), command);
        }

        private static RegisteredCommand require(String id) {
            RegisteredCommand command = commands.get(id);
            if (command == null)
                throw new IllegalArgumentException("Unknown command: " + id);
            return command;
        }

        public static Collection<String> ids() {
            return commands.keySet();
        }

        @Nullable
        public static RegisteredCommand get(String id) {
            return commands.get(id);
        }

        @SuppressWarnings("unchecked")
        public static <I> void run(String id, @Nullable I input) {
            RegisteredCommand command = require(id);
            if (!(command instanceof Command<?>))
                throw new IllegalArgumentException("Command does not accept object input: " + id);
            ((Command<I>) command).run(input);
        }

        public static void run(String id, int input) {
            RegisteredCommand command = require(id);
            if (!(command instanceof IntCommand intCommand))
                throw new IllegalArgumentException("Command does not accept int input: " + id);
            intCommand.run(input);
        }

        public static void run(String id, @Nullable JHVTime input) {
            RegisteredCommand command = require(id);
            if (!(command instanceof TimeCommand timeCommand))
                throw new IllegalArgumentException("Command does not accept time input: " + id);
            timeCommand.run(input);
        }

        public static void run(String id, @Nullable Quat input) {
            RegisteredCommand command = require(id);
            if (!(command instanceof QuatCommand quatCommand))
                throw new IllegalArgumentException("Command does not accept rotation input: " + id);
            quatCommand.run(input);
        }
    }

    public static void setViewState(@Nullable SetViewStateArgs args) {
        setViewStateCommand.run(args);
    }

    public static void setPlayback(@Nullable SetPlaybackArgs args) {
        setPlaybackCommand.run(args);
    }

    public static void setPlaybackRange(int firstFrame, int lastFrame) {
        ViewState.setPlaybackRange(firstFrame, lastFrame);
    }

    public static void play() {
        playCommand.run(null);
    }

    public static void pause() {
        pauseCommand.run(null);
    }

    public static void togglePlayback() {
        togglePlaybackCommand.run(null);
    }

    public static void seekFrame(int frame) {
        seekFrameCommand.run(frame);
    }

    public static void seekTime(@Nullable JHVTime time) {
        seekTimeCommand.run(time);
    }

    public static void nextFrame() {
        nextFrameCommand.run(null);
    }

    public static void previousFrame() {
        previousFrameCommand.run(null);
    }

    public static void setRecording(@Nullable SetRecordingArgs args) {
        setRecordingCommand.run(args);
    }

    public static void recordStart(@Nullable RecordStartArgs args) {
        recordStartCommand.run(args);
    }

    public static void recordStart(@Nullable OperationContext context, @Nullable RecordStartArgs args) {
        ExportMovie.start(context, args);
    }

    public static void recordStop() {
        recordStopCommand.run(null);
    }

    public static void loadState(URI uri) {
        loadStateCommand.run(uri);
    }

    public static void loadState(@Nullable OperationContext context, URI uri) {
        Load.state(context, uri);
    }

    public static void loadState(String json) {
        loadStateCommand.run(json);
    }

    public static void loadState(@Nullable OperationContext context, String json) {
        Load.state(context, json);
    }

    public static void loadRequest(URI uri) {
        loadRequestCommand.run(uri);
    }

    public static void loadRequest(String json) {
        loadRequestCommand.run(json);
    }

    public static void loadSunJSON(URI uri) {
        loadSunJSONCommand.run(uri);
    }

    public static void loadSunJSON(String json) {
        loadSunJSONCommand.run(json);
    }

    public static void loadImage(URI uri) {
        loadImageCommand.run(uri);
    }

    public static void loadImage(List<URI> uris) {
        loadImageCommand.run(uris);
    }

    public static void loadCDF(URI uri) {
        loadCDFCommand.run(uri);
    }

    public static void loadCDF(List<URI> uris) {
        loadCDFCommand.run(uris);
    }

    public static void loadVOTable(URI uri) {
        loadVOTableCommand.run(uri);
    }

    public static void loadHapi(URI uri) {
        loadHapiCommand.run(uri);
    }

    public static void loadHapi(List<URI> uris) {
        loadHapiCommand.run(uris);
    }

    public static void zoomIn() {
        zoomInCommand.run(null);
    }

    public static void zoomOut() {
        zoomOutCommand.run(null);
    }

    public static void zoomFit() {
        zoomFitCommand.run(null);
    }

    public static void zoomOneToOne() {
        zoomOneToOneCommand.run(null);
    }

    public static void resetView() {
        resetViewCommand.run(null);
    }

    public static void resetViewAxis() {
        resetViewAxisCommand.run(null);
    }

    public static void rotateView90(@Nullable Quat rotation) {
        rotateView90Command.run(rotation);
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

    public static void notifyRecordingFinished(
            @Nullable OperationContext context,
            boolean success,
            String message,
            @Nullable String output) {
        completionListeners.forEach(listener -> listener.recordingFinished(context, success, message, output));
    }

}
