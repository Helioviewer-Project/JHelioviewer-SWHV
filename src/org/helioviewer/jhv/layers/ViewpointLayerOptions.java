package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.time.TimeListener;

import org.json.JSONObject;

public final class ViewpointLayerOptions implements TimeListener.Range {

    public enum CameraMode {
        ObserverAt1au("Observer at 1au", UpdateViewpoint.observerAt1au),
        Location("Location", UpdateViewpoint.location),
        Heliosphere("Heliosphere", UpdateViewpoint.equatorial);

        final String label;
        final UpdateViewpoint update;

        CameraMode(String _label, UpdateViewpoint _update) {
            label = _label;
            update = _update;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final ViewpointLayerOptionsExpert locationOptions;
    private final ViewpointLayerOptionsExpert equatorialOptions;

    private CameraMode cameraMode;

    public ViewpointLayerOptions(JSONObject jo) {
        JSONObject joLocation = null;
        JSONObject joEquatorial = null;
        if (jo != null) {
            joLocation = jo.optJSONObject("location");
            joEquatorial = jo.optJSONObject("equatorial");
        }
        locationOptions = new ViewpointLayerOptionsExpert(joLocation, UpdateViewpoint.location, SpaceObject.SUN, Frame.SOLO_IAU_SUN_2009, true);
        equatorialOptions = new ViewpointLayerOptionsExpert(joEquatorial, UpdateViewpoint.equatorial, SpaceObject.SUN, Frame.SOLO_HCI, false);

        cameraMode = CameraMode.Location;
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

    boolean isDownloading() {
        return locationOptions.isDownloading() || equatorialOptions.isDownloading();
    }

    public CameraMode getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(CameraMode _cameraMode, DisplayController.ViewpointApplyMode mode) {
        cameraMode = _cameraMode;
        applyCurrentViewpoint(mode);
    }

    public ViewpointLayerOptionsExpert getLocationOptions() {
        return locationOptions;
    }

    public ViewpointLayerOptionsExpert getEquatorialOptions() {
        return equatorialOptions;
    }

    void applyCurrentViewpoint(DisplayController.ViewpointApplyMode mode) {
        DisplayController.setViewpointUpdate(cameraMode.update, mode);
    }

    void activate() {
        Movie.addTimeRangeListener(this);
    }

    void deactivate() {
        Movie.removeTimeRangeListener(this);
    }

    @Override
    public void timeRangeChanged(long start, long end) {
        locationOptions.setTimespan(start, end);
        equatorialOptions.setTimespan(start, end);
    }

    boolean isHeliospheric() {
        return cameraMode == CameraMode.Heliosphere;
    }

    @Nullable
    PositionLoad getHighlightedLoad() {
        return isHeliospheric() ? equatorialOptions.getHighlightedLoad() : null;
    }

    int getSpiralSpeed() {
        return isHeliospheric() ? equatorialOptions.getSpiralSpeed() : 0;
    }

    boolean isRelative() {
        return isHeliospheric() && equatorialOptions.isRelative();
    }

}
