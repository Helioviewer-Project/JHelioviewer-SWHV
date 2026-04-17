package org.helioviewer.jhv.gui;

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

    public enum PlaybackSpeedUnit {
        FRAMES_PER_SECOND(0),
        MINUTES_PER_SECOND(60),
        HOURS_PER_SECOND(3600),
        DAYS_PER_SECOND(86400);

        private final int secPerSecond;

        PlaybackSpeedUnit(int _secPerSecond) {
            secPerSecond = _secPerSecond;
        }

        public boolean isRelative() {
            return this == FRAMES_PER_SECOND;
        }

        public int secPerSecond() {
            return secPerSecond;
        }
    }

    public record ModeData(ProjectionMode projection, Interaction.AnnotationMode annotationMode, boolean multiview,
                           boolean tracking, boolean refresh, boolean showCorona, boolean differentialRotation) {
    }

    public record MovieData(boolean available, boolean playing, int maxFrame, int activeFrame) {
    }

    public record PlaybackData(Movie.AdvanceMode advanceMode, int speed, PlaybackSpeedUnit speedUnit) {
    }

    private static final ArrayList<ModeListener> modeListeners = new ArrayList<>();
    private static final ArrayList<MovieListener> movieListeners = new ArrayList<>();

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

    public static ModeData modeData() {
        return new ModeData(projection, getAnnotationMode(), multiview, tracking, refresh, showCorona, differentialRotation);
    }

    public static MovieData movieData() {
        return new MovieData(movieAvailable, moviePlaying, movieMaxFrame, movieActiveFrame);
    }

    public static PlaybackData playbackData() {
        return new PlaybackData(playbackAdvanceMode, playbackSpeed, playbackSpeedUnit);
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

    public static MovieData readMovieJson(JSONObject source) {
        MovieData current = movieData();
        return new MovieData(
                current.available(),
                source.optBoolean("play", current.playing()),
                current.maxFrame(),
                current.activeFrame());
    }

    public static void applyMode(ModeData data) {
        ProjectionMode newProjection = data.projection();
        Interaction.AnnotationMode newAnnotationMode = data.annotationMode();
        boolean changed = false;
        boolean needsDisplay = false;

        if (projection != newProjection) {
            projection = newProjection;
            Display.setProjectionMode(newProjection);
            changed = true;
        }
        if (annotationMode != newAnnotationMode) {
            annotationMode = newAnnotationMode;
            if (JHVFrame.getInteraction() != null)
                JHVFrame.getInteraction().setAnnotationMode(newAnnotationMode);
            changed = true;
        }
        if (multiview != data.multiview()) {
            multiview = data.multiview();
            Display.multiview = multiview;
            ImageLayers.arrangeMultiView(multiview);
            changed = true;
        }
        if (tracking != data.tracking()) {
            tracking = data.tracking();
            Display.getCamera().setTrackingMode(tracking);
            changed = true;
        }
        if (refresh != data.refresh()) {
            refresh = data.refresh();
            ImageLayers.setRefreshMode(refresh);
            changed = true;
        }
        if (showCorona != data.showCorona()) {
            showCorona = data.showCorona();
            Display.setShowCorona(showCorona);
            changed = true;
            needsDisplay = true;
        }
        if (differentialRotation != data.differentialRotation()) {
            differentialRotation = data.differentialRotation();
            ImageLayers.setDiffRotationMode(differentialRotation);
            changed = true;
            needsDisplay = true;
        }

        if (needsDisplay)
            MovieDisplay.display();
        if (changed)
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

    public static boolean isMoviePlaying() {
        return moviePlaying;
    }

    public static void setMoviePlaying(boolean newMoviePlaying) {
        if (moviePlaying == newMoviePlaying)
            return;

        moviePlaying = newMoviePlaying;
        notifyMovieListeners();
    }

    public static boolean isMovieAvailable() {
        return movieAvailable;
    }

    public static int getMovieMaxFrame() {
        return movieMaxFrame;
    }

    public static void setMovieAvailable(int newMovieMaxFrame) {
        boolean changed = !movieAvailable || movieMaxFrame != newMovieMaxFrame;
        movieAvailable = true;
        movieMaxFrame = newMovieMaxFrame;
        if (changed)
            notifyMovieListeners();
    }

    public static void clearMovie() {
        boolean changed = movieAvailable || movieMaxFrame != 0 || movieActiveFrame != 0 || moviePlaying;
        movieAvailable = false;
        movieMaxFrame = 0;
        movieActiveFrame = 0;
        moviePlaying = false;
        if (changed)
            notifyMovieListeners();
    }

    public static int getMovieActiveFrame() {
        return movieActiveFrame;
    }

    public static void setMovieActiveFrame(int newMovieActiveFrame) {
        if (movieActiveFrame == newMovieActiveFrame)
            return;

        movieActiveFrame = newMovieActiveFrame;
        notifyMovieListeners();
    }

    public static Movie.AdvanceMode getPlaybackAdvanceMode() {
        return playbackAdvanceMode;
    }

    public static void setPlaybackAdvanceMode(Movie.AdvanceMode newPlaybackAdvanceMode) {
        if (playbackAdvanceMode == newPlaybackAdvanceMode)
            return;

        playbackAdvanceMode = newPlaybackAdvanceMode;
        Movie.setAdvanceMode(newPlaybackAdvanceMode);
        notifyMovieListeners();
    }

    public static int getPlaybackSpeed() {
        return playbackSpeed;
    }

    public static PlaybackSpeedUnit getPlaybackSpeedUnit() {
        return playbackSpeedUnit;
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

    private static void applyPlaybackSpeed() {
        if (playbackSpeedUnit.isRelative())
            Movie.setDesiredRelativeSpeed(playbackSpeed);
        else
            Movie.setDesiredAbsoluteSpeed(playbackSpeed * playbackSpeedUnit.secPerSecond());
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

    private static void notifyModeListeners() {
        modeListeners.forEach(ModeListener::modeStateChanged);
    }

    private static void notifyMovieListeners() {
        movieListeners.forEach(MovieListener::movieStateChanged);
    }
}
