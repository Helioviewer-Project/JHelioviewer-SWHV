package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.GL3DQuatd;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class GL3DEarthCamera extends GL3DCamera {

    private final GL3DEarthCameraOptionPanel earthCameraOptionPanel;

    public GL3DEarthCamera() {
        super();
        earthCameraOptionPanel = new GL3DEarthCameraOptionPanel(this);
    }

    @Override
    public void reset() {
        this.resetCurrentDragRotation();
        super.reset();
        this.forceTimeChanged(Layers.getLastUpdatedTimestamp());
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        this.timeChanged(Layers.getLastUpdatedTimestamp());
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public String getName() {
        return "View from Earth";
    }

    @Override
    public void timeChanged(Date date) {
        if (!this.getTrackingMode()) {
            forceTimeChanged(date);
        } else {
            Displayer.render();
        }
    }

    private void forceTimeChanged(Date date) {
        if (date == null)
            return;

        updateRotation(date, null);

        RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
        if (renderableCamera != null) {
            renderableCamera.setTimeString(date);
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
        }
    }

    @Override
    public void updateRotation(Date date, MetaData m) {
        Position.Latitudinal p = Sun.getEarth(date);

        this.localRotation = new GL3DQuatd(p.lat, p.lon);
        this.setZTranslation(-p.rad);

        this.updateCameraTransformation();
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return earthCameraOptionPanel;
    }

}
