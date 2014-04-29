package org.helioviewer.gl3d.model;

import java.util.List;

import org.helioviewer.gl3d.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

/**
 * Helper Node to visualize the current content of the Framebuffer.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DFramebufferImage extends GL3DMesh implements ViewListener {
    private Integer textureId;
    // private Vector2dDouble textureScale;
    private boolean recreateMesh = true;

    private Region region;

    // private Viewport viewport;
    // private MetaData metaData;
    //
    // private Vector2dDouble textureScale;

    public GL3DFramebufferImage() {
        super("Framebuffer", new GL3DVec4f(1, 1, 1, 1), new GL3DVec4f(0, 0, 0, 0));
    }

    public void shapeDraw(GL3DState state) {
        if (this.region != null) {
            GLTextureHelper th = new GLTextureHelper();
            th.bindTexture(state.gl, textureId);
            super.shapeDraw(state);
            th.bindTexture(state.gl, 0);
        }
    }

    public void shapeUpdate(GL3DState state) {
        if (recreateMesh) {
            this.recreateMesh(state);
            this.recreateMesh = false;
        }
    }

    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ImageTextureRecapturedReason.class)) {
            ImageTextureRecapturedReason imageTextureRecapturedReason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
            this.textureId = imageTextureRecapturedReason.getTextureId();
            // this.textureScale =
            // imageTextureRecapturedReason.getTextureScale();
            this.region = imageTextureRecapturedReason.getCapturedRegion();
            // this.viewport =
            // sender.getAdapter(ViewportView.class).getViewport();
            // this.metaData =
            // sender.getAdapter(MetaDataView.class).getMetaData();
            this.recreateMesh = true;
            // this.textureScale =
            // imageTextureRecapturedReason.getTextureScale();
            // Log.debug("GL3DFramebufferImage: Recreating FrameBuffer, TextureScale="+imageTextureRecapturedReason.getTextureScale());
            markAsChanged();
        }

    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        // Log.debug("GL3DFramebufferImage: Create Mesh!");
        if (region != null) {
            double blx = region.getCornerX();
            double bly = region.getCornerY();
            double tr_x = region.getUpperRightCorner().getX();
            double tr_y = region.getUpperRightCorner().getY();
            positions.add(new GL3DVec3d(blx, bly, 0));
            positions.add(new GL3DVec3d(tr_x, bly, 0));
            positions.add(new GL3DVec3d(tr_x, tr_y, 0));
            positions.add(new GL3DVec3d(blx, tr_y, 0));

            textCoords.add(new GL3DVec2d(0, 0));
            textCoords.add(new GL3DVec2d(1, 0));
            textCoords.add(new GL3DVec2d(1, 1));
            textCoords.add(new GL3DVec2d(0, 1));

            // Region outerRegion =
            // ViewHelper.expandRegionToViewportAspectRatio(viewport, region,
            // metaData);
            // Log.debug("GL3DFramebufferImage: Region      = "+region);
            // Log.debug("GL3DFramebufferImage: OuterRegion = "+outerRegion);
            // Log.debug("GL3DFramebufferImage: Viewport    = "+viewport);

            // textCoords.add(new GL3DVec2d(0, 0));
            // textCoords.add(new GL3DVec2d(this.textureScale.getX(), 0));
            // textCoords.add(new GL3DVec2d(this.textureScale.getX(),
            // this.textureScale.getY()));
            // textCoords.add(new GL3DVec2d(0, this.textureScale.getY()));

            normals.add(new GL3DVec3d(0, 0, 1));
            normals.add(new GL3DVec3d(0, 0, 1));
            normals.add(new GL3DVec3d(0, 0, 1));
            normals.add(new GL3DVec3d(0, 0, 1));

            indices.add(0);
            indices.add(1);
            indices.add(2);
            indices.add(3);

        }

        return GL3DMeshPrimitive.QUADS;
    }

}
