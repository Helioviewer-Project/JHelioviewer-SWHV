package org.helioviewer.jhv.ui;

import java.util.Arrays;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.layers.Frames;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.movie.ExportFormat;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.thread.UiTimer;
import org.helioviewer.jhv.time.JHVTime;

// The transport of the active layer: the time slider with its cache completion, stepping, play, program time and the playback configuration.
// The one definition and interaction owner of this screen for every frontend. The creator owns it until dispose().
public final class MoviePanel implements Screen, Layers.Listener {

    private final Panel buffer = new Panel();

    private static final int SLIDER = 1;
    private static final int PREVIOUS = 4;
    private static final int PLAY = 5;
    private static final int NEXT = 6;
    private static final int SPEED = 7;
    private static final int UNIT = 8;
    private static final int MODE = 9;
    private static final int RECORD = 10;
    private static final int RECORD_MODE = 11;
    private static final int RECORD_SIZE = 12;
    private static final int OPTIONS = 15;
    private static final int PROGRAM_TIME = 16;
    private static final int FORMAT = 17;

    private static final String ICON_PAUSE = "";
    private static final String ICON_PLAY = "";
    private static final String ICON_RECORD = "\uf44a";
    private static final String ICON_SKIP_NEXT = "\uf4ad";
    private static final String ICON_SKIP_PREVIOUS = "\uf4ae";
    private static final String ICON_TUNE = "\uf62e";

    private static final String[] SPEED_UNITS = Panel.labels(ViewState.PlaybackSpeedUnit.values());
    // Without the internal SwingDown.
    private static final String[] ADVANCE_MODES = {"Loop", "Stop", "Swing"};
    private static final String[] RECORDING_MODES = Panel.labels(ViewState.RecordingMode.values());
    private static final String[] RECORDING_SIZES = Panel.labels(ViewState.RecordingSize.values());
    private static final String[] FORMATS = Panel.labels(ExportFormat.values());
    private static final String TIME_SAMPLE = "2000-01-01T12:00:00.000";
    private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 26;
    private static final int PLAY_SIZE = 32;
    private static final int SLIDER_HEIGHT = 20;
    private static final int POSITION_GAP = 4;

    private static final String SLIDER_TIP = Platform.isMacOS() ? "Drag: scrub • ⌥-drag: trim • ⌘-drag: move" : "Drag: scrub • Alt-drag: trim • Ctrl-drag: move";

    private boolean advanced;
    private long displayedTime = Long.MIN_VALUE;
    private final UiTimer timePoller = new UiTimer(100, this::pollTime);
    private boolean scrubResume;
    private byte[] completion = new byte[0];
    private final Commands.Completion recordingFinished;
    private Runnable listener = () -> {};

    public MoviePanel(Commands.Completion _recordingFinished) {
        recordingFinished = _recordingFinished;
        Layers.addListener(this);
        Player.addFrameListener(frame -> changed());
        Player.addPlaybackRangeListener(this::changed);
        Player.addStatusListener(this::changed);
        ViewState.addPlaybackConfigListener(this::changed);
        ViewState.addRecordingConfigListener(this::changed);
        ExportMovie.addStatusListener(this::changed);
        timePoller.start();
    }

    // Player, ViewState and ExportMovie cannot remove a listener; what remains of this panel there reports to nobody.
    public void dispose() {
        timePoller.stop();
        Layers.removeListener(this);
        listener = () -> {};
    }

    @Override
    public void listen(Runnable _listener) {
        listener = _listener;
    }

    private void changed() {
        listener.run();
    }

    public void setAdvanced(boolean _advanced) {
        advanced = _advanced;
        changed();
    }

    private void pollTime() {
        if (GLRenderer.getDisplayedViewpoint().time.milli != displayedTime)
            changed();
    }

