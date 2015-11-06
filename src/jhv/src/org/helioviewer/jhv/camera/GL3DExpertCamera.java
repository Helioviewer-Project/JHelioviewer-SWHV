package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.jhv.viewmodel.view.View;

public class GL3DExpertCamera extends GL3DCamera {

    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Sun.MeanEarthDistance;

    @Override
    public void timeChanged(JHVDate date) {
        if (!this.getTrackingMode()) {
            updateRotation(date);
        } else {
            Displayer.render();
        }
    }

    private JHVDate forceTimeChanged(JHVDate date) {
        GL3DPositionLoading positionLoading = optionPanel.getPositionLoading();
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

        RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
        if (renderableCamera != null) {
            renderableCamera.setTimeString(date.toString());
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
        }
        return date;
    }

    @Override
    public void updateRotation(JHVDate date) {
        JHVDate ndate = forceTimeChanged(date);
        Position.Latitudinal p = Sun.getEarth(ndate.getTime());

        double b = currentB;
        double l = -currentL + p.lon;
        double d = currentDistance;

        localRotation = new Quatd(b, l);
        setZTranslation(-d);
        updateCameraTransformation();
    }

    private GL3DExpertCameraOptionPanel optionPanel;

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        if (optionPanel == null) {
            optionPanel = new GL3DExpertCameraOptionPanel(this);
            optionPanel.syncWithLayerBeginTime(false);
            optionPanel.syncWithLayerEndTime(true);
        }
        return optionPanel;
    }

}
