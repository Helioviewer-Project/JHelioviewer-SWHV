package org.helioviewer.gl3d.view;

import javax.media.opengl.GL2;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.RegionAdapter;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * This view is responsible for setting the current region of interest and
 * converting incoming constraints to the 2D rectangle that needs to be
 * requested from the image source and sent through the 2D view chain part.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageRegionView extends AbstractGL3DView implements GL3DView, RegionView, ViewportView {
    private RegionView underlyingRegionView;
    private ViewportView viewportView;
    private MetaDataView metaDataView;

    private Vector2dInt renderOffset;

    private Viewport maximalViewport;
    private Viewport innerViewport;
    private Region detectedRegion;
    private Region actualImageRegion;

    public void render3D(GL3DState state) {
        GL2 gl = state.gl;

        if (this.actualImageRegion == null || this.innerViewport == null) {
            return;
        }
        // The child node will render a rect to a rect(lowerLeft, lowerRight,
        // regionWidth, regionHeight).
        // make sure that this will be in the lower left corner.
        double regionWidth = this.actualImageRegion.getWidth();
        double regionHeight = this.actualImageRegion.getHeight();
        double regionWidthOfViewport = regionWidth / this.innerViewport.getWidth() * maximalViewport.getWidth();
        double regionHeightOfViewport = regionHeight / this.innerViewport.getHeight() * maximalViewport.getHeight();

        double left = this.actualImageRegion.getCornerX();
        double right = left + regionWidthOfViewport;
        double bottom = this.actualImageRegion.getCornerY();
        double top = bottom + regionHeightOfViewport;

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(left, right, bottom, top, -1, 10000);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        this.renderChild(gl);

        // Resume Previous Projection
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        RegionView regionView = newView.getAdapter(RegionView.class);
        this.underlyingRegionView = regionView;

        ViewportView viewportView = newView.getAdapter(ViewportView.class);
        this.viewportView = viewportView;

        MetaDataView metaDataView = newView.getAdapter(MetaDataView.class);
        if (metaDataView != null) {
            this.metaDataView = metaDataView;
            if (this.detectedRegion == null) {
                this.detectedRegion = new RegionAdapter(metaDataView.getMetaData().getPhysicalRegion());
            }
        }
    }

    public boolean setRegion(Region r, ChangeEvent event) {
        if (event == null) {
            event = new ChangeEvent(new RegionUpdatedReason(this, r));
        } else {
            event.addReason(new RegionUpdatedReason(this, r));
        }

        this.detectedRegion = r;
        boolean hasChanged = this.updateRegionAndViewport(event);

        hasChanged |= this.viewportView.setViewport(innerViewport, event);
        // System.out.println(underlyingRegionView);
        // System.out.println("VPP" + this.innerViewport);
        hasChanged |= this.underlyingRegionView.setRegion(this.actualImageRegion, event);

        return hasChanged;
    }

    protected boolean updateRegionAndViewport(ChangeEvent event) {
        MetaData metaData = this.metaDataView.getMetaData();
        Region region = ViewHelper.cropRegionToImage(detectedRegion, metaData);
        ViewportImageSize requiredViewportSize = ViewHelper.calculateViewportImageSize(this.maximalViewport, region);
        this.innerViewport = StaticViewport.createAdaptedViewport(requiredViewportSize.getSizeVector());

        this.actualImageRegion = region;

        return true;
    }

    public Region getRegion() {
        return this.actualImageRegion;
    }

    public boolean setViewport(Viewport v, ChangeEvent event) {
        this.maximalViewport = v;

        this.updateRegionAndViewport(event);

        boolean hasChanged = this.viewportView.setViewport(innerViewport, event);
        hasChanged |= this.underlyingRegionView.setRegion(this.actualImageRegion, event);

        return hasChanged;
    }

    public Viewport getViewport() {
        return this.innerViewport;
    }

    public Vector2dInt getRenderOffset() {
        return renderOffset;
    }
}
