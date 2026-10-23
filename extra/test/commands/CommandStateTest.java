package org.helioviewer.jhv.io.samp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.movie.ExportFormat;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.UiThread;

import org.astrogrid.samp.Message;
import org.json.JSONObject;

public final class CommandStateTest {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.io.tmpdir", System.getProperty("user.home"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        List<String> warnings = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue())
                    warnings.add(record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger.getLogger("").addHandler(handler);
        try {
            UiThread.postAndWait(() -> {
                DisplayController.setRenderRequestHandler(_ -> {});
                checkMode(warnings);
                checkPlayback(warnings);
                checkRecording(warnings);
                checkExportFormats();
            });
        } finally {
            UiThread.postAndWait(() -> {
                ExportMovie.shutdown();
                Player.pause();
                ViewState.setRefresh(false);
            });
            Logger.getLogger("").removeHandler(handler);
        }
        System.out.println("CommandStateTest passed");
    }

    private static void checkExportFormats() {
        String original = Settings.getProperty("video.format");
        try {
            for (ExportFormat format : ExportFormat.values()) {
                Settings.setProperty("video.format", format.name());
                check(ExportFormat.fromSettings() == format, "Configured recording format changed");
            }
            Settings.setProperty("video.format", "invalid");
            check(ExportFormat.fromSettings() == ExportFormat.H264, "Invalid recording format did not default to H.264");
        } finally {
            Settings.setProperty("video.format", original);
        }
    }

    private static void checkMode(List<String> warnings) {
        ViewState.ModeData before = ViewState.modeData();
        check(ViewState.readModeJson(new JSONObject()).equals(before), "Empty state changed mode");
        JSONObject state = new JSONObject().put("projection", "HPC").put("warpLambda", 2).put("tracking", !before.tracking());
        ViewState.ModeData parsed = ViewState.readModeJson(state);
        check(parsed.projection() == MapMode.HPC && parsed.warpLambda() == 1 && parsed.tracking() != before.tracking(), "State mode parsing");
        state.put("projection", "invalid").put("tracking", "false");
        parsed = ViewState.readModeJson(state);
        check(parsed.projection() == before.projection() && parsed.tracking() == before.tracking(), "Invalid state values changed mode");
        checkWarning(warnings, "projection");
        checkWarning(warnings, "tracking");

        Message message = new Message("jhv.view.set").addParam("projection", "HPC").addParam("multiview", "TrUe")
                .addParam("tracking", "FALSE").addParam("warpLambda", "-1");
        JSONObject parameters = SampHandlers.parameters(message, "projection", "multiview", "tracking");
        message.addParam("projection", "Orthographic");
        Commands.setViewState(parameters);
        check(ViewState.getProjection() == MapMode.HPC && ViewState.isMultiview() && !ViewState.isTracking(), "SAMP booleans or parameter snapshot");
        check(ViewState.getWarpLambda() == before.warpLambda(), "Unsupported SAMP parameter changed warp");
        Commands.setViewState(new JSONObject().put("projection", "invalid").put("tracking", "invalid").put("showCorona", !before.showCorona()));
        check(ViewState.getProjection() == MapMode.HPC && !ViewState.isTracking() && ViewState.isShowCorona() != before.showCorona(), "Partial mode update");
        checkWarning(warnings, "projection");
        checkWarning(warnings, "tracking");
        ViewState.applyMode(before);
        System.out.println("PASS: shared mode parsing, strict state booleans, SAMP booleans, warnings and partial updates");
    }

