package org.helioviewer.gl3d.camera;

import java.util.Date;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class GL3DFollowObjectCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener, GL3DPositionLoadingListener {

    private final CoordinateVector startPosition = null;

    private Date currentDate = null;
    private double currentRotation = 0.0;
    private long timediff;

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

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (this.positionLoading.isLoaded()) {
            TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
            if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
                JHVJPXView jpx = timestampReason.getView().getAdapter(JHVJPXView.class);
                i = jpx.getCurrentFrameNumber() % 935;
                currentDate = new Date(this.positionLoading.positionDateTime[i].getTimestamp());
                currentL = this.positionLoading.positionDateTime[i].getPosition().y;
                currentB = this.positionLoading.positionDateTime[i].getPosition().z;
                currentDistance = 1000 * (this.positionLoading.positionDateTime[i].getPosition().x) / Constants.SunRadiusInMeter / 7;
                i = (i + 1) % 48;
                updateRotation();
            }
        }
    }

    public void updateRotation() {
        this.timediff = currentDate.getTime() - Constants.referenceDate;

        this.currentRotation = (currentL + DifferentialRotation.calculateRotationInRadians(0., this.timediff)) % (Math.PI * 2.0);
        GL3DQuatd newRotation = GL3DQuatd.createRotation(this.currentRotation, new GL3DVec3d(0, 1, 0));
        newRotation.rotate(GL3DQuatd.createRotation(currentB, new GL3DVec3d(1, 0, 0)));
        this.setLocalRotation(newRotation);
        this.setZTranslation(-currentDistance);
        System.out.println(this.getZTranslation());
        this.updateCameraTransformation();
    }

    private void resetStartPosition() {
    }

    @Override
    public void fireNewLoaded() {

    }
}
