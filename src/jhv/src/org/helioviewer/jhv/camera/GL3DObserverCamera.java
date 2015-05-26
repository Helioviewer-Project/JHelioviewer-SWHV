package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.AbstractView;

public class GL3DObserverCamera extends GL3DCamera {

    private final GL3DObserverCameraOptionPanel observerCameraOptionPanel;

    public GL3DObserverCamera() {
        super();
        observerCameraOptionPanel = new GL3DObserverCameraOptionPanel(this);
    }

    @Override
    public void reset() {
        super.reset();
        this.forceTimeChanged(Displayer.getLastUpdatedTimestamp());
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        if (Displayer.getLastUpdatedTimestamp() != null)
            this.timeChanged(Displayer.getLastUpdatedTimestamp());
        else
            this.timeChanged(new Date());
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public String getName() {
        return "View from observer";
    }

    @Override
    public void timeChanged(Date date) {
        if (!this.getTrackingMode()) {
            forceTimeChanged(date);
        }
    }

    private void forceTimeChanged(Date date) {
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
        double d;
        AbstractView view = LayersModel.getActiveView();

        if (view != null) {
            MetaData metadata = view.getMetaData();
            this.localRotation = metadata.getRotationObs();
            d = metadata.getDistanceObs();
        } else {
            this.localRotation = GL3DQuatd.ZERO;
            d = Sun.MeanEarthDistance;
        }
        this.setZTranslation(-d);

        this.updateCameraTransformation();
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return observerCameraOptionPanel;
    }

}