    private static void checkPlayback(List<String> warnings) {
        Commands.setPlayback(new JSONObject().put("advanceMode", "Swing").put("speed", "35").put("firstFrame", "2").put("lastFrame", "7"));
        ViewState.PlaybackData before = ViewState.playbackData();
        check(before.advanceMode() == Player.AdvanceMode.Swing && before.speed() == 35 && before.firstFrame() == 2 && before.lastFrame() == 7, "Playback settings");
        Commands.setPlayback(new JSONObject());
        check(ViewState.playbackData().equals(before), "Empty playback update");
        for (String invalid : List.of("1.5", "2147483648", " 12 ", "invalid")) {
            Commands.setPlayback(new JSONObject().put("speed", invalid).put("firstFrame", invalid).put("lastFrame", invalid));
            check(ViewState.playbackData().equals(before), "Invalid integer accepted: " + invalid);
            checkWarning(warnings, "playback speed");
            checkWarning(warnings, "playback first frame");
            checkWarning(warnings, "playback last frame");
        }
        Commands.setPlayback(new JSONObject().put("advanceMode", "SwingDown").put("speedUnit", "invalid"));
        check(ViewState.playbackData().equals(before), "Internal or invalid playback enum accepted");
        checkWarning(warnings, "internal playback advance mode");
        checkWarning(warnings, "speedUnit");
        Commands.setPlayback(new JSONObject().put("speedUnit", "HOURS_PER_SECOND").put("firstFrame", "3"));
        check(ViewState.playbackData().speedUnit() == ViewState.PlaybackSpeedUnit.HOURS_PER_SECOND
                && ViewState.playbackData().speed() == 35 && Player.getPlaybackFirstFrame() == 3 && Player.getPlaybackLastFrame() == 7, "Partial playback update");
        Commands.setPlayback(new JSONObject().put("speed", "0"));
        check(ViewState.playbackData().speed() == 1, "Speed minimum");
        Commands.setPlayback(new JSONObject().put("speed", "121"));
        check(ViewState.playbackData().speed() == 120, "Speed maximum");
        System.out.println("PASS: strict integers, internal mode rejection, speed clamping and partial playback updates");
    }

    private static void checkRecording(List<String> warnings) {
        Commands.setRecording(new JSONObject().put("mode", "FREE").put("size", "H1024"));
        Commands.setRecording(new JSONObject().put("mode", "invalid").put("size", "H2048"));
        check(ViewState.recordingData().mode() == ViewState.RecordingMode.FREE && ViewState.recordingData().size() == ViewState.RecordingSize.H2048, "Partial recording update");
        checkWarning(warnings, "mode");
        Commands.setRecording(new JSONObject().put("size", "invalid"));
        check(ViewState.recordingData().size() == ViewState.RecordingSize.H2048, "Invalid recording size accepted");
        checkWarning(warnings, "size");

        List<Boolean> outcomes = new ArrayList<>();
        Commands.OperationContext context = new Commands.OperationContext(null, null, null, (_, success, _, _) -> outcomes.add(success));
        ExportMovie.setMainCanvasVisible(false);
        Commands.recordStart(context, new JSONObject().put("mode", "FREE").put("size", "H1024").put("speed", "37"));
        check(!ExportMovie.isRecording() && outcomes.equals(List.of(false)), "Failed recording start did not report failure");
        check(ViewState.recordingData().size() == ViewState.RecordingSize.H1024 && ViewState.playbackData().speed() == 37, "Failed start discarded applied settings");
        ExportMovie.setMainCanvasVisible(true);

        Message message = new Message("jhv.record.start").addParam("mode", "FREE").addParam("size", "H2048")
                .addParam("advanceMode", "Stop").addParam("speed", "32").addParam("speedUnit", "FRAMES_PER_SECOND").addParam("firstFrame", "0");
        Commands.recordStart(context, SampHandlers.parameters(message, "mode", "size", "advanceMode", "speed", "speedUnit"));
        check(ExportMovie.isRecording() && outcomes.size() == 1, "Recording did not start or completed prematurely");
        ViewState.PlaybackData playback = ViewState.playbackData();
        ViewState.RecordingData recording = ViewState.recordingData();
        check(playback.speed() == 32 && playback.advanceMode() == Player.AdvanceMode.Stop && playback.firstFrame() == 3
                && recording.size() == ViewState.RecordingSize.H2048, "Recording parameters or unsupported frame parameter");
        int count = warnings.size();
        Commands.recordStart(context, new JSONObject().put("mode", "invalid").put("size", "H4096").put("speed", "120"));
        check(outcomes.equals(List.of(false, false)) && warnings.size() == count, "Already-recording request was parsed or not refused");
        check(ViewState.playbackData().equals(playback) && ViewState.recordingData().equals(recording), "Already-recording request changed settings");
        Commands.recordStart(context, null);
        check(outcomes.equals(List.of(false, false, false)) && ExportMovie.isRecording(), "GUI start bypassed the recording guard");
        System.out.println("PASS: recording partial updates, combined start settings, failure completion and already-recording guard");
    }

    private static void checkWarning(List<String> warnings, String text) {
        for (int i = 0; i < warnings.size(); i++) {
            if (warnings.get(i).contains(text)) {
                warnings.remove(i);
                return;
            }
        }
        throw new AssertionError("Missing warning: " + text);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
