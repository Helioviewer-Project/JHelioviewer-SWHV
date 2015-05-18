package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.renderable.RenderableCamera;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class GL3DFollowObjectCamera extends GL3DSolarRotationTrackingTrackballCamera implements GL3DPositionLoadingListener, LayersListener, TimeListener {
    private final GL3DFollowObjectCameraOptionPanel followObjectCameraOptionPanel;
    private final GL3DPositionLoading positionLoading;
    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Constants.SunMeanDistanceToEarth / Constants.SunRadius;

    private Date cameraDate;
    private boolean interpolation;

    public GL3DFollowObjectCamera() {
        super();
        positionLoading = new GL3DPositionLoading();
        LayersModel.addLayersListener(this);
        this.timeChanged(Displayer.getLastUpdatedTimestamp());
        followObjectCameraOptionPanel = new GL3DFollowObjectCameraOptionPanel(this);
        positionLoading.addListener(this);
    }

    @Override
    public void reset() {
        forceTimeChanged(cameraDate);
        super.reset();
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        this.activeLayerChanged(LayersModel.getActiveView());
        this.timeChanged(Displayer.getLastUpdatedTimestamp());
        Displayer.addFirstTimeListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        Displayer.removeTimeListener(this);
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
                currentB = -position.z;
                currentDistance = position.x;

                updateRotation(date);
            }
        }
    }

    private void updateRotation(Date date) {
        if (this.positionLoading.isLoaded()) {
            double currentRotation = (-currentL + Astronomy.getL0Radians(date)) % (Math.PI * 2.0);

            GL3DQuatd newRotation = GL3DQuatd.createRotation(-currentB, GL3DVec3d.XAxis);
            newRotation.rotate(GL3DQuatd.createRotation(currentRotation, GL3DVec3d.YAxis));

            this.setLocalRotation(newRotation);
            this.setZTranslation(-currentDistance);
            this.updateCameraTransformation();
        }
    }

    @Override
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
