package org.helioviewer.jhv.layers;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
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

    private final ViewpointLayerOptionsExpert locationOptionPanel;
    private final ViewpointLayerOptionsExpert equatorialOptionPanel;

    private CameraMode cameraMode;

    public ViewpointLayerOptions(JSONObject jo) {
        JSONObject joLocation = null;
        JSONObject joEquatorial = null;
        if (jo != null) {
            joLocation = jo.optJSONObject("location");
            joEquatorial = jo.optJSONObject("equatorial");
        }
        locationOptionPanel = new ViewpointLayerOptionsExpert(joLocation, UpdateViewpoint.location, SpaceObject.SUN, Frame.SOLO_IAU_SUN_2009, true);
        equatorialOptionPanel = new ViewpointLayerOptionsExpert(joEquatorial, UpdateViewpoint.equatorial, SpaceObject.SUN, Frame.SOLO_HCI, false);

        cameraMode = CameraMode.Location;
        if (jo != null) {
            try {
                cameraMode = CameraMode.valueOf(jo.optString("mode"));
            } catch (Exception ignore) {}
            JSONObject jc = jo.optJSONObject("camera");
            if (jc != null)
                Display.getCamera().fromJson(jc);
        }
    }

    void serialize(JSONObject jo) {
        jo.put("mode", cameraMode.name());
        jo.put("camera", Display.getCamera().toJson());
        jo.put("location", locationOptionPanel.toJson());
        jo.put("equatorial", equatorialOptionPanel.toJson());
    }

    boolean isDownloading() {
        return locationOptionPanel.isDownloading() || equatorialOptionPanel.isDownloading();
    }

    public CameraMode getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(CameraMode _cameraMode, Camera.ViewpointApplyMode mode) {
        cameraMode = _cameraMode;
        applyCurrentViewpoint(mode);
    }

    public Component getCurrentOptionPanel() {
        return switch (cameraMode) {
            case ObserverAt1au -> null;
            case Location -> locationOptionPanel;
            case Heliosphere -> equatorialOptionPanel;
        };
    }

    void applyCurrentViewpoint(Camera.ViewpointApplyMode mode) {
        Display.getCamera().setViewpointUpdate(cameraMode.update, mode);
    }

    void activate() {
        Movie.addTimeRangeListener(this);
    }

    void deactivate() {
        Movie.removeTimeRangeListener(this);
    }

    @Override
    public void timeRangeChanged(long start, long end) {
        locationOptionPanel.setTimespan(start, end);
        equatorialOptionPanel.setTimespan(start, end);
    }

    boolean isHeliospheric() {
        return cameraMode == CameraMode.Heliosphere;
    }

    @Nullable
    PositionLoad getHighlightedLoad() {
        return isHeliospheric() ? equatorialOptionPanel.getHighlightedLoad() : null;
    }

    int getSpiralSpeed() {
        return isHeliospheric() ? equatorialOptionPanel.getSpiralSpeed() : 0;
    }

    boolean isRelative() {
        return isHeliospheric() && equatorialOptionPanel.isRelative();
    }

}
