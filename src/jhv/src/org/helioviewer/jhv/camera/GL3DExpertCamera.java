package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

public class GL3DExpertCamera extends GL3DCamera {

    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Sun.MeanEarthDistance;

    private JHVDate interpolate(JHVDate date) {
        GL3DPositionLoading positionLoading = expertOptionPanel.getPositionLoading();
        if (positionLoading.isLoaded()) {
            long currentCameraTime, dateTime = date.getTime();
            long tLayerStart = 0, tLayerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                tLayerStart = Layers.getStartDate(view).getTime();
                tLayerEnd = Layers.getEndDate(view).getTime();
            }

            //Camera times
            long tPositionStart = positionLoading.getStartTime();
            long tPositionEnd = positionLoading.getEndTime();

            if (tLayerEnd != tLayerStart) {
                currentCameraTime = (long) (tPositionStart + (tPositionEnd - tPositionStart) * (dateTime - tLayerStart) / (double) (tLayerEnd - tLayerStart));
            } else {
                currentCameraTime = tPositionEnd;
            }

            Position.Latitudinal p = positionLoading.getInterpolatedPosition(currentCameraTime);
            if (p != null) {
                date = new JHVDate(p.milli);
                currentDistance = p.rad;
                currentL = p.lon;
                currentB = p.lat;
            }
        } else {
            Position.Latitudinal p = Sun.getEarth(date.getTime());
            currentDistance = p.rad;
            currentL = 0;
            currentB = p.lat;
        }

        return date;
    }

    @Override
    public void updateRotation(JHVDate date) {
        cameraTime = interpolate(date);
        Position.Latitudinal p = Sun.getEarth(cameraTime.getTime());

        localRotation = new Quatd(currentB, -currentL + p.lon);
        distance = currentDistance;
        updateCameraTransformation();
    }

}
