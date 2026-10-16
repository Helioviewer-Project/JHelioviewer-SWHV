package org.helioviewer.jhv.app.state;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.annotation.AnnotationMode;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.movie.Player;

import org.json.JSONObject;

public final class ViewState {

    public interface ModeListener {
        void modeStateChanged();
    }

    public interface PlaybackConfigListener {
        void playbackConfigChanged();
    }

    public interface RecordingConfigListener {
        void recordingConfigChanged();
    }

    public enum PlaybackSpeedUnit {
        FRAMES_PER_SECOND("Frames/sec", 0),
        MINUTES_PER_SECOND("Solar minutes/sec", 60),
        HOURS_PER_SECOND("Solar hours/sec", 3600),
        DAYS_PER_SECOND("Solar days/sec", 86400);

        private final String label;
        private final int secPerSecond;

        PlaybackSpeedUnit(String _label, int _secPerSecond) {
            label = _label;
            secPerSecond = _secPerSecond;
        }

        public boolean isRelative() {
            return this == FRAMES_PER_SECOND;
        }

        public int secPerSecond() {
            return secPerSecond;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum RecordingMode {
        LOOP("One loop"),
        SHOT("Screenshot"),
        FREE("Unlimited");

        private final String label;

        RecordingMode(String _label) {
            label = _label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum RecordingSize {
        ORIGINAL("On screen", 0, 0),
        H1024("1024×1024", 1024, 1024),
        H1080("1920×1080", 1920, 1080),
        H2048("2048×2048", 2048, 2048),
        H2160("3840×2160", 3840, 2160),
        H4096("4096×4096", 4096, 4096);

        private final String label;
        private final int width;
        private final int height;

        RecordingSize(String _label, int _width, int _height) {
            label = _label;
            width = _width;
            height = _height;
        }

        public Size getSize() {
            if (this == ORIGINAL)
                return new Size(Display.fullViewport.width, Display.fullViewport.height, false);
            return new Size(width, height, true);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record ModeData(MapMode projection, double warpLambda, AnnotationMode annotationMode, boolean multiview,
                           boolean tracking, boolean refresh, boolean showCorona, boolean differentialRotation) {}

    public record PlaybackData(Player.AdvanceMode advanceMode, int speed, PlaybackSpeedUnit speedUnit,
                               int firstFrame, int lastFrame) {}

    public record RecordingData(RecordingMode mode, RecordingSize size) {}

    public record Size(int width, int height, boolean internal) {}

    private static final ArrayList<ModeListener> modeListeners = new ArrayList<>();
    private static final ArrayList<PlaybackConfigListener> playbackConfigListeners = new ArrayList<>();
    private static final ArrayList<RecordingConfigListener> recordingConfigListeners = new ArrayList<>();
    private static boolean suppressModeNotifications;

    private static AnnotationMode annotationMode = AnnotationMode.Cross;
    public static final int PLAYBACK_SPEED_MIN = 1;
    public static final int PLAYBACK_SPEED_MAX = 120;
    private static Player.AdvanceMode playbackAdvanceMode = Player.AdvanceMode.Loop;
    private static int playbackSpeed = Player.FPS_RELATIVE_DEFAULT;
    private static PlaybackSpeedUnit playbackSpeedUnit = PlaybackSpeedUnit.FRAMES_PER_SECOND;
    private static RecordingMode recordingMode = RecordingMode.LOOP;
    private static RecordingSize recordingSize = RecordingSize.ORIGINAL;

    public static ModeData modeData() {
        return new ModeData(getProjection(), getWarpLambda(), getAnnotationMode(), isMultiview(), isTracking(), isRefresh(), isShowCorona(), isDifferentialRotation());
    }

    public static PlaybackData playbackData() {
        return new PlaybackData(playbackAdvanceMode, playbackSpeed, playbackSpeedUnit, Player.getPlaybackFirstFrame(), Player.getPlaybackLastFrame());
    }

    public static RecordingData recordingData() {
        return new RecordingData(recordingMode, recordingSize);
    }

    public static void writeModeJson(JSONObject target) {
        ModeData data = modeData();
        target.put("multiview", data.multiview());
        target.put("projection", data.projection());
        target.put("warpLambda", data.warpLambda());
        target.put("annotationMode", data.annotationMode());
        target.put("tracking", data.tracking());
        target.put("refresh", data.refresh());
        target.put("showCorona", data.showCorona());
        target.put("differentialRotation", data.differentialRotation());
    }

    public static ModeData readModeJson(JSONObject source) {
        ModeData current = modeData();
        return new ModeData(
                readEnum(source, "projection", current.projection()),
                Math.clamp(source.optDouble("warpLambda", current.warpLambda()), -1, 1),
                readEnum(source, "annotationMode", current.annotationMode()),
                readBoolean(source, "multiview", current.multiview()),
                readBoolean(source, "tracking", current.tracking()),
                readBoolean(source, "refresh", current.refresh()),
                readBoolean(source, "showCorona", current.showCorona()),
                readBoolean(source, "differentialRotation", current.differentialRotation()));
    }

    public static void applyMode(ModeData data) {
        boolean changed = getProjection() != data.projection()
                || getWarpLambda() != data.warpLambda()
                || annotationMode != data.annotationMode()
                || isMultiview() != data.multiview()
                || isTracking() != data.tracking()
                || isRefresh() != data.refresh()
                || isShowCorona() != data.showCorona()
                || isDifferentialRotation() != data.differentialRotation();

        if (!changed)
            return;

        suppressModeNotifications = true;
        try {
            setProjection(data.projection());
            setWarpLambda(data.warpLambda());
            setAnnotationMode(data.annotationMode());
            setMultiview(data.multiview());
            setTracking(data.tracking());
            setRefresh(data.refresh());
            setShowCorona(data.showCorona());
            setDifferentialRotation(data.differentialRotation());
        } finally {
            suppressModeNotifications = false;
        }

        notifyModeListeners();
    }

    public static MapMode getProjection() {
        return Display.mode;
    }

    public static void setProjection(MapMode newProjection) {
        if (getProjection() == newProjection)
            return;

        Display.setMapMode(newProjection);
        notifyModeListeners();
    }

    public static double getWarpLambda() {
        return Display.getWarpLambda();
    }

    public static void setWarpLambda(double newWarpLambda) {
        newWarpLambda = Math.clamp(newWarpLambda, -1, 1);
        if (getWarpLambda() == newWarpLambda)
            return;

        Display.setWarpLambda(newWarpLambda);
        DisplayController.display();
        notifyModeListeners();
    }

    public static AnnotationMode getAnnotationMode() {
        return annotationMode;
    }

    public static void setAnnotationMode(AnnotationMode newAnnotationMode) {
        if (annotationMode == newAnnotationMode)
            return;

        annotationMode = newAnnotationMode;
        notifyModeListeners();
    }

    public static boolean isMultiview() {
        return Display.multiview;
    }

    public static void setMultiview(boolean newMultiview) {
        if (isMultiview() == newMultiview)
            return;

        Display.multiview = newMultiview;
        ImageLayers.arrangeMultiView();
        notifyModeListeners();
    }

    public static boolean isTracking() {
        return DisplayController.getTrackingMode();
    }

    public static void setTracking(boolean newTracking) {
        if (isTracking() == newTracking)
            return;

        DisplayController.setTrackingMode(newTracking);
        notifyModeListeners();
    }

    public static boolean isRefresh() {
        return ImageLayers.getRefreshMode();
    }

    public static void setRefresh(boolean newRefresh) {
        if (isRefresh() == newRefresh)
            return;

        ImageLayers.setRefreshMode(newRefresh);
        notifyModeListeners();
    }

    public static boolean isShowCorona() {
        return Display.getShowCorona();
    }

    public static void setShowCorona(boolean newShowCorona) {
        if (isShowCorona() == newShowCorona)
            return;

        Display.setShowCorona(newShowCorona);
        DisplayController.display();
        notifyModeListeners();
    }

    public static boolean isDifferentialRotation() {
        return ImageLayers.getDiffRotationMode();
    }

    public static void setDifferentialRotation(boolean newDifferentialRotation) {
        if (isDifferentialRotation() == newDifferentialRotation)
            return;

        ImageLayers.setDiffRotationMode(newDifferentialRotation);
        DisplayController.display();
        notifyModeListeners();
    }

    public static void setPlaybackAdvanceMode(Player.AdvanceMode newPlaybackAdvanceMode) {
        if (playbackAdvanceMode == newPlaybackAdvanceMode)
            return;

        playbackAdvanceMode = newPlaybackAdvanceMode;
        Player.setAdvanceMode(newPlaybackAdvanceMode);
        notifyPlaybackConfigListeners();
    }

    public static void setPlaybackSpeed(int newPlaybackSpeed, PlaybackSpeedUnit newPlaybackSpeedUnit) {
        int speed = Math.clamp(newPlaybackSpeed, PLAYBACK_SPEED_MIN, PLAYBACK_SPEED_MAX);
        if (speed != newPlaybackSpeed)
            Log.warn("Clamping invalid playback speed " + newPlaybackSpeed + " to " + speed);
        if (playbackSpeed == speed && playbackSpeedUnit == newPlaybackSpeedUnit)
            return;

        playbackSpeed = speed;
        playbackSpeedUnit = newPlaybackSpeedUnit;
        if (playbackSpeedUnit.isRelative())
            Player.setDesiredRelativeSpeed(playbackSpeed);
        else
            Player.setDesiredAbsoluteSpeed(playbackSpeed * playbackSpeedUnit.secPerSecond());
        notifyPlaybackConfigListeners();
    }

    public static void applyPlayback(JSONObject source) {
        Player.AdvanceMode advanceMode = readEnum(source, "advanceMode", playbackAdvanceMode);
        if (advanceMode == Player.AdvanceMode.SwingDown) {
            Log.warn("Ignoring internal playback advance mode value: " + advanceMode);
            advanceMode = playbackAdvanceMode;
        }
        Integer speed = readInteger(source, "speed", "playback speed");
        PlaybackSpeedUnit speedUnit = readEnum(source, "speedUnit", playbackSpeedUnit);
        Integer firstFrame = readInteger(source, "firstFrame", "playback first frame");
        Integer lastFrame = readInteger(source, "lastFrame", "playback last frame");

        setPlaybackAdvanceMode(advanceMode);
        if (speed != null || speedUnit != playbackSpeedUnit)
            setPlaybackSpeed(speed == null ? playbackSpeed : speed, speedUnit);
        if (firstFrame != null || lastFrame != null)
            Player.setPlaybackRange(firstFrame == null ? Player.getPlaybackFirstFrame() : firstFrame,
                    lastFrame == null ? Player.getPlaybackLastFrame() : lastFrame);
    }

    private static @Nullable Integer readInteger(JSONObject source, String key, String name) {
        String value = source.optString(key, null);
        if (value == null)
            return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            Log.warn("Ignoring invalid " + name + " value: " + value, e);
            return null;
        }
    }

    private static boolean readBoolean(JSONObject source, String key, boolean currentValue) {
        if (!source.has(key) || source.isNull(key))
            return currentValue;

        Object value = source.opt(key);
        if (value instanceof Boolean booleanValue)
            return booleanValue;

        Log.warn("Ignoring invalid " + key + " state value: " + value);
        return currentValue;
    }

    private static <E extends Enum<E>> E readEnum(JSONObject source, String key, E currentValue) {
        String value = source.optString(key, null);
        if (value == null)
            return currentValue;
        try {
            return Enum.valueOf(currentValue.getDeclaringClass(), value);
        } catch (IllegalArgumentException e) {
            Log.warn("Ignoring invalid " + key + " value: " + value, e);
            return currentValue;
        }
    }

    public static void setRecordingMode(RecordingMode newRecordingMode) {
        if (recordingMode == newRecordingMode)
            return;

        recordingMode = newRecordingMode;
        notifyRecordingConfigListeners();
    }

    public static void setRecordingSize(RecordingSize newRecordingSize) {
        if (recordingSize == newRecordingSize)
            return;

        recordingSize = newRecordingSize;
        notifyRecordingConfigListeners();
    }

    public static void applyRecording(JSONObject source) {
        RecordingMode mode = readEnum(source, "mode", recordingMode);
        RecordingSize size = readEnum(source, "size", recordingSize);
        setRecordingMode(mode);
        setRecordingSize(size);
    }

    public static void addModeListener(ModeListener listener) {
        if (!modeListeners.contains(listener))
            modeListeners.add(listener);
    }

    public static void addPlaybackConfigListener(PlaybackConfigListener listener) {
        if (!playbackConfigListeners.contains(listener)) {
            playbackConfigListeners.add(listener);
            listener.playbackConfigChanged();
        }
    }

    public static void addRecordingConfigListener(RecordingConfigListener listener) {
        if (!recordingConfigListeners.contains(listener)) {
            recordingConfigListeners.add(listener);
            listener.recordingConfigChanged();
        }
    }

    private static void notifyModeListeners() {
        if (suppressModeNotifications)
            return;
        modeListeners.forEach(ModeListener::modeStateChanged);
    }

    private static void notifyPlaybackConfigListeners() {
        playbackConfigListeners.forEach(PlaybackConfigListener::playbackConfigChanged);
    }

    private static void notifyRecordingConfigListeners() {
        recordingConfigListeners.forEach(RecordingConfigListener::recordingConfigChanged);
    }

    private ViewState() {}
}
