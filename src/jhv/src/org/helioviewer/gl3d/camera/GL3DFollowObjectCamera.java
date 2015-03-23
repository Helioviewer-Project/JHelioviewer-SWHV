package org.helioviewer.gl3d.camera;

import java.util.ArrayList;
import java.util.Date;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class GL3DFollowObjectCamera extends GL3DSolarRotationTrackingTrackballCamera implements GL3DPositionLoadingListener, LayersListener, TimeListener {

    private final ArrayList<GL3DFollowObjectCameraListener> followObjectCameraListeners = new ArrayList<GL3DFollowObjectCameraListener>();
    private final GL3DPositionLoading positionLoading;
    private double FOVangle;
    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Constants.SunMeanDistanceToEarth / Constants.SunRadius;

    private Date cameraDate;
    private boolean interpolation;
    private boolean fovhidden = false;

    protected GL3DCameraFOV cameraFOVDraw;

    public GL3DFollowObjectCamera() {
        super();
        positionLoading = new GL3DPositionLoading();
        positionLoading.addListener(this);
        this.cameraFOVDraw = new GL3DCameraFOV(1., 1.);
        this.cameraFOVDraw.getDrawBits().set(Bit.Hidden, fovhidden);
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
    }

    @Override
    public void activate() {
        super.activate();
        this.cameraFOVDraw.getDrawBits().set(Bit.Hidden, this.fovhidden);
        Displayer.addTimeListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.fovhidden = this.cameraFOVDraw.getDrawBits().get(Bit.Hidden);
        this.cameraFOVDraw.getDrawBits().on(Bit.Hidden);
        this.cameraFOVDraw.getDrawBits().set(Bit.Hidden, true);
        Displayer.removeTimeListener(this);
    }

    public void createNewFOV() {
        boolean hidden = this.cameraFOVDraw.getDrawBits().get(Bit.Hidden);
        GL3DCameraFOV newFOVDraw = new GL3DCameraFOV(1., 1.);
        newFOVDraw.getDrawBits().set(Bit.Hidden, hidden);
        this.cameraFOVDraw = newFOVDraw;
    }

    @Override
    public String getName() {
        return "Follow Object Camera";
    }

    @Override
    public void timeChanged(Date date) {
        if (this.positionLoading.isLoaded() && !this.getTrackingMode()) {
            //Layer times
            long t1 = LayersModel.getSingletonInstance().getFirstDate().getTime();
            long t2 = LayersModel.getSingletonInstance().getLastDate().getTime();
            //Camera times
            long t3 = this.positionLoading.getBeginDate().getTime();
            long t4 = this.positionLoading.getEndDate().getTime();

            long currentCameraTime, dateTime = date.getTime();
            if (interpolation) {
                if (t4 != t3) {
                    currentCameraTime = (long) ((t3 + 1. * (t4 - t3) * (dateTime - t1) / (t2 - t1)));
                } else {
                    currentCameraTime = t4;
                }
            } else {
                currentCameraTime = dateTime;
            }

            cameraDate = new Date(currentCameraTime);
            for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
                listener.fireCameraTime(cameraDate);
            }

            this.setTime(currentCameraTime);

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

            this.cameraFOVDraw.scale(currentDistance * Math.tan(FOVangle));
            this.cameraFOVDraw.setAngles(currentB, currentRotation);
        }
    }

    public void addFollowObjectCameraListener(GL3DFollowObjectCameraListener listener) {
        this.followObjectCameraListeners.add(listener);
    }

    public void removeFollowObjectCameraListener(GL3DFollowObjectCameraListener listener) {
        this.followObjectCameraListeners.remove(listener);
    }

    @Override
    public void fireNewLoaded(String state) {
        for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
            listener.fireLoaded(state);
        }
    }

    @Override
    public void fireNewDate() {
        for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
            listener.fireNewDate(cameraDate);
        }
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

    public void setFOVangleDegrees(double fovAngle) {
        this.FOVangle = fovAngle * Math.PI / 180.0;
    }

    public void setInterpolation(boolean interpolation) {
        this.interpolation = interpolation;
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(final int idx) {
        if (!interpolation) {
            View nextView = LayersModel.getSingletonInstance().getLayer(idx);
            if (nextView != null) {

                JHVJPXView jpxView = nextView.getAdapter(JHVJPXView.class);
                if (jpxView != null) {
                    Date beginDate = null;
                    Date endDate = null;
                    for (int frame = 0; frame <= jpxView.getMaximumFrameNumber(); frame++) {
                        ImmutableDateTime date = jpxView.getFrameDateTime(frame);
                        if (beginDate == null || date.getTime().getTime() < beginDate.getTime()) {
                            beginDate = date.getTime();
                        }
                        if (endDate == null || date.getTime().getTime() > endDate.getTime()) {
                            endDate = date.getTime();
                        }
                    }
                    positionLoading.setBeginDate(beginDate, false);
                    positionLoading.setEndDate(endDate, true);
                }
            }
        }
    }

    public Date getBeginTime() {
        return this.positionLoading.getBeginDate();
    }

    public Date getEndTime() {
        return this.positionLoading.getEndDate();
    }

}
