package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.View;

public class GL3DObserverCamera extends GL3DCamera {

    private final GL3DObserverCameraOptionPanel observerCameraOptionPanel;

    public GL3DObserverCamera() {
        super();
        observerCameraOptionPanel = new GL3DObserverCameraOptionPanel(this);
    }

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
        return "View from observer";
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
       if (m == null) {
            View view = Layers.getActiveView();
            if (view != null) {
                m = view.getMetaData(new ImmutableDateTime(date.getTime()));
            }
        }

        double d;
        if (m == null) {
            localRotation = GL3DQuatd.ZERO;
            d = Sun.MeanEarthDistance;
        } else {
            localRotation = m.getRotationObs();
            d = m.getDistanceObs();
        }

        setZTranslation(-d);
        updateCameraTransformation();
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return observerCameraOptionPanel;
    }

}
