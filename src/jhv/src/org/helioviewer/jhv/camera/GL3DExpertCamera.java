package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

public class GL3DExpertCamera extends GL3DCamera implements LayersListener {

    private final GL3DExpertCameraOptionPanel expertCameraOptionPanel;
    private final GL3DPositionLoading positionLoading;
    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Sun.MeanEarthDistance;

    private boolean interpolation = false;

    public GL3DExpertCamera() {
        super();
        expertCameraOptionPanel = new GL3DExpertCameraOptionPanel(this);
        positionLoading = new GL3DPositionLoading(this);
        this.timeChanged(Layers.getLastUpdatedTimestamp());
        expertCameraOptionPanel.syncWithLayerBeginTime(false);
        expertCameraOptionPanel.syncWithLayerEndTime(true);
    }

    @Override
    public void reset() {
        super.reset();
        updateRotation(Layers.getLastUpdatedTimestamp(), null);
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        this.timeChanged(Layers.getLastUpdatedTimestamp());
        Layers.addLayersListener(this);
    }

    @Override
    public void deactivate() {
        Layers.removeLayersListener(this);
        super.deactivate();
    }

    @Override
    public String getName() {
        return "Expert camera";
    }

    @Override
    public void timeChanged(JHVDate date) {
        if (!this.getTrackingMode()) {
            updateRotation(date, null);
        } else {
            Displayer.render();
        }
    }

    private JHVDate forceTimeChanged(JHVDate date) {
        if (positionLoading.isLoaded()) {
            long currentCameraTime, dateTime = date.getTime();
            if (interpolation) {
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
            } else {
                currentCameraTime = dateTime;
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
    public void updateRotation(JHVDate date, MetaData m) {
        JHVDate ndate = forceTimeChanged(date);
        Position.Latitudinal p = Sun.getEarth(ndate.getTime());

        double b = currentB;
        double l = -currentL + p.lon;
        double d = currentDistance;

        localRotation = new Quatd(b, l);
        setZTranslation(-d);
        updateCameraTransformation();
    }

    public void fireNewLoaded(String state) {
        expertCameraOptionPanel.fireLoaded(state);
        updateRotation(Layers.getLastUpdatedTimestamp(), null);
        Displayer.render();
    }

    public void setBeginDate(Date date, boolean applyChanges) {
        positionLoading.setBeginDate(date, applyChanges);
        Displayer.render();
    }

    public void setEndDate(Date date, boolean applyChanges) {
        positionLoading.setEndDate(date, applyChanges);
        Displayer.render();
    }

    public void setObservingObject(String object, boolean applyChanges) {
        positionLoading.setObserver(object, applyChanges);
        Displayer.render();
    }

    public void setInterpolation(boolean _interpolation) {
        interpolation = _interpolation;
        if (!interpolation) {
            activeLayerChanged(Layers.getActiveView());
        }
    }

    @Override
    public void layerAdded(View view) {
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view != null && !interpolation) {
            positionLoading.setBeginDate(Layers.getStartDate(view).getDate(), false);
            positionLoading.setEndDate(Layers.getEndDate(view).getDate(), true);
            Displayer.render();
        }
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return expertCameraOptionPanel;
    }

}
