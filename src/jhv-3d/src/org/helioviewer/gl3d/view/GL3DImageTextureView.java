package org.helioviewer.gl3d.view;

import java.awt.Rectangle;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.gl3d.model.image.GL3DImageMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.viewport.Viewport;

import sun.reflect.generics.visitor.Reifier;

//import com.sun.xml.internal.ws.api.addressing.WSEndpointReference.Metadata;

/**
 * Connects the 3D viewchain to the 2D viewchain. The underlying 2D viewchain
 * renders it's image to the framebuffer. This view then copies that framebuffer
 * to a texture object which can then be used to be mapped onto a 3D mesh. Use a
 * {@link GL3DImageMesh} to connect the resulting texture to a mesh, or directly
 * use the {@link GL3DShaderFactory} to create standard Image Meshes.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageTextureView extends AbstractGL3DView implements GL3DView {

	private int textureId = -1;

	private Vector2dDouble textureScale = null;

	private Region capturedRegion = null;

	private boolean recaptureRequested = true;
	private boolean regionChanged = true;
	private boolean forceUpdate = false;
	
	private GL3DImageVertexShaderProgram vertexShader = null;
	public MetaData metadata = null;
	
	public double minZ = 0.0;
	public double maxZ = Constants.SunRadius;
	// private Vector2dInt renderOffset;

	private GLFragmentShaderProgram fragmentShader;

	public void render3D(GL3DState state) {
		GL gl = state.gl;
		if (this.getView() != null) {
			// Log.debug("GL3DImageTextureView.render3D: Rendering for view "+this.getView());

			// Only copy Framebuffer if necessary
			GLTextureHelper th = new GLTextureHelper();
			if (forceUpdate || recaptureRequested || regionChanged) {
				// state.checkGLErrors("GL3DImageTextureView.beforeRenderChild");
				// gl.glDisable(GL.GL_BLEND);
				this.renderChild(gl);

				// state.checkGLErrors("GL3DImageTextureView.beforeCopyToTexture");
				this.capturedRegion = copyScreenToTexture(state, th);
				gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

				if (forceUpdate) {
					// state.checkGLErrors("GL3DImageTextureView.beforeNotify");
					this.notifyViewListeners(new ChangeEvent(
							new ImageTextureRecapturedReason(
									this,
									this.textureId,
									this.textureScale,
									StaticRegion
											.createAdaptedRegion(this.capturedRegion
													.getRectangle()))));
				}
				regionChanged = false;
				forceUpdate = false;
			}
		}
	}

	public void deactivate(GL3DState state) {
		textureHelper.delTextureID(state.gl, this.textureId);
		this.textureId = -1;
	}

	public int getTextureId() {
		return this.textureId;
	}

	private Region copyScreenToTexture(GL3DState state, GLTextureHelper th) {
		GL gl = state.gl;

		if (this.textureId < 0) {
			this.textureId = th.genTextureID(gl);
		}
		// gl.glBindTexture(GL.GL_TEXTURE_2D, this.textureId);

		Region region = getAdapter(RegionView.class).getRegion();
		Viewport viewport = getAdapter(ViewportView.class).getViewport();
		Vector2dInt renderOffset = getAdapter(GL3DImageRegionView.class)
				.getRenderOffset();
		if (viewport == null || region == null) {
			regionChanged = false;
			return null;
		}
		int offsetX = renderOffset == null ? 0 : renderOffset.getX();
		int offsetY = (renderOffset == null ? 0 : renderOffset.getY());
		Rectangle captureRectangle = new Rectangle(offsetX, offsetY,
				viewport.getWidth(), viewport.getHeight());
		// Log.debug("GL3DImageTextureView: Capturing "+captureRectangle);
		
		if (region != null) capturedRegion = region;

		if (this.metadata != null){
			th.copyFrameBufferToTexture(gl, textureId, captureRectangle);
			this.textureScale = th.getTextureScale(textureId);
		
			double xOffset = (region.getLowerLeftCorner().getX() - this.metadata.getPhysicalLowerLeft().getX())/this.metadata.getPhysicalImageWidth();
			double yOffset = (region.getLowerLeftCorner().getY() - this.metadata.getPhysicalLowerLeft().getY())/this.metadata.getPhysicalImageHeight();
			double xScale = (this.metadata.getPhysicalImageWidth()/region.getWidth());
			double yScale = (this.metadata.getPhysicalImageHeight()/region.getHeight());
			
			if (vertexShader != null) {
				this.vertexShader.changeRect(xOffset, yOffset, Math.abs(xScale), Math.abs(yScale));
				this.vertexShader.changeTextureScale(this.textureScale.getX(), this.textureScale.getY());
				GL3DImageFragmentShaderProgram fr = (GL3DImageFragmentShaderProgram)(this.fragmentShader);
				fr.changeTextureScale(this.textureScale.getX(), this.textureScale.getY());
			}
		}

		this.recaptureRequested = false;

		return region;
	}

	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		newView.addViewListener(new ViewListener() {

			public void viewChanged(View sender, ChangeEvent aEvent) {
				// Log.debug("GL3DImageTextureView.viewChanged RegionChanged "+aEvent);
				if (aEvent.reasonOccurred(RegionChangedReason.class)) {
					recaptureRequested = true;
					regionChanged = true;
				} else if (aEvent.reasonOccurred(RegionUpdatedReason.class)) {
					// regionChanged = true;
					recaptureRequested = true;
					// Log.debug("GL3DImageTextureView.viewChanged RegionUpdated "+aEvent);
				} else if (aEvent
						.reasonOccurred(SubImageDataChangedReason.class)) {
					// regionChanged = true;
					recaptureRequested = true;
					// Log.debug("GL3DImageTextureView.viewChanged SubImageDataChanged"+aEvent);
				} else if (aEvent
						.reasonOccurred(CacheStatusChangedReason.class)) {
					recaptureRequested = true;
					// Log.debug("GL3DImageTextureView.viewChanged CacheStatus "+aEvent);
				} else {
					// Log.debug("GL3DImageTextureView.viewChanged Not Handling Event "+aEvent);
				}
			}
		});
	}

	public Vector2dDouble getTextureScale() {
		return textureScale;
	}

	public Region getCapturedRegion() {
		return capturedRegion;
	}

	public void forceUpdate() {
		this.forceUpdate = true;
	}

	public void setVertexShader(GL3DImageVertexShaderProgram vertexShader) {
		this.vertexShader = vertexShader;
	}

	public void setFragmentShader(GLFragmentShaderProgram fragmentShader) {
		this.fragmentShader = fragmentShader;	
	}
}
