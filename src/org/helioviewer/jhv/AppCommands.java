package org.helioviewer.jhv;

import java.awt.Dimension;
import java.net.URI;
import java.util.List;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ViewerState;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.io.SoarClient;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.timelines.band.BandReaderHapi;

public final class AppCommands {

    private AppCommands() {
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

    public interface Command<I> {
        String id();

        void run(@Nullable I input) throws Exception;
    }

    @FunctionalInterface
    private interface URILoader {
        void load(URI uri);
    }

    @FunctionalInterface
    private interface JSONLoader {
        void load(String json);
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
            @Nullable ViewerState.PlaybackSpeedUnit speedUnit) {
    }

    public record SeekFrameArgs(int frame) {
    }

    public record SeekTimeArgs(JHVTime time) {
    }

    public record SetRecordingArgs(
            @Nullable ViewerState.RecordingMode mode,
            @Nullable ViewerState.RecordingSize size) {
    }

    public record RecordStartArgs(
            @Nullable ViewerState.RecordingMode mode,
            @Nullable ViewerState.RecordingSize size,
            @Nullable Movie.AdvanceMode advanceMode,
            @Nullable Integer speed,
            @Nullable ViewerState.PlaybackSpeedUnit speedUnit) {
    }

    public record LoadURIOrJSONArgs(
            @Nullable URI uri,
            @Nullable String json) {
    }

    public record LoadURIsArgs(
            List<URI> uris) {
    }

    private static final SetViewStateCommand setViewStateCommand = new SetViewStateCommand();
    private static final SetPlaybackCommand setPlaybackCommand = new SetPlaybackCommand();
    private static final PlayCommand playCommand = new PlayCommand();
    private static final PauseCommand pauseCommand = new PauseCommand();
    private static final TogglePlaybackCommand togglePlaybackCommand = new TogglePlaybackCommand();
    private static final SeekFrameCommand seekFrameCommand = new SeekFrameCommand();
    private static final SeekTimeCommand seekTimeCommand = new SeekTimeCommand();
    private static final NextFrameCommand nextFrameCommand = new NextFrameCommand();
    private static final PreviousFrameCommand previousFrameCommand = new PreviousFrameCommand();
    private static final SetRecordingCommand setRecordingCommand = new SetRecordingCommand();
    private static final RecordStartCommand recordStartCommand = new RecordStartCommand();
    private static final RecordStopCommand recordStopCommand = new RecordStopCommand();
    private static final LoadStateCommand loadStateCommand = new LoadStateCommand();
    private static final LoadRequestCommand loadRequestCommand = new LoadRequestCommand();
    private static final LoadSunJSONCommand loadSunJSONCommand = new LoadSunJSONCommand();
    private static final LoadImageCommand loadImageCommand = new LoadImageCommand();
    private static final LoadCDFCommand loadCDFCommand = new LoadCDFCommand();
    private static final LoadVOTableCommand loadVOTableCommand = new LoadVOTableCommand();
    private static final LoadHapiCommand loadHapiCommand = new LoadHapiCommand();

    public static final class Registry {
        private static final LinkedHashMap<String, Command<?>> commands = new LinkedHashMap<>();

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
        }

        private Registry() {
        }

        private static void register(Command<?> command) {
            commands.put(command.id(), command);
        }

        public static Collection<String> ids() {
            return commands.keySet();
        }

        @Nullable
        public static Command<?> get(String id) {
            return commands.get(id);
        }

        @SuppressWarnings("unchecked")
        public static <I> void run(String id, @Nullable I input) throws Exception {
            Command<I> command = (Command<I>) commands.get(id);
            if (command == null)
                throw new IllegalArgumentException("Unknown command: " + id);
            command.run(input);
        }
    }

    public static void setViewState(@Nullable SetViewStateArgs args) {
        setViewStateCommand.run(args);
    }

