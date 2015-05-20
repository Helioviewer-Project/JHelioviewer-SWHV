package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.renderable.components.RenderableCamera;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DEarthCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DEarthCamera extends GL3DCamera implements TimeListener {

    private final GL3DEarthCameraOptionPanel earthCameraOptionPanel;

    public GL3DEarthCamera() {
        super();
        earthCameraOptionPanel = new GL3DEarthCameraOptionPanel(this);
    }

    @Override
    public void reset() {
        this.resetCurrentDragRotation();
        super.reset();
        this.forceTimeChanged(Displayer.getLastUpdatedTimestamp());
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        this.timeChanged(Displayer.getLastUpdatedTimestamp());
        Displayer.addFirstTimeListener(this);
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
            forceTimeChanged(date);
        }
    }

    public void forceTimeChanged(Date date) {
        if (date != null) {
            updateRotation(date);

            RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
            if (renderableCamera != null) {
                renderableCamera.setTimeString(date);
                ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
            }
        }
    }

    private void updateRotation(Date date) {
        this.setZTranslation(-Astronomy.getDistanceSolarRadii(date));
        double b0 = Astronomy.getB0Radians(date);
        double currentRotation = Astronomy.getL0Radians(date);

        this.localRotation = GL3DQuatd.createRotation(b0, GL3DVec3d.XAxis);
        this.localRotation.rotate(GL3DQuatd.createRotation(currentRotation, GL3DVec3d.YAxis));

        this.updateCameraTransformation();
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return earthCameraOptionPanel;
    }

}
