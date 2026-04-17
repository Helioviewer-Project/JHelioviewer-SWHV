package org.helioviewer.jhv.gui;

import java.util.ArrayList;
import java.util.Objects;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.json.JSONObject;

public final class ViewerState {

    public interface Listener {
        void viewerStateChanged();
    }

    public record Data(ProjectionMode projection, boolean multiview, boolean tracking,
                       boolean refresh, boolean showCorona, boolean differentialRotation) {
    }

    private final ArrayList<Listener> listeners = new ArrayList<>();

    private ProjectionMode projection = Display.mode;
    private boolean multiview = Display.multiview;
    private boolean tracking = Display.getCamera().getTrackingMode();
    private boolean refresh = ImageLayers.getRefreshMode();
    private boolean showCorona = Display.getShowCorona();
    private boolean differentialRotation = ImageLayers.getDiffRotationMode();

    public Data data() {
        return new Data(projection, multiview, tracking, refresh, showCorona, differentialRotation);
    }

    public void writeJson(JSONObject target) {
        Data data = data();
        target.put("multiview", data.multiview());
        target.put("projection", data.projection());
        target.put("tracking", data.tracking());
        target.put("refresh", data.refresh());
        target.put("showCorona", data.showCorona());
        target.put("differentialRotation", data.differentialRotation());
    }

    public Data readJson(JSONObject source) {
        Data current = data();
        ProjectionMode projectionValue = current.projection();
        try {
            projectionValue = ProjectionMode.valueOf(source.optString("projection", projectionValue.name()));
        } catch (Exception ignore) {
        }

        return new Data(
                projectionValue,
                source.optBoolean("multiview", current.multiview()),
                source.optBoolean("tracking", current.tracking()),
                source.optBoolean("refresh", current.refresh()),
                source.optBoolean("showCorona", current.showCorona()),
                source.optBoolean("differentialRotation", current.differentialRotation()));
    }

    public void apply(Data data) {
        ProjectionMode newProjection = Objects.requireNonNull(data.projection());
        boolean changed = false;
        boolean needsDisplay = false;

        if (projection != newProjection) {
            projection = newProjection;
            Display.setProjectionMode(newProjection);
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
            notifyListeners();
    }

    public ProjectionMode getProjection() {
        return projection;
    }

    public void setProjection(ProjectionMode newProjection) {
        ProjectionMode value = Objects.requireNonNull(newProjection);
        if (projection == value)
            return;

        projection = value;
        Display.setProjectionMode(value);
        notifyListeners();
    }

    public boolean isMultiview() {
        return multiview;
    }

    public void setMultiview(boolean newMultiview) {
        if (multiview == newMultiview)
            return;

        multiview = newMultiview;
        Display.multiview = newMultiview;
        ImageLayers.arrangeMultiView(newMultiview);
        notifyListeners();
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setTracking(boolean newTracking) {
        if (tracking == newTracking)
            return;

        tracking = newTracking;
        Display.getCamera().setTrackingMode(newTracking);
        notifyListeners();
    }

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean newRefresh) {
        if (refresh == newRefresh)
            return;

        refresh = newRefresh;
        ImageLayers.setRefreshMode(newRefresh);
        notifyListeners();
    }

    public boolean isShowCorona() {
        return showCorona;
    }

    public void setShowCorona(boolean newShowCorona) {
        if (showCorona == newShowCorona)
            return;

        showCorona = newShowCorona;
        Display.setShowCorona(newShowCorona);
        MovieDisplay.display();
        notifyListeners();
    }

    public boolean isDifferentialRotation() {
        return differentialRotation;
    }

    public void setDifferentialRotation(boolean newDifferentialRotation) {
        if (differentialRotation == newDifferentialRotation)
            return;

        differentialRotation = newDifferentialRotation;
        ImageLayers.setDiffRotationMode(newDifferentialRotation);
        MovieDisplay.display();
        notifyListeners();
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Listener::viewerStateChanged);
    }
}
