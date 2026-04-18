package org.helioviewer.jhv.app.state;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;

import org.json.JSONObject;

public final class ViewState {

    private ViewState() {
    }

    public interface ModeListener {
        void modeStateChanged();
    }

    public interface MovieListener {
        void movieStateChanged();
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
        ORIGINAL("On screen", 0, 0, false),
        H1024("1024×1024", 1024, 1024, true),
        H1080("1920×1080", 1920, 1080, true),
        H2048("2048×2048", 2048, 2048, true),
        H2160("3840×2160", 3840, 2160, true),
        H4096("4096×4096", 4096, 4096, true);

        private final String label;
        private final int width;
        private final int height;
        private final boolean internal;

        RecordingSize(String _label, int _width, int _height, boolean _internal) {
            label = _label;
            width = _width;
            height = _height;
            internal = _internal;
        }

        public boolean isInternal() {
            return internal;
        }

        public Dimension getSize() {
            if (this == ORIGINAL)
                return new Dimension(Display.fullViewport.width, Display.fullViewport.height);
            return new Dimension(width, height);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record ModeData(ProjectionMode projection, Interaction.AnnotationMode annotationMode, boolean multiview,
                           boolean tracking, boolean refresh, boolean showCorona, boolean differentialRotation) {
    }

    public record MovieData(boolean available, boolean playing, int maxFrame, int activeFrame, boolean recording) {
    }

    public record PlaybackData(Movie.AdvanceMode advanceMode, int speed, PlaybackSpeedUnit speedUnit,
                               int firstFrame, int lastFrame) {
    }

    public record RecordingData(RecordingMode mode, RecordingSize size) {
    }

    private static final ArrayList<ModeListener> modeListeners = new ArrayList<>();
    private static final ArrayList<MovieListener> movieListeners = new ArrayList<>();
    private static final ArrayList<PlaybackRangeListener> playbackRangeListeners = new ArrayList<>();
    private static boolean suppressModeNotifications;

    private static ProjectionMode projection = Display.mode;
    private static Interaction.AnnotationMode annotationMode = Interaction.AnnotationMode.Cross;
    private static boolean multiview = Display.multiview;
    private static boolean tracking = Display.getCamera().getTrackingMode();
    private static boolean refresh = ImageLayers.getRefreshMode();
    private static boolean showCorona = Display.getShowCorona();
    private static boolean differentialRotation = ImageLayers.getDiffRotationMode();
    private static boolean moviePlaying;
    private static boolean movieAvailable;
    private static int movieMaxFrame;
    private static int movieActiveFrame;
    public static final int PLAYBACK_SPEED_MIN = 1;
    public static final int PLAYBACK_SPEED_MAX = 120;
    private static Movie.AdvanceMode playbackAdvanceMode = Movie.AdvanceMode.Loop;
    private static int playbackSpeed = Movie.FPS_RELATIVE_DEFAULT;
    private static PlaybackSpeedUnit playbackSpeedUnit = PlaybackSpeedUnit.FRAMES_PER_SECOND;
    private static int playbackFirstFrame;
    private static int playbackLastFrame;
    private static RecordingMode recordingMode = RecordingMode.LOOP;
    private static RecordingSize recordingSize = RecordingSize.ORIGINAL;

    public static ModeData modeData() {
        return new ModeData(projection, getAnnotationMode(), multiview, tracking, refresh, showCorona, differentialRotation);
    }

    public static MovieData movieData() {
        return new MovieData(movieAvailable, moviePlaying, movieMaxFrame, movieActiveFrame, Movie.isRecording());
    }

    public static PlaybackData playbackData() {
        return new PlaybackData(playbackAdvanceMode, playbackSpeed, playbackSpeedUnit, playbackFirstFrame, playbackLastFrame);
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
        ProjectionMode projectionValue = current.projection();
        Interaction.AnnotationMode annotationModeValue = current.annotationMode();
        boolean multiviewValue = current.multiview();
        boolean trackingValue = current.tracking();
        boolean refreshValue = current.refresh();
        boolean showCoronaValue = current.showCorona();
        boolean differentialRotationValue = current.differentialRotation();
        String projectionName = source.optString("projection", projectionValue.name());
        String annotationModeName = source.optString("annotationMode", annotationModeValue.name());
        try {
            projectionValue = ProjectionMode.valueOf(projectionName);
        } catch (IllegalArgumentException e) {
            Log.warn("Ignoring invalid projection state value: " + projectionName, e);
        }
        try {
            annotationModeValue = Interaction.AnnotationMode.valueOf(annotationModeName);
        } catch (IllegalArgumentException e) {
            Log.warn("Ignoring invalid annotation mode state value: " + annotationModeName, e);
        }
        if (source.has("multiview") && !source.isNull("multiview")) {
            Object multiview = source.opt("multiview");
            if (multiview instanceof Boolean value)
                multiviewValue = value;
            else
                Log.warn("Ignoring invalid multiview state value: " + multiview);
        }
        if (source.has("tracking") && !source.isNull("tracking")) {
            Object tracking = source.opt("tracking");
            if (tracking instanceof Boolean value)
                trackingValue = value;
            else
                Log.warn("Ignoring invalid tracking state value: " + tracking);
        }
        if (source.has("refresh") && !source.isNull("refresh")) {
            Object refresh = source.opt("refresh");
            if (refresh instanceof Boolean value)
                refreshValue = value;
            else
                Log.warn("Ignoring invalid refresh state value: " + refresh);
        }
        if (source.has("showCorona") && !source.isNull("showCorona")) {
            Object showCorona = source.opt("showCorona");
            if (showCorona instanceof Boolean value)
                showCoronaValue = value;
            else
                Log.warn("Ignoring invalid showCorona state value: " + showCorona);
        }
        if (source.has("differentialRotation") && !source.isNull("differentialRotation")) {
            Object differentialRotation = source.opt("differentialRotation");
            if (differentialRotation instanceof Boolean value)
                differentialRotationValue = value;
            else
                Log.warn("Ignoring invalid differentialRotation state value: " + differentialRotation);
        }

        return new ModeData(
                projectionValue,
                annotationModeValue,
                multiviewValue,
                trackingValue,
                refreshValue,
                showCoronaValue,
                differentialRotationValue);
    }

    public static void writeMovieJson(JSONObject target) {
        target.put("play", moviePlaying);
    }

    public static boolean readMoviePlaying(JSONObject source) {
        if (!source.has("play") || source.isNull("play"))
            return moviePlaying;

        Object play = source.opt("play");
        if (play instanceof Boolean value)
            return value;

        Log.warn("Ignoring invalid movie play state value: " + play);
        return moviePlaying;
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

    // Commands-only partial update entry point.
    public static void applyModeUpdate(
            @Nullable ProjectionMode projection,
            @Nullable Interaction.AnnotationMode annotationMode,
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

    // Commands-only partial update entry point.
    public static void applyModeUpdateRaw(
            @Nullable String projection,
            @Nullable String annotationMode,
            @Nullable String multiview,
            @Nullable String tracking,
            @Nullable String refresh,
            @Nullable String showCorona,
            @Nullable String differentialRotation) {
        applyModeUpdate(
                parseEnum(projection, ProjectionMode.class, "projection"),
                parseEnum(annotationMode, Interaction.AnnotationMode.class, "annotation mode"),
                parseBoolean(multiview, "multiview"),
                parseBoolean(tracking, "tracking"),
                parseBoolean(refresh, "refresh"),
                parseBoolean(showCorona, "showCorona"),
                parseBoolean(differentialRotation, "differentialRotation"));
    }

    public static ProjectionMode getProjection() {
        return projection;
    }

    public static void setProjection(ProjectionMode newProjection) {
        if (projection == newProjection)
            return;

        projection = newProjection;
        Display.setProjectionMode(newProjection);
        notifyModeListeners();
    }

    public static void initFromInteraction() {
        if (JHVFrame.getInteraction() != null)
            annotationMode = JHVFrame.getInteraction().getAnnotationMode();
    }

    public static Interaction.AnnotationMode getAnnotationMode() {
        return annotationMode;
    }

    public static void setAnnotationMode(Interaction.AnnotationMode newAnnotationMode) {
        if (annotationMode == newAnnotationMode)
            return;

        annotationMode = newAnnotationMode;
        if (JHVFrame.getInteraction() != null)
            JHVFrame.getInteraction().setAnnotationMode(newAnnotationMode);
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
        Display.getCamera().setTrackingMode(newTracking);
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
        MovieDisplay.display();
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
        MovieDisplay.display();
        notifyModeListeners();
    }

    public static void setMoviePlaying(boolean newMoviePlaying) {
        if (moviePlaying == newMoviePlaying)
            return;

        moviePlaying = newMoviePlaying;
        notifyMovieListeners();
    }

    public static void setMovieAvailable(int newMovieMaxFrame) {
        int maxFrame = Math.max(0, newMovieMaxFrame);
        if (maxFrame != newMovieMaxFrame)
            Log.warn("Clamping invalid movie max frame " + newMovieMaxFrame + " to " + maxFrame);

        boolean changed = !movieAvailable || movieMaxFrame != maxFrame;
        movieAvailable = true;
        movieMaxFrame = maxFrame;
        applyPlaybackRangeState(0, maxFrame);
        notifyPlaybackRangeListeners();
        if (changed)
            notifyMovieListeners();
    }

    public static void clearMovie() {
        boolean changed = movieAvailable || movieMaxFrame != 0 || movieActiveFrame != 0 || moviePlaying || Movie.isRecording();
        movieAvailable = false;
        movieMaxFrame = 0;
        movieActiveFrame = 0;
        moviePlaying = false;
        applyPlaybackRangeState(0, 0);
        notifyPlaybackRangeListeners();
        if (changed)
            notifyMovieListeners();
    }

    public static void setMovieActiveFrame(int newMovieActiveFrame) {
        int maxFrame = movieAvailable ? movieMaxFrame : 0;
        int activeFrame = Math.clamp(newMovieActiveFrame, 0, maxFrame);
        if (activeFrame != newMovieActiveFrame)
            Log.warn("Clamping invalid movie active frame " + newMovieActiveFrame + " to " + activeFrame);

        if (movieActiveFrame == activeFrame)
            return;

        movieActiveFrame = activeFrame;
        notifyMovieListeners();
    }

    public static void movieRecordingChanged() {
        notifyMovieListeners();
    }

    public static void setPlaybackAdvanceMode(Movie.AdvanceMode newPlaybackAdvanceMode) {
        if (playbackAdvanceMode == newPlaybackAdvanceMode)
            return;

        playbackAdvanceMode = newPlaybackAdvanceMode;
        Movie.setAdvanceMode(newPlaybackAdvanceMode);
        notifyMovieListeners();
    }

    public static void setPlaybackSpeed(int newPlaybackSpeed, PlaybackSpeedUnit newPlaybackSpeedUnit) {
        int speed = Math.clamp(newPlaybackSpeed, PLAYBACK_SPEED_MIN, PLAYBACK_SPEED_MAX);
        if (speed != newPlaybackSpeed)
            Log.warn("Clamping invalid playback speed " + newPlaybackSpeed + " to " + speed);
        if (playbackSpeed == speed && playbackSpeedUnit == newPlaybackSpeedUnit)
            return;

        playbackSpeed = speed;
        playbackSpeedUnit = newPlaybackSpeedUnit;
        applyPlaybackSpeed();
        notifyMovieListeners();
    }

    // Commands-only partial update entry point.
    public static void applyPlaybackUpdate(
            @Nullable Movie.AdvanceMode advanceMode,
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

    // Commands-only partial update entry point.
    public static void applyPlaybackUpdateRaw(
            @Nullable String advanceMode,
            @Nullable String speed,
            @Nullable String speedUnit,
            @Nullable String firstFrame,
            @Nullable String lastFrame) {
        applyPlaybackUpdate(
                parseEnum(advanceMode, Movie.AdvanceMode.class, "playback advance mode"),
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
        RecordingMode resolvedMode = parseEnum(mode, RecordingMode.class, "recording mode");
        if (resolvedMode != null)
            setRecordingMode(resolvedMode);

        RecordingSize resolvedSize = parseEnum(size, RecordingSize.class, "recording size");
        if (resolvedSize != null)
            setRecordingSize(resolvedSize);

        Movie.AdvanceMode resolvedAdvanceMode = parseEnum(advanceMode, Movie.AdvanceMode.class, "playback advance mode");
        if (resolvedAdvanceMode != null)
            setPlaybackAdvanceMode(resolvedAdvanceMode);

        if (speed != null || speedUnit != null) {
            PlaybackData current = playbackData();
            Integer parsedSpeed = parseInteger(speed, "playback speed");
            PlaybackSpeedUnit parsedSpeedUnit = parseEnum(speedUnit, PlaybackSpeedUnit.class, "playback speed unit");
            int resolvedSpeed = parsedSpeed == null ? current.speed() : parsedSpeed;
            PlaybackSpeedUnit resolvedSpeedUnit = parsedSpeedUnit == null ? current.speedUnit() : parsedSpeedUnit;
            setPlaybackSpeed(resolvedSpeed, resolvedSpeedUnit);
        }
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

    private static void applyPlaybackSpeed() {
        if (playbackSpeedUnit.isRelative())
            Movie.setDesiredRelativeSpeed(playbackSpeed);
        else
            Movie.setDesiredAbsoluteSpeed(playbackSpeed * playbackSpeedUnit.secPerSecond());
    }

    private static void applyPlaybackRangeState(int firstFrame, int lastFrame) {
        playbackFirstFrame = firstFrame;
        playbackLastFrame = lastFrame;
        Movie.setPlaybackRange(firstFrame, lastFrame);
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

    public static void setRecordingMode(RecordingMode newRecordingMode) {
        if (recordingMode == newRecordingMode)
            return;

        recordingMode = newRecordingMode;
        notifyMovieListeners();
    }

    public static void setRecordingSize(RecordingSize newRecordingSize) {
        if (recordingSize == newRecordingSize)
            return;

        recordingSize = newRecordingSize;
        notifyMovieListeners();
    }

    // Commands-only partial update entry point.
    public static void applyRecordingUpdate(
            @Nullable RecordingMode mode,
            @Nullable RecordingSize size) {
        if (mode != null)
            setRecordingMode(mode);
        if (size != null)
            setRecordingSize(size);
    }

    // Commands-only partial update entry point.
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

    public static void addMovieListener(MovieListener listener) {
        if (!movieListeners.contains(listener))
            movieListeners.add(listener);
    }

    public static void removeMovieListener(MovieListener listener) {
        movieListeners.remove(listener);
    }

    public static void addPlaybackRangeListener(PlaybackRangeListener listener) {
        if (!playbackRangeListeners.contains(listener))
            playbackRangeListeners.add(listener);
    }

    public static void removePlaybackRangeListener(PlaybackRangeListener listener) {
        playbackRangeListeners.remove(listener);
    }

    private static void notifyModeListeners() {
        if (suppressModeNotifications)
            return;
        modeListeners.forEach(ModeListener::modeStateChanged);
    }

    private static void notifyMovieListeners() {
        movieListeners.forEach(MovieListener::movieStateChanged);
    }

    private static void notifyPlaybackRangeListeners() {
        playbackRangeListeners.forEach(PlaybackRangeListener::playbackRangeChanged);
    }
}
