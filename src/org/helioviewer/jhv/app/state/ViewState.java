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

    public interface PlaybackRangeListener {
        void playbackRangeChanged();
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

    public record ModeData(MapMode projection, AnnotationMode annotationMode, boolean multiview,
                           boolean tracking, boolean refresh, boolean showCorona, boolean differentialRotation) {}

    public record PlaybackData(Player.AdvanceMode advanceMode, int speed, PlaybackSpeedUnit speedUnit,
                               int firstFrame, int lastFrame) {}

    public record RecordingData(RecordingMode mode, RecordingSize size) {}

    public record Size(int width, int height, boolean internal) {}

    private static final ArrayList<ModeListener> modeListeners = new ArrayList<>();
    private static final ArrayList<PlaybackConfigListener> playbackConfigListeners = new ArrayList<>();
    private static final ArrayList<PlaybackRangeListener> playbackRangeListeners = new ArrayList<>();
    private static final ArrayList<RecordingConfigListener> recordingConfigListeners = new ArrayList<>();
    private static boolean suppressModeNotifications;

    private static MapMode projection = Display.mode;
    private static AnnotationMode annotationMode = AnnotationMode.Cross;
    private static boolean multiview = Display.multiview;
    private static boolean tracking = DisplayController.getTrackingMode();
    private static boolean refresh = ImageLayers.getRefreshMode();
    private static boolean showCorona = Display.getShowCorona();
    private static boolean differentialRotation = ImageLayers.getDiffRotationMode();
    public static final int PLAYBACK_SPEED_MIN = 1;
    public static final int PLAYBACK_SPEED_MAX = 120;
    private static Player.AdvanceMode playbackAdvanceMode = Player.AdvanceMode.Loop;
    private static int playbackSpeed = Player.FPS_RELATIVE_DEFAULT;
    private static PlaybackSpeedUnit playbackSpeedUnit = PlaybackSpeedUnit.FRAMES_PER_SECOND;
    private static int playbackFirstFrame;
    private static int playbackLastFrame;
    private static RecordingMode recordingMode = RecordingMode.LOOP;
    private static RecordingSize recordingSize = RecordingSize.ORIGINAL;

    public static ModeData modeData() {
        return new ModeData(projection, getAnnotationMode(), multiview, tracking, refresh, showCorona, differentialRotation);
    }

    public static PlaybackData playbackData() {
        return new PlaybackData(playbackAdvanceMode, playbackSpeed, playbackSpeedUnit, playbackFirstFrame, playbackLastFrame);
    }

    // Estimated real-time length (seconds) of one pass through a movie of `frames` frames spanning
    // `spanMillis` of solar time, at the current playback speed. Frames/sec mode records at `speed`
    // fps (length = frames/speed); solar-time modes play the span at speed*secPerSecond (see ExportMovie).
    public static double estimateVideoSeconds(int frames, long spanMillis) {
        PlaybackData pd = playbackData();
        if (pd.speedUnit().isRelative())
            return pd.speed() <= 0 ? 0 : frames / (double) pd.speed();
        long rate = (long) pd.speed() * pd.speedUnit().secPerSecond();
        return rate <= 0 ? 0 : (spanMillis / 1000.) / rate;
    }

    public static RecordingData recordingData() {
        return new RecordingData(recordingMode, recordingSize);
    }

    public static void writeModeJson(JSONObject target) {
        ModeData data = modeData();
        target.put("multiview", data.multiview());
        target.put("projection", data.projection());
        target.put("annotationMode", data.annotationMode());
        target.put("tracking", data.tracking());
        target.put("refresh", data.refresh());
        target.put("showCorona", data.showCorona());
        target.put("differentialRotation", data.differentialRotation());
    }

    public static ModeData readModeJson(JSONObject source) {
        ModeData current = modeData();
        MapMode projectionValue = current.projection();
        AnnotationMode annotationModeValue = current.annotationMode();
        boolean multiviewValue = current.multiview();
        boolean trackingValue = current.tracking();
        boolean refreshValue = current.refresh();
        boolean showCoronaValue = current.showCorona();
        boolean differentialRotationValue = current.differentialRotation();
        String projectionName = source.optString("projection", projectionValue.name());
        String annotationModeName = source.optString("annotationMode", annotationModeValue.name());
        try {
            projectionValue = MapMode.valueOf(projectionName);
        } catch (IllegalArgumentException e) {
            Log.warn("Ignoring invalid projection state value: " + projectionName, e);
        }
        try {
            annotationModeValue = AnnotationMode.valueOf(annotationModeName);
        } catch (IllegalArgumentException e) {
            Log.warn("Ignoring invalid annotation mode state value: " + annotationModeName, e);
        }
        multiviewValue = readBoolean(source, "multiview", multiviewValue);
        trackingValue = readBoolean(source, "tracking", trackingValue);
        refreshValue = readBoolean(source, "refresh", refreshValue);
        showCoronaValue = readBoolean(source, "showCorona", showCoronaValue);
        differentialRotationValue = readBoolean(source, "differentialRotation", differentialRotationValue);

        return new ModeData(
                projectionValue,
                annotationModeValue,
                multiviewValue,
                trackingValue,
                refreshValue,
                showCoronaValue,
                differentialRotationValue);
    }

    public static void applyMode(ModeData data) {
        boolean changed = projection != data.projection()
                || annotationMode != data.annotationMode()
                || multiview != data.multiview()
                || tracking != data.tracking()
                || refresh != data.refresh()
                || showCorona != data.showCorona()
                || differentialRotation != data.differentialRotation();

        if (!changed)
            return;

        suppressModeNotifications = true;
        try {
            setProjection(data.projection());
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

    private static void applyModeUpdate(
            @Nullable MapMode projection,
            @Nullable AnnotationMode annotationMode,
            @Nullable Boolean multiview,
            @Nullable Boolean tracking,
            @Nullable Boolean refresh,
            @Nullable Boolean showCorona,
            @Nullable Boolean differentialRotation) {
        ModeData current = modeData();
        applyMode(new ModeData(
                projection == null ? current.projection() : projection,
                annotationMode == null ? current.annotationMode() : annotationMode,
                multiview == null ? current.multiview() : multiview,
                tracking == null ? current.tracking() : tracking,
                refresh == null ? current.refresh() : refresh,
                showCorona == null ? current.showCorona() : showCorona,
                differentialRotation == null ? current.differentialRotation() : differentialRotation));
    }

    public static void applyModeUpdateRaw(
            @Nullable String projection,
            @Nullable String annotationMode,
            @Nullable String multiview,
            @Nullable String tracking,
            @Nullable String refresh,
            @Nullable String showCorona,
            @Nullable String differentialRotation) {
        applyModeUpdate(
                parseEnum(projection, MapMode.class, "projection"),
                parseEnum(annotationMode, AnnotationMode.class, "annotation mode"),
                parseBoolean(multiview, "multiview"),
                parseBoolean(tracking, "tracking"),
                parseBoolean(refresh, "refresh"),
                parseBoolean(showCorona, "showCorona"),
                parseBoolean(differentialRotation, "differentialRotation"));
    }

    public static MapMode getProjection() {
        return projection;
    }

    public static void setProjection(MapMode newProjection) {
        if (projection == newProjection)
            return;

        projection = newProjection;
        Display.setMapMode(newProjection);
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
        return multiview;
    }

    public static void setMultiview(boolean newMultiview) {
        if (multiview == newMultiview)
            return;

        multiview = newMultiview;
        Display.multiview = newMultiview;
        ImageLayers.arrangeMultiView(newMultiview);
        notifyModeListeners();
    }

    public static boolean isTracking() {
        return tracking;
    }

    public static void setTracking(boolean newTracking) {
        if (tracking == newTracking)
            return;

        tracking = newTracking;
        DisplayController.setTrackingMode(newTracking);
        notifyModeListeners();
    }

    public static boolean isRefresh() {
        return refresh;
    }

    public static void setRefresh(boolean newRefresh) {
        if (refresh == newRefresh)
            return;

        refresh = newRefresh;
        ImageLayers.setRefreshMode(newRefresh);
        notifyModeListeners();
    }

    public static boolean isShowCorona() {
        return showCorona;
    }

    public static void setShowCorona(boolean newShowCorona) {
        if (showCorona == newShowCorona)
            return;

        showCorona = newShowCorona;
        Display.setShowCorona(newShowCorona);
        DisplayController.display();
        notifyModeListeners();
    }

    public static boolean isDifferentialRotation() {
        return differentialRotation;
    }

    public static void setDifferentialRotation(boolean newDifferentialRotation) {
        if (differentialRotation == newDifferentialRotation)
            return;

        differentialRotation = newDifferentialRotation;
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

    private static void applyPlaybackUpdate(
            @Nullable Player.AdvanceMode advanceMode,
            @Nullable Integer speed,
            @Nullable PlaybackSpeedUnit speedUnit,
            @Nullable Integer firstFrame,
            @Nullable Integer lastFrame) {
        if (advanceMode != null)
            setPlaybackAdvanceMode(advanceMode);

        PlaybackData current = playbackData();
        if (speed != null || speedUnit != null) {
            int mergedSpeed = speed == null ? current.speed() : speed;
            PlaybackSpeedUnit mergedSpeedUnit = speedUnit == null ? current.speedUnit() : speedUnit;
            setPlaybackSpeed(mergedSpeed, mergedSpeedUnit);
        }

        if (firstFrame != null || lastFrame != null) {
            int mergedFirstFrame = firstFrame == null ? current.firstFrame() : firstFrame;
            int mergedLastFrame = lastFrame == null ? current.lastFrame() : lastFrame;
            setPlaybackRange(mergedFirstFrame, mergedLastFrame);
        }
    }

    public static void applyPlaybackUpdateRaw(
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit,
            @Nullable String firstFrame,
            @Nullable String lastFrame) {
        applyPlaybackUpdate(
                parseAdvanceMode(advanceMode),
                parseInteger(speed, "playback speed"),
                parseEnum(speedUnit, PlaybackSpeedUnit.class, "playback speed unit"),
                parseInteger(firstFrame, "playback first frame"),
                parseInteger(lastFrame, "playback last frame"));
    }

    // Commands/ExportMovie-only partial update entry point.
    public static void applyRecordStartUpdate(
            @Nullable String mode,
            @Nullable String size,
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit) {
        applyRecordingUpdateRaw(mode, size);
        applyPlaybackUpdateRaw(advanceMode, speed, speedUnit, null, null);
    }

    public static void setPlaybackRange(int newPlaybackFirstFrame, int newPlaybackLastFrame) {
        int lastFrame = Math.max(0, newPlaybackLastFrame);
        int firstFrame = Math.clamp(newPlaybackFirstFrame, 0, lastFrame);
        if (lastFrame != newPlaybackLastFrame)
            Log.warn("Clamping invalid playback last frame " + newPlaybackLastFrame + " to " + lastFrame);
        if (firstFrame != newPlaybackFirstFrame)
            Log.warn("Clamping invalid playback first frame " + newPlaybackFirstFrame + " to " + firstFrame);
        if (playbackFirstFrame == firstFrame && playbackLastFrame == lastFrame)
            return;

        applyPlaybackRangeState(firstFrame, lastFrame);
        notifyPlaybackRangeListeners();
    }

    private static void applyPlaybackRangeState(int firstFrame, int lastFrame) {
        playbackFirstFrame = firstFrame;
        playbackLastFrame = lastFrame;
        Player.setPlaybackRange(firstFrame, lastFrame);
    }

    private static @Nullable Integer parseInteger(@Nullable String value, String name) {
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

    private static @Nullable Boolean parseBoolean(@Nullable String value, String name) {
        if (value == null)
            return null;
        if ("true".equalsIgnoreCase(value))
            return true;
        if ("false".equalsIgnoreCase(value))
            return false;
        Log.warn("Ignoring invalid " + name + " value: " + value);
        return null;
    }

    private static <E extends Enum<E>> @Nullable E parseEnum(@Nullable String value, Class<E> enumClass, String name) {
        if (value == null)
            return null;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            Log.warn("Ignoring invalid " + name + " value: " + value, e);
            return null;
        }
    }

    private static @Nullable Player.AdvanceMode parseAdvanceMode(@Nullable String value) {
        Player.AdvanceMode mode = parseEnum(value, Player.AdvanceMode.class, "playback advance mode");
        if (mode == Player.AdvanceMode.SwingDown) {
            Log.warn("Ignoring internal playback advance mode value: " + value);
            return null;
        }
        return mode;
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

    private static void applyRecordingUpdate(
            @Nullable RecordingMode mode,
            @Nullable RecordingSize size) {
        if (mode != null)
            setRecordingMode(mode);
        if (size != null)
            setRecordingSize(size);
    }

    public static void applyRecordingUpdateRaw(
            @Nullable String mode,
            @Nullable String size) {
        applyRecordingUpdate(
                parseEnum(mode, RecordingMode.class, "recording mode"),
                parseEnum(size, RecordingSize.class, "recording size"));
    }

    public static void addModeListener(ModeListener listener) {
        if (!modeListeners.contains(listener))
            modeListeners.add(listener);
    }

    public static void removeModeListener(ModeListener listener) {
        modeListeners.remove(listener);
    }

    public static void addPlaybackConfigListener(PlaybackConfigListener listener) {
        if (!playbackConfigListeners.contains(listener)) {
            playbackConfigListeners.add(listener);
            listener.playbackConfigChanged();
        }
    }

    public static void removePlaybackConfigListener(PlaybackConfigListener listener) {
        playbackConfigListeners.remove(listener);
    }

    public static void addPlaybackRangeListener(PlaybackRangeListener listener) {
        if (!playbackRangeListeners.contains(listener)) {
            playbackRangeListeners.add(listener);
            listener.playbackRangeChanged();
        }
    }

    public static void removePlaybackRangeListener(PlaybackRangeListener listener) {
        playbackRangeListeners.remove(listener);
    }

    public static void addRecordingConfigListener(RecordingConfigListener listener) {
        if (!recordingConfigListeners.contains(listener)) {
            recordingConfigListeners.add(listener);
            listener.recordingConfigChanged();
        }
    }

    public static void removeRecordingConfigListener(RecordingConfigListener listener) {
        recordingConfigListeners.remove(listener);
    }

    private static void notifyModeListeners() {
        if (suppressModeNotifications)
            return;
        modeListeners.forEach(ModeListener::modeStateChanged);
    }

    private static void notifyPlaybackConfigListeners() {
        playbackConfigListeners.forEach(PlaybackConfigListener::playbackConfigChanged);
    }

    private static void notifyPlaybackRangeListeners() {
        playbackRangeListeners.forEach(PlaybackRangeListener::playbackRangeChanged);
    }

    private static void notifyRecordingConfigListeners() {
        recordingConfigListeners.forEach(RecordingConfigListener::recordingConfigChanged);
    }

    private ViewState() {}
}
