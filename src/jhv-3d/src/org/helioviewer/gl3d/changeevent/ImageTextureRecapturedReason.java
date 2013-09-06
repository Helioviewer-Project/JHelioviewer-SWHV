package org.helioviewer.gl3d.changeevent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;

/**
 * The ChangedReason that is emitted when the {@link GL3DImageTextureView}
 * recaptured the image that was produced by the underlying 2D sub-viewchain.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 */
public class ImageTextureRecapturedReason implements ChangedReason {

    private View sender;

    private Vector2dDouble textureScale;

    private Integer textureId;

    private Region capturedRegion;

    public ImageTextureRecapturedReason(View sender, Integer textureId, Vector2dDouble textureScale, Region capturedRegion) {
        this.sender = sender;
        this.textureId = textureId;
        this.textureScale = textureScale;
        this.capturedRegion = capturedRegion;
    }

    public View getView() {
        return sender;
    }

    public Vector2dDouble getTextureScale() {
        return this.textureScale;
    }

    public Integer getTextureId() {
        return textureId;
    }

    public Region getCapturedRegion() {
        return capturedRegion;
    }
}
