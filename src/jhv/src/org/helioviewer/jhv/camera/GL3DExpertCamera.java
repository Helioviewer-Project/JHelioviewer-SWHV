package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class GL3DExpertCamera extends GL3DCamera implements LayersListener {

    private final GL3DExpertCameraOptionPanel followObjectCameraOptionPanel;
    private final GL3DPositionLoading positionLoading;
    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Sun.MeanEarthDistance;

    private Date cameraDate;
    private boolean interpolation;

    public GL3DExpertCamera() {
        super();
        followObjectCameraOptionPanel = new GL3DExpertCameraOptionPanel(this);
        positionLoading = new GL3DPositionLoading(this);
        this.timeChanged(Displayer.getLastUpdatedTimestamp());
        followObjectCameraOptionPanel.syncWithLayerBeginTime(false);
        followObjectCameraOptionPanel.syncWithLayerEndTime(true);
    }

    @Override
    public void reset() {
        super.reset();
        forceTimeChanged(cameraDate);
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        this.timeChanged(Displayer.getLastUpdatedTimestamp());
        LayersModel.addLayersListener(this);
    }

    @Override
    public void deactivate() {
        LayersModel.removeLayersListener(this);
        super.deactivate();
    }

    @Override
    public String getName() {
        return "Follow object camera";
    }

    @Override
    public void timeChanged(Date date) {
        if (!this.getTrackingMode()) {
            forceTimeChanged(date);
        }
    }

    public void forceTimeChanged(Date date) {
        if (date == null)
            return;

        if (positionLoading.isLoaded()) {
            long currentCameraTime, dateTime = date.getTime();
            if (interpolation) {
                long tLayerStart = 0, tLayerEnd = 0;
                // Active layer times
                AbstractView view = LayersModel.getActiveView();
                if (view instanceof JHVJPXView) {
                    tLayerStart = LayersModel.getStartDate(view).getTime();
                    tLayerEnd = LayersModel.getEndDate(view).getTime();
                }
                //Camera times
                long tPositionStart = this.positionLoading.getStartTime();
                long tPositionEnd = this.positionLoading.getEndTime();

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
                cameraDate = date = new Date(p.milli);
                currentDistance = p.rad;
                currentL = p.lon;
                currentB = p.lat;
            }
        } else {
            Position.Latitudinal p = Sun.getRBL(date);
            currentDistance = p.rad;
            currentL = 0;
            currentB = p.lat;
        }

        updateRotation(date);

        RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
        if (renderableCamera != null) {
            renderableCamera.setTimeString(date);
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
        }
    }

    private void updateRotation(Date date) {
        Position.Latitudinal p = Sun.getRBL(date);

        double b = currentB;
        double l = -currentL + p.lon;
        double d = currentDistance;

        this.localRotation = new GL3DQuatd(b, l);
        this.setZTranslation(-d);

        this.updateCameraTransformation();
    }

    public void fireNewLoaded(String state) {
        followObjectCameraOptionPanel.fireLoaded(state);
        Displayer.render();
    }

    public void setBeginDate(Date date, boolean applyChanges) {
        this.positionLoading.setBeginDate(date, applyChanges);
    }

    public void setEndDate(Date date, boolean applyChanges) {
        this.positionLoading.setEndDate(date, applyChanges);
    }

    public void setObservingObject(String object, boolean applyChanges) {
        this.positionLoading.setObserver(object, applyChanges);
    }

    public void setInterpolation(boolean interpolation) {
        this.interpolation = interpolation;
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
        if (!interpolation && view instanceof JHVJPXView) {
            positionLoading.setBeginDate(LayersModel.getStartDate(view), false);
            positionLoading.setEndDate(LayersModel.getEndDate(view), true);
        }
    }

    public Date getBeginTime() {
        return this.positionLoading.getBeginDate();
    }

    public Date getEndTime() {
        return this.positionLoading.getEndDate();
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return followObjectCameraOptionPanel;
    }

}
