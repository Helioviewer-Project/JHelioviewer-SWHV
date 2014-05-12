package org.helioviewer.gl3d.camera;

import java.util.Date;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SolarSphereToStonyhurstHeliographicConversion;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.StonyhurstHeliographicCoordinateSystem;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DTrackballCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DTrackballCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener {

    private CoordinateVector startPosition = null;

    private final Date currentDate = null;
    private double currentRotation = 0.0;

    private final StonyhurstHeliographicCoordinateSystem stonyhurstCoordinateSystem = new StonyhurstHeliographicCoordinateSystem();
    private final SolarSphereCoordinateSystem solarSphereCoordinateSystem = new SolarSphereCoordinateSystem();
    private final SolarSphereToStonyhurstHeliographicConversion stonyhurstConversion = (SolarSphereToStonyhurstHeliographicConversion) solarSphereCoordinateSystem.getConversion(stonyhurstCoordinateSystem);


    public GL3DTrackballCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
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
        return "Solar Rotation Tracking Camera";
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
/*        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
            currentDate = timestampReason.getNewDateTime().getTime();
            if (startPosition != null) {
                long timediff = (currentDate.getTime()) / 1000 - Constants.referenceDate;

                setDifferentialRotation(-DifferentialRotation.calculateRotationInRadians(0., timediff) % (Math.PI * 2.0));

                this.getRotation().rotate(GL3DQuatd.createRotation(currentRotation - getDifferentialRotation(), new GL3DVec3d(0, 1, 0)));
                this.updateCameraTransformation();
                this.currentRotation = getDifferentialRotation();
            } else {
                currentRotation = 0.0;
                resetStartPosition();
            }

        }*/
    }

    @Override
    public void updateRotation(long currentTime){
        if (startPosition != null) {
            long timediff = currentTime/1000 - Constants.referenceDate;

            setDifferentialRotation(-DifferentialRotation.calculateRotationInRadians(0., timediff) % (Math.PI * 2.0));

            this.getRotation().rotate(GL3DQuatd.createRotation(currentRotation - getDifferentialRotation(), new GL3DVec3d(0, 1, 0)));
            this.updateCameraTransformation();
            this.currentRotation = getDifferentialRotation();
        } else {
            currentRotation = 0.0;
            resetStartPosition();
        }
    }

    private void resetStartPosition() {
        GL3DRayTracer positionTracer = new GL3DRayTracer(sceneGraphView.getHitReferenceShape(), this);
        GL3DRay positionRay = positionTracer.castCenter();

        GL3DVec3d position = positionRay.getHitPoint();

        if (position != null) {
            CoordinateVector solarSpherePosition = solarSphereCoordinateSystem.createCoordinateVector(position.x, position.y, position.z);
            CoordinateVector stonyhurstPosition = stonyhurstConversion.convert(solarSpherePosition);
            // Log.debug("GL3DSolarRotationTrackingCam: StonyhurstPosition="+stonyhurstPosition);
            this.startPosition = stonyhurstPosition;

            Log.debug("GL3DSolarRotationTracking.Set Start hitpoint! " + positionRay.getDirection());
        } else {
            Log.debug("GL3DSolarRotationTracking.cannot reset hitpoint! " + positionRay.getDirection());

        }

    }

    private Date getStartDate() {
        if (LinkedMovieManager.getActiveInstance() == null) {
            return null;
        }
        TimedMovieView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();
        if (masterView == null) {
            return null;
        }
        ImmutableDateTime idt = masterView.getCurrentFrameDateTime();
        if (idt == null) {
            return null;
        }
        return idt.getTime();
    }
}
