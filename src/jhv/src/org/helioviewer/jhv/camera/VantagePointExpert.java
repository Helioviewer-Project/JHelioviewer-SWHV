package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

public class VantagePointExpert extends VantagePoint {

    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Sun.MeanEarthDistance;
    private GL3DPositionLoading positionLoading = new GL3DPositionLoading(this);

    private JHVDate interpolate(JHVDate date) {
        if (positionLoading.isLoaded()) {
            long currentCameraTime, dateTime = date.getTime();
            long tLayerStart = 0, tLayerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                tLayerStart = Layers.getStartDate(view).getTime();
                tLayerEnd = Layers.getEndDate(view).getTime();
            }

            // camera times
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
    public void update(JHVDate date) {
        time = interpolate(date);

        Position.Latitudinal p = Sun.getEarth(time.getTime());
        orientation = new Quatd(currentB, -currentL + p.lon);
        distance = currentDistance;
    }

    public void fireLoaded(final String state) {
        // optionPanel.fireLoaded(state);
    }

}
