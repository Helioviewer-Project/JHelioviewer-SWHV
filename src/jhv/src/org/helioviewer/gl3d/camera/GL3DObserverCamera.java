package org.helioviewer.gl3d.camera;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.metadata.HelioviewerPositionedMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DObserverCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DObserverCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener {

    private Date currentDate = null;
    private double currentRotation = 0.0;

    private long timediff;

    public GL3DObserverCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
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
        return "View from observer";
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

        double addl0 = 0.;
        double addb0 = 0.;
        MetaDataView mdv = LayersModel.getSingletonInstance().getActiveView().getAdapter(MetaDataView.class);
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date(currentDate.getTime()));
        if (mdv != null) {
            MetaData metadata = mdv.getMetaData();
            if (metadata instanceof HelioviewerPositionedMetaData) {
                HelioviewerPositionedMetaData hvMetadata = (HelioviewerPositionedMetaData) metadata;
                if (!hvMetadata.isStonyhurstProvided()) {
                    addb0 = Astronomy.getB0InRadians(cal);
                } else {
                    addl0 = hvMetadata.getStonyhurstLongitude() / MathUtils.radeg;
                    addb0 = -hvMetadata.getStonyhurstLatitude() / MathUtils.radeg;
                }

            } else {
                addb0 = Astronomy.getB0InRadians(cal);
            }
        }

        this.timediff = (currentDate.getTime()) / 1000 - Constants.referenceDate;
        this.currentRotation = DifferentialRotation.calculateRotationInRadians(0., this.timediff) % (Math.PI * 2.0);

        this.getLocalRotation().clear();
        this.getLocalRotation().rotate(GL3DQuatd.createRotation(-addb0, new GL3DVec3d(1, 0, 0)));

        this.getLocalRotation().rotate(GL3DQuatd.createRotation(this.currentRotation - addl0, new GL3DVec3d(0, 1, 0)));
        this.updateCameraTransformation();
    }
}
