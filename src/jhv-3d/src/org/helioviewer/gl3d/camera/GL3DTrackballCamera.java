package org.helioviewer.gl3d.camera;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.camera.GL3DCamera;
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
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DBaseTrackballCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTrackballCamera extends GL3DBaseTrackballCamera implements ViewListener {
	
	private final Date startDate;

    private CoordinateVector startPosition = null;

    private Date currentDate = null;
    private double currentRotation = 0.0;

    private StonyhurstHeliographicCoordinateSystem stonyhurstCoordinateSystem = new StonyhurstHeliographicCoordinateSystem();
    private SolarSphereCoordinateSystem solarSphereCoordinateSystem = new SolarSphereCoordinateSystem();
    private SolarSphereToStonyhurstHeliographicConversion stonyhurstConversion = (SolarSphereToStonyhurstHeliographicConversion) solarSphereCoordinateSystem.getConversion(stonyhurstCoordinateSystem);

	private Date previousDate;

	private GL3DQuatd baseRot;




    public GL3DTrackballCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
        Calendar cal = new GregorianCalendar();
        cal.set(2000, 1, 1, 0, 0, 0);
        startDate = cal.getTime();
    }

    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        sceneGraphView.addViewListener(this);
    }

    public void deactivate() {
        sceneGraphView.removeViewListener(this);
        this.startPosition = null;
    };

    public String getName() {
        return "Solar Rotation Tracking Camera";
    }
    

    public void viewChanged(View sender, ChangeEvent aEvent) {
        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
            currentDate = timestampReason.getNewDateTime().getTime();
            
            if (startPosition != null) {
                long timediff = (currentDate.getTime() - startDate.getTime()) / 1000;
                Calendar cal = new GregorianCalendar();
                cal.setTime(startDate);
                double b02000 = Astronomy.getB0InRadians(cal);
                cal.setTime(currentDate);
                double b0 = Astronomy.getB0InRadians(cal); 
                localrotation = DifferentialRotation.calculateRotationInRadians(0, timediff);
                rotateAll();

            } else {
                Calendar cal = new GregorianCalendar();
                cal.setTime(startDate);
                baseRot = this.getRotation().copy();
                resetStartPosition();
            }

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
   
}
