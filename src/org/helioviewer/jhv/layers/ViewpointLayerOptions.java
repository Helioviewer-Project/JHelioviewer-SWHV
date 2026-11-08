package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeListener;

import org.json.JSONObject;

public final class ViewpointLayerOptions implements TimeListener.Range {

    public enum CameraMode {
        Observer("Observer"),
        ObserverAt1au("Observer at 1au"),
        Location("Location"),
        Heliosphere("Heliosphere");

        final String label;

        CameraMode(String _label) {
            label = _label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    final ViewpointSky sky;

    private final ViewpointLayerOptionsExpert locationOptions;
    private final ViewpointLayerOptionsExpert equatorialOptions;

    private CameraMode cameraMode;
    private boolean active;

    public ViewpointLayerOptions(JSONObject jo) {
        JSONObject joLocation = null;
        JSONObject joEquatorial = null;
        if (jo != null) {
            joLocation = jo.optJSONObject("location");
            joEquatorial = jo.optJSONObject("equatorial");
        }
        locationOptions = new ViewpointLayerOptionsExpert(joLocation, Frame.SOLO_IAU_SUN_2009, true);
        equatorialOptions = new ViewpointLayerOptionsExpert(joEquatorial, Frame.SOLO_HEEQ, false);
        sky = new ViewpointSky(locationOptions);
        locationOptions.setSkyChangeListener(sky::reload);
        locationOptions.setChangeListener(() -> optionStateChanged(CameraMode.Location));
        equatorialOptions.setChangeListener(() -> optionStateChanged(CameraMode.Heliosphere));

        cameraMode = jo == null ? CameraMode.Observer : CameraMode.Location;
        if (jo != null) {
            try {
                cameraMode = CameraMode.valueOf(jo.optString("mode"));
            } catch (Exception ignore) {}
            JSONObject jc = jo.optJSONObject("camera");
            if (jc != null)
                DisplayController.cameraFromJson(jc);
        }
    }

    void serialize(JSONObject jo) {
        jo.put("mode", cameraMode.name());
        jo.put("camera", DisplayController.cameraToJson());
        jo.put("location", locationOptions.toJson());
        jo.put("equatorial", equatorialOptions.toJson());
    }

    public CameraMode getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(CameraMode _cameraMode, DisplayController.ViewpointApplyMode mode) {
        cameraMode = _cameraMode;
        updateSky();
        if (active)
            applyCurrentViewpoint(mode);
    }

    public ViewpointLayerOptionsExpert getLocationOptions() {
        return locationOptions;
    }

    public ViewpointLayerOptionsExpert getEquatorialOptions() {
        return equatorialOptions;
    }

    void applyCurrentViewpoint(DisplayController.ViewpointApplyMode mode) {
        DisplayController.setViewpointUpdate(createViewpointUpdate(), mode);
    }

    private UpdateViewpoint createViewpointUpdate() {
        long start = Player.getStartTime();
        long end = Player.getEndTime();
        return switch (cameraMode) {
            case Observer -> UpdateViewpoint.observer;
            case ObserverAt1au -> UpdateViewpoint.observerAt1au;
            case Location -> new UpdateViewpoint.Location(locationOptions.getHighlightedLoad(), start, end);
            case Heliosphere ->
                    new UpdateViewpoint.Equatorial(equatorialOptions.getHighlightedLoad(), equatorialOptions.getFrame(), equatorialOptions.isRelative(),
                            start, end);
        };
    }

    private void optionStateChanged(CameraMode mode) {
        if (active && cameraMode == mode) {
            applyCurrentViewpoint(DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
            DisplayController.render();
        }
    }

    void activate() {
        Player.addTimeRangeListener(this);
        active = true;
        updateSky();
    }

    void deactivate() {
        Player.removeTimeRangeListener(this);
        active = false;
        updateSky();
    }

    private void updateSky() {
        sky.setActive(active && cameraMode == CameraMode.Location);
    }

    @Override
    public void timeRangeChanged(long start, long end) {
        locationOptions.setTimespan(start, end);
        equatorialOptions.setTimespan(start, end);
        optionStateChanged(cameraMode);
    }

    boolean isLocation() {
        return cameraMode == CameraMode.Location;
    }

    boolean isHeliospheric() {
        return cameraMode == CameraMode.Heliosphere;
    }

}
