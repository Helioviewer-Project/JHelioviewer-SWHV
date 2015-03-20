package org.helioviewer.gl3d.camera;

import java.util.Date;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DEarthCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DEarthCamera extends GL3DSolarRotationTrackingTrackballCamera implements TimeListener {

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
        Displayer.addTimeListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        Displayer.removeTimeListener(this);
    }

    @Override
    public String getName() {
        return "View from Earth";
    }

    @Override
    public void timeChanged(Date date) {
        if (!this.getTrackingMode()) {
            updateRotation(date);
        }
    }

    private void updateRotation(Date date) {
        double b0 = Astronomy.getB0InRadians(date);
        double currentRotation = Astronomy.getL0Radians(date);

        this.localRotation = GL3DQuatd.createRotation(b0, GL3DVec3d.XAxis);
        this.localRotation.rotate(GL3DQuatd.createRotation(currentRotation, GL3DVec3d.YAxis));

        this.updateCameraTransformation();
        this.setTime(date.getTime());
    }

}
