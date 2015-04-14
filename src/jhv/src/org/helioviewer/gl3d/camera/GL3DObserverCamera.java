package org.helioviewer.gl3d.camera;

import java.util.Date;

import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DObserverCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DObserverCamera extends GL3DSolarRotationTrackingTrackballCamera implements TimeListener {

    public GL3DObserverCamera() {
        super();
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        Displayer.addTimeListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        Displayer.removeTimeListener(this);
    }

    @Override
    public String getName() {
        return "View from Observer";
    }

    @Override
    public void timeChanged(Date date) {
        if (!this.getTrackingMode()) {
            updateRotation(date);
        }
    }

    private void updateRotation(Date date) {
        double addl0 = 0.;
        double addb0 = 0.;

        MetaDataView mdv = Displayer.getLayersModel().getActiveView();
        if (mdv != null) {
            MetaData metadata = mdv.getMetaData();
            if (metadata instanceof HelioviewerMetaData) {
                HelioviewerMetaData hvMetadata = (HelioviewerMetaData) metadata;
                if (!hvMetadata.isStonyhurstProvided()) {
                    addb0 = Astronomy.getB0InRadians(date);
                } else {
                    addl0 = hvMetadata.getStonyhurstLongitude() / MathUtils.radeg;
                    addb0 = hvMetadata.getStonyhurstLatitude() / MathUtils.radeg;
                }
                this.setZTranslation(-hvMetadata.getDobs() / Constants.SunRadiusInMeter);
            } else {
                addb0 = Astronomy.getB0InRadians(date);
            }
        }

        double currentRotation = Astronomy.getL0Radians(date);

        this.localRotation = GL3DQuatd.createRotation(addb0, GL3DVec3d.XAxis);
        this.localRotation.rotate(GL3DQuatd.createRotation(currentRotation - addl0, GL3DVec3d.YAxis));

        this.updateCameraTransformation();
        this.setTime(date.getTime());
    }

}
