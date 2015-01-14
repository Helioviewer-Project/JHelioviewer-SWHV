package org.helioviewer.gl3d.camera;

import java.util.Date;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DEarthCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DEarthCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener {

    private Date currentDate = null;
    private double currentRotation = 0.0;

    public GL3DEarthCamera(GL3DSceneGraphView sceneGraphView) {
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
        getSceneGraphView().addViewListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        getSceneGraphView().removeViewListener(this);

    };

    @Override
    public String getName() {
        return "View from earth";
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (!this.getTrackingMode()) {
            TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
            if (timestampReason != null && LayersModel.getSingletonInstance().getActiveView() != null) {
                boolean isjp2 = LayersModel.getSingletonInstance().getActiveView().getAdapter(JHVJP2View.class).getClass() == JHVJP2View.class;
                if (isjp2 || ((timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView()))) {
                    currentDate = timestampReason.getNewDateTime().getTime();
                    this.setTime(currentDate.getTime());
                    updateRotation();
                }
            }
        }
    }

    public void updateRotation() {
        double b0 = Astronomy.getB0InRadians(currentDate);
        this.currentRotation = Astronomy.getL0Radians(currentDate);
        this.getLocalRotation().clear();
        this.getLocalRotation().rotate(GL3DQuatd.createRotation(b0, new GL3DVec3d(1, 0, 0)));
        this.getLocalRotation().rotate(GL3DQuatd.createRotation(this.currentRotation, new GL3DVec3d(0, 1, 0)));
        this.updateCameraTransformation();
    }
}
