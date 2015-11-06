package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

public class GL3DObserverCamera extends GL3DCamera {

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
        MetaData m = null;
        View view = Layers.getActiveView();
        if (view != null) {
            m = view.getMetaData(date);
        }

        double d;
        if (m == null) {
            localRotation = Quatd.ZERO;
            d = Sun.MeanEarthDistance;
        } else {
            localRotation = m.getRotationObs();
            d = m.getDistanceObs();
        }

        setZTranslation(-d);
        updateCameraTransformation();
    }

    private GL3DObserverCameraOptionPanel optionPanel;

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        if (optionPanel == null) {
            optionPanel = new GL3DObserverCameraOptionPanel(this);
        }
        return optionPanel;
    }

}
