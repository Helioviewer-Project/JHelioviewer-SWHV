package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.AbstractView;

public class GL3DObserverCamera extends GL3DCamera implements TimeListener {

    private final GL3DObserverCameraOptionPanel observerCameraOptionPanel;

    public GL3DObserverCamera(boolean init) {
        super();
        observerCameraOptionPanel = new GL3DObserverCameraOptionPanel(this);
        if (init) {
            Displayer.addFirstTimeListener(this);
        }
    }

    public GL3DObserverCamera() {
        this(false);
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

        Displayer.addFirstTimeListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        Displayer.removeTimeListener(this);
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
        MetaData metadata;
        AbstractView view = LayersModel.getActiveView();

        if (view != null && (metadata = view.getMetaData()) instanceof HelioviewerMetaData) {
            this.localRotation = metadata.getLocalRotation();
            d = ((HelioviewerMetaData) metadata).getDistanceSolarRadii();
        } else {
            this.localRotation = new GL3DQuatd(0, Astronomy.getL0Radians(date), 0);
            d = Astronomy.getDistanceSolarRadii(date);
        }
        this.setZTranslation(-d);

        this.updateCameraTransformation();
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return observerCameraOptionPanel;
    }

}
