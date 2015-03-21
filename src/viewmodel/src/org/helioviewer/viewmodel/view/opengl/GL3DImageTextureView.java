package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;

public class GL3DImageTextureView extends AbstractGL3DView implements GL3DView {

    private boolean recaptureRequested = true;
    private boolean regionChanged = true;
    private boolean forceUpdate = false;

    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        render3D(GL3DState.get());
    }

    @Override
    public void render3D(GL3DState state) {
        // Only copy Framebuffer if necessary
        if (forceUpdate || recaptureRequested || regionChanged) {

            regionChanged = false;
            forceUpdate = false;
            recaptureRequested = false;
        }

    }

    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        newView.addViewListener(new ViewListener() {
            @Override
            public void viewChanged(View sender, ChangeEvent aEvent) {
                if (aEvent.reasonOccurred(RegionChangedReason.class)) {
                    recaptureRequested = true;
                    regionChanged = true;
                } else if (aEvent.reasonOccurred(RegionUpdatedReason.class)) {
                    regionChanged = true;
                } else if (aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
                    recaptureRequested = true;
                } else if (aEvent.reasonOccurred(CacheStatusChangedReason.class)) {
                    recaptureRequested = true;
                }
            }
        });
    }

    public void forceUpdate() {
        this.forceUpdate = true;
    }

}
