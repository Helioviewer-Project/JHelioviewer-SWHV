package org.helioviewer.gl3d.camera;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DTrackballStonyhurstCamera} by automatically rotating the camera
 * around the Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DTrackballStonyhurstCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener {

    private final CoordinateVector startPosition = null;

    private Date currentDate = null;
    private double currentRotation = 0.0;

    public GL3DTrackballStonyhurstCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        getSceneGraphView().addViewListener(this);
    }

    @Override
    public void deactivate() {
        getSceneGraphView().removeViewListener(this);
    };

    @Override
    public String getName() {
        return "Stonyhurst Camera";
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
            currentDate = timestampReason.getNewDateTime().getTime();
            updateRotation();
        }
    }

    public void updateRotation() {
        this.currentRotation = Astronomy.getL0Radians(currentDate);//DifferentialRotation.calculateRotationInRadians(0., this.timediff) % (Math.PI * 2.0);
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date(currentDate.getTime()));
        this.getLocalRotation().clear();
        this.getLocalRotation().rotate(GL3DQuatd.createRotation(this.currentRotation, GL3DVec3d.YAxis));
        this.updateCameraTransformation();
    }

    private void resetStartPosition() {
    }

}