    @Override
    public Panel build() {
        Frames frames = Layers.getActiveImageLayer().frames();
        int n = frames.size();
        if (completion.length != n)
            completion = new byte[n];
        if (frames.isComplete()) {
            Arrays.fill(completion, (byte) 2);
        } else {
            for (int i = 0; i < n; i++) {
                Boolean status = frames.completion(i);
                completion[i] = status == null ? 0 : status ? (byte) 2 : (byte) 1;
            }
        }
        boolean playing = Player.isPlaying();
        boolean recording = ExportMovie.isRecording();
        ViewState.PlaybackData d = ViewState.playbackData();
        ViewState.RecordingData r = ViewState.recordingData();
        JHVTime time = GLRenderer.getDisplayedViewpoint().time;
        displayedTime = time.milli;
        int reserved = Math.max(10000, n);
        Panel p = buffer.reset()
                .timeSlider(SLIDER, frames.current(), Player.getPlaybackFirstFrame(), Player.getPlaybackLastFrame(), n - 1, completion).sizePoints(0, SLIDER_HEIGHT).tip(SLIDER_TIP)
                .row().tight()
                .button(PREVIOUS, "").icon(ICON_SKIP_PREVIOUS, 18).sizePoints(BUTTON_WIDTH, BUTTON_HEIGHT).flat().repeat(125).tip("Step to previous frame")
                .button(PLAY, "").icon(playing ? ICON_PAUSE : ICON_PLAY, 18).sizePoints(PLAY_SIZE, PLAY_SIZE).flat().tip(playing ? "Pause movie" : "Play movie")
                .button(NEXT, "").icon(ICON_SKIP_NEXT, 18).sizePoints(BUTTON_WIDTH, BUTTON_HEIGHT).flat().repeat(125).tip("Step to next frame")
                .toggleButton(RECORD, "", recording).icon(ICON_RECORD, 18).sizePoints(BUTTON_WIDTH, BUTTON_HEIGHT).flat().foreground(recording ? 0xff0000 : 0x800000).tip("Record movie")
                .toggleButton(OPTIONS, "", advanced).icon(ICON_TUNE, 18).sizePoints(BUTTON_WIDTH, BUTTON_HEIGHT).flat().tip("Playback and recording options")
                .row().right().label((frames.current() + 1) + "/" + n).mono().small().right().widthText(reserved + "/" + reserved)
                .space(0, 0).sizePoints(POSITION_GAP, 0)
                .text(PROGRAM_TIME, time.toString(), 0).mono().small().right().readOnly(true).flat().widthText(TIME_SAMPLE).tip("Program time").rowEnd()
                .rowEnd();
        if (advanced) {
            // The whole playback and recording configuration is disabled while recording.
            boolean screenshot = r.mode() == ViewState.RecordingMode.SHOT;
            p.column().enabled(!recording)
                    .row().right()
                    .label("Play")
                    .numberSpinner(SPEED, d.speed(), ViewState.PLAYBACK_SPEED_MIN, ViewState.PLAYBACK_SPEED_MAX, 1).format(0, 1, "").swingDefaultNumberEditor().tip("Maximum " + ViewState.PLAYBACK_SPEED_MAX + " fps")
                    .combo(UNIT, SPEED_UNITS, d.speedUnit().ordinal())
                    .label(" and")
                    .combo(MODE, ADVANCE_MODES, d.advanceMode().ordinal())
                    .rowEnd()
                    .row().right()
                    .label("Record").radio(RECORD_MODE, RECORDING_MODES, r.mode().ordinal())
                    .rowEnd()
                    .row().right()
                    .label("Output size").combo(RECORD_SIZE, RECORDING_SIZES, r.size().ordinal())
                    .label("Format").combo(FORMAT, FORMATS, ExportFormat.fromSettings().ordinal()).enabled(!screenshot).tip(screenshot ? "Screenshots are saved as PNG" : null)
                    .rowEnd()
                    .columnEnd();
        }
        return p;
    }

    @Override
    public void choice(int owner, int item, int value) {
        switch (item) {
            case SLIDER -> Commands.seekFrame(value);
            case UNIT -> ViewState.setPlaybackSpeed(ViewState.playbackData().speed(), Panel.choice(ViewState.PlaybackSpeedUnit.values(), value));
            case MODE -> ViewState.setPlaybackAdvanceMode(Panel.choice(Player.AdvanceMode.values(), value));
            case RECORD_MODE -> ViewState.setRecordingMode(Panel.choice(ViewState.RecordingMode.values(), value));
            case RECORD_SIZE -> ViewState.setRecordingSize(Panel.choice(ViewState.RecordingSize.values(), value));
            case FORMAT -> Settings.setProperty("video.format", Panel.choice(ExportFormat.values(), value).name());
            default -> throw new IllegalArgumentException("MoviePanel choice " + item);
        }
    }

    @Override
    public void number(int owner, int item, double value) {
        switch (item) {
            case SPEED -> ViewState.setPlaybackSpeed((int) value, ViewState.playbackData().speedUnit());
            default -> throw new IllegalArgumentException("MoviePanel number " + item);
        }
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (item == SLIDER) {
            switch (command) {
                case SCRUB_START -> {
                    scrubResume = Player.isPlaying();
                    if (scrubResume)
                        Commands.pause();
                }
                case SCRUB_END -> {
                    if (scrubResume)
                        Commands.play();
                    scrubResume = false;
                }
                case PLAY_PAUSE -> Commands.togglePlayback();
                case STEP_BACKWARD -> Commands.stepBackward();
                case STEP_FORWARD -> Commands.stepForward();
                case INCREASE -> Commands.nextFrame();
                case DECREASE -> Commands.previousFrame();
                default -> throw new IllegalArgumentException("Time slider command " + command);
            }
            return;
        }
        switch (item) {
            case OPTIONS -> {
                advanced = !advanced;
                changed();
            }
            case PREVIOUS -> Commands.stepBackward();
            case PLAY -> Commands.togglePlayback();
            case NEXT -> Commands.stepForward();
            case RECORD -> Commands.toggleRecording(recordingFinished);
            default -> throw new IllegalArgumentException("Movie item " + item);
        }
    }

    @Override
    public void setRange(int owner, int item, double low, double high) {
        if (item != SLIDER)
            throw new IllegalArgumentException("Movie range item " + item);
        Commands.setPlaybackRange((int) low, (int) high);
    }

    @Override
    public void layerUpdated(Layer layer) {
        changed();
    }

    @Override
    public void layersCleared() {
        changed();
    }

    @Override
    public void completionUpdated(ImageLayer layer) {
        if (layer == Layers.getActiveImageLayer())
            changed();
    }

}
