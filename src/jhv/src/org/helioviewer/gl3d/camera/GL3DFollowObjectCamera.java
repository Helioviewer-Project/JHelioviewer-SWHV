package org.helioviewer.gl3d.camera;

import java.util.ArrayList;
import java.util.Date;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;

public class GL3DFollowObjectCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener, GL3DPositionLoadingListener {

    private final CoordinateVector startPosition = null;

    private Date currentDate = null;
    private double currentRotation = 0.0;
    private long timediff;
    private final ArrayList<GL3DFollowObjectCameraListener> followObjectCameraListeners = new ArrayList<GL3DFollowObjectCameraListener>();

    private final GL3DPositionLoading positionLoading;

    public GL3DFollowObjectCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
        positionLoading = new GL3DPositionLoading();
        positionLoading.addListener(this);
    }

    @Override
    public void reset() {
        this.resetCurrentDragRotation();
        super.reset();
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        sceneGraphView.addViewListener(this);
    }

    @Override
    public void deactivate() {
        sceneGraphView.removeViewListener(this);
    };

    @Override
    public String getName() {
        return "Follow Object Camera";
    }

    int i = 0;

    private double currentL = 0.;

    private double currentB = 0.;

    private double currentDistance = Constants.SunRadius;

    private long currentCameraTime;

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (this.positionLoading.isLoaded()) {
            TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
            if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
                currentDate = timestampReason.getNewDateTime().getTime();
                //Layer times
                long t1 = LayersModel.getSingletonInstance().getFirstDate().getTime();
                long t2 = LayersModel.getSingletonInstance().getLastDate().getTime();
                //Camera times
                long t3 = this.positionLoading.getBeginDate().getTime();
                long t4 = this.positionLoading.getEndDate().getTime();
                int i;
                //Linear interpolation
                if (t4 != t3) {
                    currentCameraTime = (long) ((t3 + 1. * (t4 - t3) * (timestampReason.getNewDateTime().getMillis() - t1) / (t2 - t1)));
                    i = (int) (1. * (currentCameraTime - t3) / (t4 - t3) * this.positionLoading.positionDateTime.length);

                } else {
                    currentCameraTime = t4;
                    i = 0;
                }
                this.fireCameratTime(new Date(currentCameraTime));
                i = Math.min(i, this.positionLoading.positionDateTime.length);
                i = Math.max(i, 0);
                try {
                    currentL = this.positionLoading.positionDateTime[i].getPosition().y;
                    currentB = this.positionLoading.positionDateTime[i].getPosition().z;
                    currentDistance = 1000 * (this.positionLoading.positionDateTime[i].getPosition().x) / Constants.SunRadiusInMeter / 7;
                } catch (Exception e) {
                }
                updateRotation();
            }

        }
    }

    private void fireCameratTime(Date currentCameraTime) {
        synchronized (followObjectCameraListeners) {
            for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
                listener.fireCameraTime(currentCameraTime);
            }
        }
    }

    public void updateRotation() {
        if (this.positionLoading.isLoaded() && currentDate != null) {

            this.timediff = (currentCameraTime) / 1000 - Constants.referenceDate;
            this.currentRotation = (-currentL + DifferentialRotation.calculateRotationInRadians(0., this.timediff)) % (Math.PI * 2.0);
            GL3DQuatd newRotation = GL3DQuatd.createRotation(0., new GL3DVec3d(0, 1, 0));
            newRotation.rotate(GL3DQuatd.createRotation(currentB, new GL3DVec3d(1, 0, 0)));
            newRotation.rotate(GL3DQuatd.createRotation(this.currentRotation, new GL3DVec3d(0, 1, 0)));

            this.setLocalRotation(newRotation);
            this.setZTranslation(-currentDistance);
            this.updateCameraTransformation();
        }
    }

    public void addFollowObjectCameraListener(GL3DFollowObjectCameraListener listener) {
        synchronized (followObjectCameraListeners) {
            this.followObjectCameraListeners.add(listener);
        }
    }

    public void removeFollowObjectCameraListener(GL3DFollowObjectCameraListener listener) {
        synchronized (followObjectCameraListeners) {
            this.followObjectCameraListeners.remove(listener);
        }
    }

    @Override
    public void fireNewLoaded(boolean isLoaded) {
        synchronized (followObjectCameraListeners) {
            for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
                listener.fireLoaded(isLoaded);
            }
        }
    }

    public void setBeginDate(Date date) {
        this.positionLoading.setBeginDate(date);
    }

    public void setEndDate(Date date) {
        this.positionLoading.setEndDate(date);
    }

    public void setObservingObject(String object) {
        this.positionLoading.setObservingObject(object);
    }

}
