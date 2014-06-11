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

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DSolarOrbiterCamera} by automatically rotating the camera around
 * the Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSolarOrbiterCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener {

    private final CoordinateVector startPosition = null;

    private Date currentDate = null;
    private double currentRotation = 0.0;
    private long timediff;

    private final GL3DPositionLoading positionLoading;

    public GL3DSolarOrbiterCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
        positionLoading = new GL3DPositionLoading();
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
        return "Solar orbiter Camera";
    }

    int i = 0;

    private double currentL = 0.;

    private double currentB = 0.;

    private double currentDistance = Constants.SunRadius;

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
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
}
