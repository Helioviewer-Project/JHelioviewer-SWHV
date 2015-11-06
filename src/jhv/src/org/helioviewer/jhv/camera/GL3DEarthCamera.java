package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;

public class GL3DEarthCamera extends GL3DCamera {

    @Override
    public void reset() {
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
    public void timeChanged(JHVDate date) {
        if (!this.getTrackingMode()) {
            forceTimeChanged(date);
        } else {
            Displayer.render();
        }
    }

    private void forceTimeChanged(JHVDate date) {
        if (date == null)
            return;

        updateRotation(date);

        RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
        if (renderableCamera != null) {
            renderableCamera.setTimeString(date.toString());
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
        }
    }

    @Override
    public void updateRotation(JHVDate date) {
        Position.Latitudinal p = Sun.getEarth(date.getTime());

        this.localRotation = new Quatd(p.lat, p.lon);
        this.setZTranslation(-p.rad);

        this.updateCameraTransformation();
    }

    private GL3DEarthCameraOptionPanel optionPanel;

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        if (optionPanel == null) {
            optionPanel = new GL3DEarthCameraOptionPanel(this);
        }
        return optionPanel;
    }

}
