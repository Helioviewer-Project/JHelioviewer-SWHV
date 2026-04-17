package org.helioviewer.jhv.gui;

import java.awt.Dimension;
import java.util.ArrayList;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.json.JSONObject;

public final class ViewerState {

    private ViewerState() {
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
        try {
            projectionValue = ProjectionMode.valueOf(source.optString("projection", projectionValue.name()));
        } catch (Exception ignore) {
        }
        try {
            annotationModeValue = Interaction.AnnotationMode.valueOf(source.optString("annotationMode", annotationModeValue.name()));
        } catch (Exception ignore) {
        }

        return new ModeData(
                projectionValue,
                annotationModeValue,
                source.optBoolean("multiview", current.multiview()),
                source.optBoolean("tracking", current.tracking()),
                source.optBoolean("refresh", current.refresh()),
                source.optBoolean("showCorona", current.showCorona()),
                source.optBoolean("differentialRotation", current.differentialRotation()));
    }

    public static void writeMovieJson(JSONObject target) {
        target.put("play", moviePlaying);
    }

    public static boolean readMoviePlaying(JSONObject source) {
        return source.optBoolean("play", moviePlaying);
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
        boolean changed = !movieAvailable || movieMaxFrame != newMovieMaxFrame;
        movieAvailable = true;
        movieMaxFrame = newMovieMaxFrame;
        playbackFirstFrame = 0;
        playbackLastFrame = newMovieMaxFrame;
        Movie.setPlaybackRange(playbackFirstFrame, playbackLastFrame);
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
        playbackFirstFrame = 0;
        playbackLastFrame = 0;
        Movie.setPlaybackRange(playbackFirstFrame, playbackLastFrame);
        notifyPlaybackRangeListeners();
        if (changed)
            notifyMovieListeners();
    }

    public static void setMovieActiveFrame(int newMovieActiveFrame) {
        if (movieActiveFrame == newMovieActiveFrame)
            return;

        movieActiveFrame = newMovieActiveFrame;
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
        boolean changed = playbackSpeed != speed || playbackSpeedUnit != newPlaybackSpeedUnit;

        playbackSpeed = speed;
        playbackSpeedUnit = newPlaybackSpeedUnit;
        applyPlaybackSpeed();
        if (changed)
            notifyMovieListeners();
    }

    public static void setPlaybackRange(int newPlaybackFirstFrame, int newPlaybackLastFrame) {
        int firstFrame = Math.clamp(newPlaybackFirstFrame, 0, newPlaybackLastFrame);
        int lastFrame = Math.max(firstFrame, newPlaybackLastFrame);
        if (playbackFirstFrame == firstFrame && playbackLastFrame == lastFrame)
            return;

        playbackFirstFrame = firstFrame;
        playbackLastFrame = lastFrame;
        Movie.setPlaybackRange(firstFrame, lastFrame);
        notifyPlaybackRangeListeners();
    }

    private static void applyPlaybackSpeed() {
        if (playbackSpeedUnit.isRelative())
            Movie.setDesiredRelativeSpeed(playbackSpeed);
        else
            Movie.setDesiredAbsoluteSpeed(playbackSpeed * playbackSpeedUnit.secPerSecond());
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