    public static void setPlayback(@Nullable SetPlaybackArgs args) {
        setPlaybackCommand.run(args);
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

    public static void seekFrame(SeekFrameArgs args) {
        seekFrameCommand.run(args);
    }

    public static void seekTime(SeekTimeArgs args) {
        seekTimeCommand.run(args);
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

    public static void recordStop() {
        recordStopCommand.run(null);
    }

    public static void loadState(URI uri) {
        loadStateCommand.run(new LoadURIOrJSONArgs(uri, null));
    }

    public static void loadState(String json) {
        loadStateCommand.run(new LoadURIOrJSONArgs(null, json));
    }

    public static void loadRequest(URI uri) {
        loadRequestCommand.run(new LoadURIOrJSONArgs(uri, null));
    }

    public static void loadRequest(String json) {
        loadRequestCommand.run(new LoadURIOrJSONArgs(null, json));
    }

    public static void loadSunJSON(URI uri) {
        loadSunJSONCommand.run(new LoadURIOrJSONArgs(uri, null));
    }

    public static void loadSunJSON(String json) {
        loadSunJSONCommand.run(new LoadURIOrJSONArgs(null, json));
    }

    public static void loadImage(URI uri) {
        loadImageCommand.run(new LoadURIsArgs(List.of(uri)));
    }

    public static void loadImage(List<URI> uris) {
        loadImageCommand.run(new LoadURIsArgs(uris));
    }

    public static void loadCDF(URI uri) {
        loadCDFCommand.run(new LoadURIsArgs(List.of(uri)));
    }

    public static void loadCDF(List<URI> uris) {
        loadCDFCommand.run(new LoadURIsArgs(uris));
    }

    public static void loadVOTable(URI uri) {
        loadVOTableCommand.run(uri);
    }

    public static void loadHapi(URI uri) {
        loadHapiCommand.run(new LoadURIsArgs(List.of(uri)));
    }

    public static void loadHapi(List<URI> uris) {
        loadHapiCommand.run(new LoadURIsArgs(uris));
    }

    private static void loadURIOrJSON(String commandId, @Nullable URI uri, @Nullable String json, URILoader uriLoader, JSONLoader jsonLoader) {
        if (uri == null && json == null)
            return;
        if (uri != null && json != null)
            throw new IllegalArgumentException(commandId + " accepts either uri or json");

        if (uri != null)
            uriLoader.load(uri);
        else
            jsonLoader.load(json);
    }

    private static boolean hasURIs(@Nullable LoadURIsArgs input) {
        return input != null && input.uris() != null && !input.uris().isEmpty();
    }

    private static final class SetViewStateCommand implements Command<SetViewStateArgs> {
        @Override
        public String id() {
            return SET_VIEW_STATE;
        }

        @Override
        public void run(@Nullable SetViewStateArgs input) {
            if (input == null)
                return;

            ViewerState.ModeData current = ViewerState.modeData();
            ViewerState.applyMode(new ViewerState.ModeData(
                    input.projection() == null ? current.projection() : input.projection(),
                    input.annotationMode() == null ? current.annotationMode() : input.annotationMode(),
                    input.multiview() == null ? current.multiview() : input.multiview(),
                    input.tracking() == null ? current.tracking() : input.tracking(),
                    input.refresh() == null ? current.refresh() : input.refresh(),
                    input.showCorona() == null ? current.showCorona() : input.showCorona(),
                    input.differentialRotation() == null ? current.differentialRotation() : input.differentialRotation()));
        }
    }

    private static final class SetPlaybackCommand implements Command<SetPlaybackArgs> {
        @Override
        public String id() {
            return SET_PLAYBACK;
        }

        @Override
        public void run(@Nullable SetPlaybackArgs input) {
            if (input == null)
                return;

            if (input.advanceMode() != null)
                ViewerState.setPlaybackAdvanceMode(input.advanceMode());

            if (input.speed() != null || input.speedUnit() != null) {
                ViewerState.PlaybackData current = ViewerState.playbackData();
                int speed = input.speed() == null ? current.speed() : input.speed();
                ViewerState.PlaybackSpeedUnit speedUnit = input.speedUnit() == null ? current.speedUnit() : input.speedUnit();
                ViewerState.setPlaybackSpeed(speed, speedUnit);
            }
        }
    }

    private static final class PlayCommand implements Command<Void> {
        @Override
        public String id() {
            return PLAY;
        }

        @Override
        public void run(@Nullable Void input) {
            Movie.play();
        }
    }

    private static final class PauseCommand implements Command<Void> {
        @Override
        public String id() {
            return PAUSE;
        }

        @Override
        public void run(@Nullable Void input) {
            Movie.pause();
        }
    }

    private static final class TogglePlaybackCommand implements Command<Void> {
        @Override
        public String id() {
            return TOGGLE_PLAYBACK;
        }

        @Override
        public void run(@Nullable Void input) {
            Movie.toggle();
        }
    }

    private static final class SeekFrameCommand implements Command<SeekFrameArgs> {
        @Override
        public String id() {
            return SEEK_FRAME;
        }

        @Override
        public void run(@Nullable SeekFrameArgs input) {
            if (input == null)
                return;
            Movie.setFrame(input.frame());
        }
    }

    private static final class SeekTimeCommand implements Command<SeekTimeArgs> {
        @Override
        public String id() {
            return SEEK_TIME;
        }

        @Override
        public void run(@Nullable SeekTimeArgs input) {
            if (input == null || input.time() == null)
                return;
            Movie.setTime(input.time());
        }
    }

    private static final class NextFrameCommand implements Command<Void> {
        @Override
        public String id() {
            return NEXT_FRAME;
        }

        @Override
        public void run(@Nullable Void input) {
            Movie.nextFrame();
        }
    }

    private static final class PreviousFrameCommand implements Command<Void> {
        @Override
        public String id() {
            return PREVIOUS_FRAME;
        }

        @Override
        public void run(@Nullable Void input) {
            Movie.previousFrame();
        }
    }

    private static final class SetRecordingCommand implements Command<SetRecordingArgs> {
        @Override
        public String id() {
            return SET_RECORDING;
        }

        @Override
        public void run(@Nullable SetRecordingArgs input) {
            if (input == null)
                return;

            if (input.mode() != null)
                ViewerState.setRecordingMode(input.mode());
            if (input.size() != null)
                ViewerState.setRecordingSize(input.size());
        }
    }

    private static final class RecordStartCommand implements Command<RecordStartArgs> {
        @Override
        public String id() {
            return RECORD_START;
        }

        @Override
        public void run(@Nullable RecordStartArgs input) {
            if (Movie.isRecording())
                return;

            if (input != null) {
                setRecordingCommand.run(new SetRecordingArgs(input.mode(), input.size()));
                setPlaybackCommand.run(new SetPlaybackArgs(input.advanceMode(), input.speed(), input.speedUnit()));
            }

            ViewerState.RecordingData recordingData = ViewerState.recordingData();
            ViewerState.PlaybackData playbackData = ViewerState.playbackData();
            Dimension size = recordingData.size().getSize();
            int fps = playbackData.speedUnit().isRelative() ? playbackData.speed() : Movie.FPS_ABSOLUTE;
            ExportMovie.start(size.width, size.height, recordingData.size().isInternal(), fps, recordingData.mode());
        }
    }

    private static final class RecordStopCommand implements Command<Void> {
        @Override
        public String id() {
            return RECORD_STOP;
        }

        @Override
        public void run(@Nullable Void input) {
            if (!Movie.isRecording())
                return;
            ExportMovie.shallStop();
        }
    }

    private static final class LoadStateCommand implements Command<LoadURIOrJSONArgs> {
        @Override
        public String id() {
            return LOAD_STATE;
        }

        @Override
        public void run(@Nullable LoadURIOrJSONArgs input) {
            if (input == null)
                return;
            loadURIOrJSON(id(), input.uri(), input.json(), Load::state, Load::state);
        }
    }

    private static final class LoadRequestCommand implements Command<LoadURIOrJSONArgs> {
        @Override
        public String id() {
            return LOAD_REQUEST;
        }

        @Override
        public void run(@Nullable LoadURIOrJSONArgs input) {
            if (input == null)
                return;
            loadURIOrJSON(id(), input.uri(), input.json(), Load::request, Load::request);
        }
    }

    private static final class LoadSunJSONCommand implements Command<LoadURIOrJSONArgs> {
        @Override
        public String id() {
            return LOAD_SUN_JSON;
        }

        @Override
        public void run(@Nullable LoadURIOrJSONArgs input) {
            if (input == null)
                return;
            loadURIOrJSON(id(), input.uri(), input.json(), uri -> Load.getAllSunJSON(List.of(uri)), Load::sunJSON);
        }
    }

    private static final class LoadImageCommand implements Command<LoadURIsArgs> {
        @Override
        public String id() {
            return LOAD_IMAGE;
        }

        @Override
        public void run(@Nullable LoadURIsArgs input) {
            if (!hasURIs(input))
                return;
            Load.getAllImage(input.uris());
        }
    }

    private static final class LoadCDFCommand implements Command<LoadURIsArgs> {
        @Override
        public String id() {
            return LOAD_CDF;
        }

        @Override
        public void run(@Nullable LoadURIsArgs input) {
            if (!hasURIs(input))
                return;
            Load.getAllCDF(input.uris());
        }
    }

    private static final class LoadVOTableCommand implements Command<URI> {
        @Override
        public String id() {
            return LOAD_VOTABLE;
        }

        @Override
        public void run(@Nullable URI input) {
            if (input == null)
                return;
            SoarClient.submitTable(input);
        }
    }

    private static final class LoadHapiCommand implements Command<LoadURIsArgs> {
        @Override
        public String id() {
            return LOAD_HAPI;
        }

        @Override
        public void run(@Nullable LoadURIsArgs input) {
            if (!hasURIs(input))
                return;
            input.uris().forEach(BandReaderHapi::loadUri);
        }
    }
}
