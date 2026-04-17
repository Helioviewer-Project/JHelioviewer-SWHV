package org.helioviewer.jhv.gui;

import java.util.ArrayList;
import java.util.Objects;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.MovieDisplay;

public final class ViewerState {

    public interface Listener {
        void viewerStateChanged();
    }

    private final ArrayList<Listener> listeners = new ArrayList<>();

    private ProjectionMode projection = Display.mode;
    private boolean multiview = Display.multiview;
    private boolean tracking = Display.getCamera().getTrackingMode();
    private boolean refresh = ImageLayers.getRefreshMode();
    private boolean showCorona = Display.getShowCorona();
    private boolean differentialRotation = ImageLayers.getDiffRotationMode();

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
