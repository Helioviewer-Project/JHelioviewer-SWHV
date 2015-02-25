package org.helioviewer.gl3d.changeevent;

import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;

/**
 * The ChangedReason that is emitted when the {@link GL3DImageTextureView}
 * recaptured the image that was produced by the underlying 2D sub-viewchain.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 */
public class ImageTextureRecapturedReason implements ChangedReason {

    private View sender;
    private Region capturedRegion;

    public ImageTextureRecapturedReason(View sender, Region capturedRegion) {
        this.sender = sender;
        this.capturedRegion = capturedRegion;
    }

    public View getView() {
        return sender;
    }

    public Region getCapturedRegion() {
        return capturedRegion;
    }
}
