package org.helioviewer.jhv.gui;

import java.util.ArrayList;
import java.util.Objects;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.layers.ImageLayers;
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

    public record ModeData(ProjectionMode projection, Interaction.AnnotationMode annotationMode, boolean multiview,
                           boolean tracking, boolean refresh, boolean showCorona, boolean differentialRotation) {
    }

    public record MovieData(boolean available, boolean playing, int maxFrame, int activeFrame) {
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

    public static ModeData modeData() {
        return new ModeData(projection, annotationMode, multiview, tracking, refresh, showCorona, differentialRotation);
    }

    public static MovieData movieData() {
        return new MovieData(movieAvailable, moviePlaying, movieMaxFrame, movieActiveFrame);
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
        ProjectionMode newProjection = Objects.requireNonNull(data.projection());
        Interaction.AnnotationMode newAnnotationMode = Objects.requireNonNull(data.annotationMode());
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
        ProjectionMode value = Objects.requireNonNull(newProjection);
        if (projection == value)
            return;

        projection = value;
        Display.setProjectionMode(value);
        notifyModeListeners();
    }

    public static Interaction.AnnotationMode getAnnotationMode() {
        return annotationMode;
    }

    public static void setAnnotationMode(Interaction.AnnotationMode newAnnotationMode) {
        Interaction.AnnotationMode value = Objects.requireNonNull(newAnnotationMode);
        if (annotationMode == value)
            return;

        annotationMode = value;
        if (JHVFrame.getInteraction() != null)
            JHVFrame.getInteraction().setAnnotationMode(value);
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
