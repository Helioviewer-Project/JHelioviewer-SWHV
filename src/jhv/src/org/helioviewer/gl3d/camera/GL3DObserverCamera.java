package org.helioviewer.gl3d.camera;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec3d;
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
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

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

    public GL3DObserverCamera(GL3DSceneGraphView sceneGraphView) {
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
        super.deactivate();
        getSceneGraphView().removeViewListener(this);
    };

    @Override
    public String getName() {
        return "View from Observer";
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (!this.getTrackingMode()) {
            TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
            if (timestampReason != null && LayersModel.getSingletonInstance().getActiveView() != null) {
                boolean isjp2 = LayersModel.getSingletonInstance().getActiveView().getAdapter(JHVJP2View.class).getClass() == JHVJP2View.class;
                if (isjp2 || ((timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView()))) {
                    currentDate = timestampReason.getNewDateTime().getTime();
                    updateRotation();
                }
            }
        }
    }

    public void updateRotation() {
        double addl0 = 0.;
        double addb0 = 0.;
        if (LayersModel.getSingletonInstance().getActiveView() != null) {
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
                        addb0 = hvMetadata.getStonyhurstLatitude() / MathUtils.radeg;
                    }
                    this.setZTranslation(-hvMetadata.getDobs() / Constants.SunRadiusInMeter);
                } else {
                    addb0 = Astronomy.getB0InRadians(cal);
                }
            }
            this.setTime(currentDate.getTime());
            this.currentRotation = Astronomy.getL0Radians(currentDate);

            this.localRotation = GL3DQuatd.createRotation(addb0, GL3DVec3d.XAxis);
            this.localRotation.rotate(GL3DQuatd.createRotation(this.currentRotation - addl0, GL3DVec3d.YAxis));

            this.updateCameraTransformation();
        }
    }

}
