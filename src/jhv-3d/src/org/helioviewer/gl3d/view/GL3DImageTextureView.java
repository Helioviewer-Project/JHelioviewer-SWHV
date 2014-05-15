package org.helioviewer.gl3d.view;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
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
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.viewport.Viewport;

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
public class GL3DImageTextureView extends AbstractGL3DView implements GL3DView, GLFragmentShaderView {

    public GL3DImageTextureView() {
        super();
    }

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
    private final GL3DImageFragmentShaderProgram fragmentShader = new GL3DImageFragmentShaderProgram();


    @Override
    public void renderGL(GL gl, boolean nextView) {
        render3D(GL3DState.get());
    }

    @Override
    public void render3D(GL3DState state) {
        GL gl = state.gl;
        if (this.getView() != null) {
            // Only copy Framebuffer if necessary
            GLTextureHelper th = new GLTextureHelper();
            if (true) {
                this.capturedRegion = copyScreenToTexture(state, th);
                // gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                if (forceUpdate) {
                    this.notifyViewListeners(new ChangeEvent(new ImageTextureRecapturedReason(this, this.textureId, this.textureScale, StaticRegion.createAdaptedRegion(this.capturedRegion.getRectangle()))));
                }
                regionChanged = false;
                forceUpdate = false;
                recaptureRequested = false;
            }
        }
    }

    @Override
    public void deactivate(GL3DState state) {
        textureHelper.delTextureID(state.gl, this.textureId);
        this.textureId = -1;
    }

    public int getTextureId() {
        return this.textureId;
    }

    public Region copyScreenToTexture(GL3DState state, GLTextureHelper th) {
        JHVJPXView jhvjpx = getAdapter(JHVJPXView.class);
        Region region = jhvjpx.getImageData().getRegion();
        Viewport viewport = getAdapter(ViewportView.class).getViewport();

        if (viewport == null || region == null) {
            regionChanged = false;
            return null;
        }

        this.textureId = getAdapter(JHVJPXView.class).texID;
        // th.copyFrameBufferToTexture(gl, textureId, captureRectangle);
        this.textureScale = th.getTextureScale(textureId);
        if (vertexShader != null) {

            double xOffset = (region.getLowerLeftCorner().getX());
            double yOffset = (region.getLowerLeftCorner().getY());
            double xScale = (1. / region.getWidth());
            double yScale = (1. / region.getHeight());

            double deltat = jhvjpx.getImageData().getDateMillis() / 1000.0 - Constants.referenceDate;
            double theta = 0.0;
            phi = DifferentialRotation.calculateRotationInRadians(0.0, deltat) % (Math.PI * 2.0);
            this.vertexShader.changeRect(xOffset, yOffset, xScale, yScale);
            this.vertexShader.changeTextureScale(jhvjpx.getImageData().getScaleX(), jhvjpx.getImageData().getScaleY());
            this.vertexShader.changeAngles(theta, phi);

            if(!jhvjpx.getBaseDifferenceMode() && jhvjpx.getPreviousImageData()!=null){
                Region differenceRegion = jhvjpx.getPreviousImageData().getRegion();
                double differenceXOffset = (differenceRegion.getLowerLeftCorner().getX());
                double differenceYOffset = (differenceRegion.getLowerLeftCorner().getY());
                double differenceXScale = (1./differenceRegion.getWidth());
                double differenceYScale = (1./differenceRegion.getHeight());
                double differenceDeltat = jhvjpx.getPreviousImageData().getDateMillis() / 1000.0 - Constants.referenceDate;
                double differenceTheta = 0.0;
                double differencePhi = DifferentialRotation.calculateRotationInRadians(0.0, differenceDeltat) % (Math.PI * 2.0);
                this.vertexShader.changeDifferenceTextureScale(jhvjpx.getPreviousImageData().getScaleX(), jhvjpx.getPreviousImageData().getScaleY());
                this.vertexShader.setDifferenceRect(differenceXOffset, differenceYOffset, differenceXScale, differenceYScale);
                this.vertexShader.changeDifferenceAngles(differenceTheta, differencePhi);
            }
            if(jhvjpx.getBaseDifferenceMode() && jhvjpx.getBaseDifferenceImageData()!=null){
                Region differenceRegion = jhvjpx.getBaseDifferenceImageData().getRegion();
                double differenceXOffset = (differenceRegion.getLowerLeftCorner().getX());
                double differenceYOffset = (differenceRegion.getLowerLeftCorner().getY());
                double differenceXScale = (1./differenceRegion.getWidth());
                double differenceYScale = (1./differenceRegion.getHeight());
                double differenceDeltat = jhvjpx.getBaseDifferenceImageData().getDateMillis() / 1000.0 - Constants.referenceDate;
                double differenceTheta = 0.0;
                double differencePhi = DifferentialRotation.calculateRotationInRadians(0.0, differenceDeltat) % (Math.PI * 2.0);
                this.vertexShader.changeDifferenceTextureScale(jhvjpx.getBaseDifferenceImageData().getScaleX(), jhvjpx.getBaseDifferenceImageData().getScaleY());
                this.vertexShader.setDifferenceRect(differenceXOffset, differenceYOffset, differenceXScale, differenceYScale);
                this.vertexShader.changeDifferenceAngles(differenceTheta, differencePhi);
            }
            this.fragmentShader.changeTextureScale(jhvjpx.getImageData().getScaleX(), jhvjpx.getImageData().getScaleY());
            this.fragmentShader.changeAngles(theta, phi);
        }

        this.recaptureRequested = false;
        return region;
    }

    public double phi = 0.0;

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

    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        GLFragmentShaderView nextView = view.getAdapter(GLFragmentShaderView.class);
        if (nextView != null) {
            shaderBuilder = nextView.buildFragmentShader(shaderBuilder);
        }

        fragmentShader.build(shaderBuilder);
        //System.out.println("GLTEXTURE: " + shaderBuilder.getCode());
        return shaderBuilder;
    }

    public GL3DImageFragmentShaderProgram getFragmentShader() {
        return this.fragmentShader;
    }
}
