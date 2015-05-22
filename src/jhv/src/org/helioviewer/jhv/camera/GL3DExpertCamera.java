package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.physics.Astronomy;
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
    private double currentDistance = Sun.MeanEarthDistance / Sun.Radius;

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
        forceTimeChanged(cameraDate);
        super.reset();
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
        if (date != null && this.positionLoading.isLoaded()) {

            long currentCameraTime, dateTime = date.getTime();
            if (interpolation) {
                long t1 = 0, t2 = 0;
                // Active layer times
                AbstractView view = LayersModel.getActiveView();
                if (view instanceof JHVJPXView) {
                    t1 = LayersModel.getStartDate(view).getTime();
                    t2 = LayersModel.getEndDate(view).getTime();
                }
                //Camera times
                long t3 = this.positionLoading.getBeginDate().getTime();
                long t4 = this.positionLoading.getEndDate().getTime();

                if (t2 != t1) {
                    currentCameraTime = (long) (t3 + (t4 - t3) * (dateTime - t1) / (double) (t2 - t1));
                } else {
                    currentCameraTime = t4;
                }
            } else {
                currentCameraTime = dateTime;
            }

            cameraDate = new Date(currentCameraTime);

            RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
            if (renderableCamera != null) {
                renderableCamera.setTimeString(cameraDate);
                ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
            }

            GL3DVec3d position = this.positionLoading.getInterpolatedPosition(currentCameraTime);
            if (position != null) {
                currentL = position.y;
                currentB = position.z;
                currentDistance = position.x;

                updateRotation(date);
            }
        } else if (date != null) {
            currentL = 0;
            currentB = Astronomy.getB0Radians(date);
            currentDistance = Astronomy.getDistanceSolarRadii(date);
            updateRotation(date);
        }
    }

    private void updateRotation(Date date) {
        double b = currentB;
        double l = (-currentL + Astronomy.getL0Radians(date)) % (Math.PI * 2.0);
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
