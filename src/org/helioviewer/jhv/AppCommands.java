package org.helioviewer.jhv;

import java.awt.Dimension;
import java.net.URI;
import java.util.List;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.camera.ViewActions;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ViewerState;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.io.SoarClient;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.math.Quat;
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

        void run(@Nullable I input) throws Exception;
    }

    private interface IntCommand extends RegisteredCommand {
        void run(int input) throws Exception;
    }

    private interface TimeCommand extends RegisteredCommand {
        void run(@Nullable JHVTime input) throws Exception;
    }

    private interface QuatCommand extends RegisteredCommand {
        void run(@Nullable Quat input) throws Exception;
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
            @Nullable ViewerState.PlaybackSpeedUnit speedUnit,
            @Nullable Integer firstFrame,
            @Nullable Integer lastFrame) {
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
    private static final ZoomInCommand zoomInCommand = new ZoomInCommand();
    private static final ZoomOutCommand zoomOutCommand = new ZoomOutCommand();
    private static final ZoomFitCommand zoomFitCommand = new ZoomFitCommand();
    private static final ZoomOneToOneCommand zoomOneToOneCommand = new ZoomOneToOneCommand();
    private static final ResetViewCommand resetViewCommand = new ResetViewCommand();
    private static final ResetViewAxisCommand resetViewAxisCommand = new ResetViewAxisCommand();
    private static final RotateView90Command rotateView90Command = new RotateView90Command();

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

        public static Collection<String> ids() {
            return commands.keySet();
        }

        @Nullable
        public static RegisteredCommand get(String id) {
            return commands.get(id);
        }

        @SuppressWarnings("unchecked")
        public static <I> void run(String id, @Nullable I input) throws Exception {
            RegisteredCommand command = commands.get(id);
            if (command == null)
                throw new IllegalArgumentException("Unknown command: " + id);
            if (!(command instanceof Command<?>))
                throw new IllegalArgumentException("Command does not accept object input: " + id);
            ((Command<I>) command).run(input);
        }

        public static void run(String id, int input) throws Exception {
            RegisteredCommand command = commands.get(id);
            if (command == null)
                throw new IllegalArgumentException("Unknown command: " + id);
            if (!(command instanceof IntCommand intCommand))
                throw new IllegalArgumentException("Command does not accept int input: " + id);
            intCommand.run(input);
        }

        public static void run(String id, @Nullable JHVTime input) throws Exception {
            RegisteredCommand command = commands.get(id);
            if (command == null)
                throw new IllegalArgumentException("Unknown command: " + id);
            if (!(command instanceof TimeCommand timeCommand))
                throw new IllegalArgumentException("Command does not accept time input: " + id);
            timeCommand.run(input);
        }

        public static void run(String id, @Nullable Quat input) throws Exception {
            RegisteredCommand command = commands.get(id);
            if (command == null)
                throw new IllegalArgumentException("Unknown command: " + id);
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
        setPlaybackCommand.setRange(firstFrame, lastFrame);
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

    public static void recordStop() {
        recordStopCommand.run(null);
    }

    public static void loadState(URI uri) {
        loadStateCommand.run(uri);
    }

    public static void loadState(String json) {
        loadStateCommand.run(json);
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

    private static void loadURIOrJSON(String commandId, @Nullable Object input, URILoader uriLoader, JSONLoader jsonLoader) {
        switch (input) {
            case null -> {
                return;
            }
            case URI uri -> {
                uriLoader.load(uri);
                return;
            }
            case String json -> {
                jsonLoader.load(json);
                return;
            }
            default -> {
            }
        }
        throw new IllegalArgumentException(commandId + " accepts URI or String");
    }

    @SuppressWarnings("unchecked")
    private static List<URI> asURIList(String commandId, Object input) {
        if (!(input instanceof List<?> uris))
            throw new IllegalArgumentException(commandId + " accepts URI or List<URI>");
        for (Object uri : uris) {
            if (!(uri instanceof URI))
                throw new IllegalArgumentException(commandId + " accepts URI or List<URI>");
        }
        return (List<URI>) uris;
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

            if (input.firstFrame() != null || input.lastFrame() != null) {
                ViewerState.PlaybackData current = ViewerState.playbackData();
                int firstFrame = input.firstFrame() == null ? current.firstFrame() : input.firstFrame();
                int lastFrame = input.lastFrame() == null ? current.lastFrame() : input.lastFrame();
                setRange(firstFrame, lastFrame);
            }
        }

        private void setRange(int firstFrame, int lastFrame) {
            ViewerState.setPlaybackRange(firstFrame, lastFrame);
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

    private static final class SeekFrameCommand implements IntCommand {
        @Override
        public String id() {
            return SEEK_FRAME;
        }

        @Override
        public void run(int input) {
            Movie.setFrame(input);
        }
    }

    private static final class SeekTimeCommand implements TimeCommand {
        @Override
        public String id() {
            return SEEK_TIME;
        }

        @Override
        public void run(@Nullable JHVTime input) {
            if (input == null)
                return;
            Movie.setTime(input);
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
                if (input.mode() != null)
                    ViewerState.setRecordingMode(input.mode());
                if (input.size() != null)
                    ViewerState.setRecordingSize(input.size());
                if (input.advanceMode() != null)
                    ViewerState.setPlaybackAdvanceMode(input.advanceMode());
                if (input.speed() != null || input.speedUnit() != null) {
                    ViewerState.PlaybackData current = ViewerState.playbackData();
                    int speed = input.speed() == null ? current.speed() : input.speed();
                    ViewerState.PlaybackSpeedUnit speedUnit = input.speedUnit() == null ? current.speedUnit() : input.speedUnit();
                    ViewerState.setPlaybackSpeed(speed, speedUnit);
                }
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

    private static final class LoadStateCommand implements Command<Object> {
        @Override
        public String id() {
            return LOAD_STATE;
        }

        @Override
        public void run(@Nullable Object input) {
            loadURIOrJSON(id(), input, Load::state, Load::state);
        }
    }

    private static final class LoadRequestCommand implements Command<Object> {
        @Override
        public String id() {
            return LOAD_REQUEST;
        }

        @Override
        public void run(@Nullable Object input) {
            loadURIOrJSON(id(), input, Load::request, Load::request);
        }
    }

    private static final class LoadSunJSONCommand implements Command<Object> {
        @Override
        public String id() {
            return LOAD_SUN_JSON;
        }

        @Override
        public void run(@Nullable Object input) {
            loadURIOrJSON(id(), input, Load::getAllSunJSON, Load::sunJSON);
        }
    }

    private static final class LoadImageCommand implements Command<Object> {
        @Override
        public String id() {
            return LOAD_IMAGE;
        }

        @Override
        public void run(@Nullable Object input) {
            switch (input) {
                case null -> {
                    return;
                }
                case URI uri -> {
                    Load.getAllImage(uri);
                    return;
                }
                case List<?> uris when !uris.isEmpty() -> {
                    Load.getAllImage(asURIList(id(), uris));
                    return;
                }
                default -> throw new IllegalArgumentException(id() + " accepts URI or List<URI>");
            }
        }
    }

    private static final class LoadCDFCommand implements Command<Object> {
        @Override
        public String id() {
            return LOAD_CDF;
        }

        @Override
        public void run(@Nullable Object input) {
            switch (input) {
                case null -> {
                    return;
                }
                case URI uri -> {
                    Load.getAllCDF(uri);
                    return;
                }
                case List<?> uris when !uris.isEmpty() -> {
                    Load.getAllCDF(asURIList(id(), uris));
                    return;
                }
                default -> throw new IllegalArgumentException(id() + " accepts URI or List<URI>");
            }
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

    private static final class LoadHapiCommand implements Command<Object> {
        @Override
        public String id() {
            return LOAD_HAPI;
        }

        @Override
        public void run(@Nullable Object input) {
            switch (input) {
                case null -> {
                    return;
                }
                case URI uri -> {
                    BandReaderHapi.loadUri(uri);
                    return;
                }
                case List<?> uris when !uris.isEmpty() -> {
                    asURIList(id(), uris).forEach(BandReaderHapi::loadUri);
                    return;
                }
                default -> throw new IllegalArgumentException(id() + " accepts URI or List<URI>");
            }
        }
    }

    private static final class ZoomInCommand implements Command<Void> {
        @Override
        public String id() {
            return ZOOM_IN;
        }

        @Override
        public void run(@Nullable Void input) {
            ViewActions.zoomIn();
        }
    }

    private static final class ZoomOutCommand implements Command<Void> {
        @Override
        public String id() {
            return ZOOM_OUT;
        }

        @Override
        public void run(@Nullable Void input) {
            ViewActions.zoomOut();
        }
    }

    private static final class ZoomFitCommand implements Command<Void> {
        @Override
        public String id() {
            return ZOOM_FIT;
        }

        @Override
        public void run(@Nullable Void input) {
            ViewActions.zoomFit();
        }
    }

    private static final class ZoomOneToOneCommand implements Command<Void> {
        @Override
        public String id() {
            return ZOOM_ONE_TO_ONE;
        }

        @Override
        public void run(@Nullable Void input) {
            ViewActions.zoomOneToOne();
        }
    }

    private static final class ResetViewCommand implements Command<Void> {
        @Override
        public String id() {
            return RESET_VIEW;
        }

        @Override
        public void run(@Nullable Void input) {
            ViewActions.resetView();
        }
    }

    private static final class ResetViewAxisCommand implements Command<Void> {
        @Override
        public String id() {
            return RESET_VIEW_AXIS;
        }

        @Override
        public void run(@Nullable Void input) {
            ViewActions.resetViewAxis();
        }
    }

    private static final class RotateView90Command implements QuatCommand {
        @Override
        public String id() {
            return ROTATE_VIEW_90;
        }

        @Override
        public void run(@Nullable Quat input) {
            ViewActions.rotateView90(input);
        }
    }
}
