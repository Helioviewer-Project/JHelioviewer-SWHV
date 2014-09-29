package org.helioviewer.gl3d.view;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.media.opengl.GL2;

import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Astronomy;
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
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerPositionedMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
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
    public void renderGL(GL2 gl, boolean nextView) {
        render3D(GL3DState.get());
    }

    @Override
    public void render3D(GL3DState state) {
        if (this.getView() != null) {
            // Only copy Framebuffer if necessary
            GLTextureHelper th = new GLTextureHelper();
            if (true) {
                this.capturedRegion = copyScreenToTexture(state, th);
                // gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
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
        if (this.textureId == 0 || this.textureId == -1) {
            this.textureId = getAdapter(JHVJP2View.class).texID;
        }
        return this.textureId;
    }

    public Region copyScreenToTexture(GL3DState state, GLTextureHelper th) {
        SubimageDataView sim = this.getAdapter(ImageInfoView.class).getAdapter(SubimageDataView.class);
        MetaDataView metadataView = this.getAdapter(MetaDataView.class);

        Region region = sim.getSubimageData().getRegion();

        Viewport viewport = getAdapter(ViewportView.class).getViewport();

        if (viewport == null || region == null) {
            regionChanged = false;
            return null;
        }

        this.textureId = getAdapter(JHVJP2View.class).texID;
        // th.copyFrameBufferToTexture(gl, textureId, captureRectangle);
        this.textureScale = th.getTextureScale(textureId);
        if (vertexShader != null) {

            double xOffset = (region.getLowerLeftCorner().getX());
            double yOffset = (region.getLowerLeftCorner().getY());
            double xScale = (1. / region.getWidth());
            double yScale = (1. / region.getHeight());

            Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(sim.getSubimageData().getDateMillis());
            theta = -Astronomy.getB0InRadians(cal);
            phi = Astronomy.getL0Radians(new Date(sim.getSubimageData().getDateMillis()));//DifferentialRotation.calculateRotationInRadians(0.0, deltat) % (Math.PI * 2.0);
            if (metadataView.getMetaData() instanceof HelioviewerPositionedMetaData && ((HelioviewerPositionedMetaData) (metadataView.getMetaData())).getInstrument().equalsIgnoreCase("SECCHI")) {
                phi -= ((HelioviewerPositionedMetaData) (metadataView.getMetaData())).getStonyhurstLongitude() / MathUtils.radeg;
                theta = -((HelioviewerPositionedMetaData) (metadataView.getMetaData())).getStonyhurstLatitude() / MathUtils.radeg;
            }
            this.vertexShader.changeRect(xOffset, yOffset, xScale, yScale);
            this.vertexShader.changeTextureScale(sim.getSubimageData().getScaleX(), sim.getSubimageData().getScaleY());
            this.vertexShader.changeAngles(theta, phi);
            JHVJPXView jhvjpx = this.getAdapter(JHVJPXView.class);
            if (jhvjpx != null) {
                if (!jhvjpx.getBaseDifferenceMode() && jhvjpx.getPreviousImageData() != null) {
                    Region differenceRegion = jhvjpx.getPreviousImageData().getRegion();
                    double differenceXOffset = (differenceRegion.getLowerLeftCorner().getX());
                    double differenceYOffset = (differenceRegion.getLowerLeftCorner().getY());
                    double differenceXScale = (1. / differenceRegion.getWidth());
                    double differenceYScale = (1. / differenceRegion.getHeight());
                    cal.setTimeInMillis(jhvjpx.getPreviousImageData().getDateMillis());
                    double differenceTheta = -Astronomy.getB0InRadians(cal);
                    double differencePhi = Astronomy.getL0Radians(new Date(jhvjpx.getPreviousImageData().getDateMillis()));//DifferentialRotation.calculateRotationInRadians(0.0, differenceDeltat) % (Math.PI * 2.0);
                    this.vertexShader.changeDifferenceTextureScale(jhvjpx.getPreviousImageData().getScaleX(), jhvjpx.getPreviousImageData().getScaleY());
                    this.vertexShader.setDifferenceRect(differenceXOffset, differenceYOffset, differenceXScale, differenceYScale);
                    this.vertexShader.changeDifferenceAngles(differenceTheta, differencePhi);
                    this.fragmentShader.changeDifferenceTextureScale(jhvjpx.getPreviousImageData().getScaleX(), jhvjpx.getPreviousImageData().getScaleY());
                    this.fragmentShader.setDifferenceRect(differenceXOffset, differenceYOffset, differenceXScale, differenceYScale);
                    this.fragmentShader.changeDifferenceAngles(differenceTheta, differencePhi);
                } else if (jhvjpx.getBaseDifferenceMode() && jhvjpx.getBaseDifferenceImageData() != null) {
                    Region differenceRegion = jhvjpx.getBaseDifferenceImageData().getRegion();
                    double differenceXOffset = (differenceRegion.getLowerLeftCorner().getX());
                    double differenceYOffset = (differenceRegion.getLowerLeftCorner().getY());
                    double differenceXScale = (1. / differenceRegion.getWidth());
                    double differenceYScale = (1. / differenceRegion.getHeight());
                    cal.setTimeInMillis(jhvjpx.getBaseDifferenceImageData().getDateMillis());
                    double differenceTheta = -Astronomy.getB0InRadians(cal);
                    double differencePhi = Astronomy.getL0Radians(new Date(jhvjpx.getBaseDifferenceImageData().getDateMillis()));//DifferentialRotation.calculateRotationInRadians(0.0, differenceDeltat) % (Math.PI * 2.0);
                    this.vertexShader.changeDifferenceTextureScale(jhvjpx.getBaseDifferenceImageData().getScaleX(), jhvjpx.getBaseDifferenceImageData().getScaleY());
                    this.vertexShader.setDifferenceRect(differenceXOffset, differenceYOffset, differenceXScale, differenceYScale);
                    this.vertexShader.changeDifferenceAngles(differenceTheta, differencePhi);
                    this.fragmentShader.changeDifferenceTextureScale(jhvjpx.getBaseDifferenceImageData().getScaleX(), jhvjpx.getBaseDifferenceImageData().getScaleY());
                    this.fragmentShader.setDifferenceRect(differenceXOffset, differenceYOffset, differenceXScale, differenceYScale);
                    this.fragmentShader.changeDifferenceAngles(differenceTheta, differencePhi);
                } else {
                    this.fragmentShader.changeDifferenceTextureScale(jhvjpx.getImageData().getScaleX(), jhvjpx.getImageData().getScaleY());
                    this.fragmentShader.setDifferenceRect(xOffset, yOffset, xScale, yScale);
                    this.fragmentShader.changeDifferenceAngles(theta, phi);
                }
            }
            this.fragmentShader.changeTextureScale(sim.getSubimageData().getScaleX(), sim.getSubimageData().getScaleY());
            this.fragmentShader.changeAngles(theta, phi);
            if (metadata instanceof HelioviewerOcculterMetaData) {
                HelioviewerOcculterMetaData md = (HelioviewerOcculterMetaData) metadata;
                this.fragmentShader.setCutOffRadius(md.getInnerPhysicalOcculterRadius());
                this.fragmentShader.setOuterCutOffRadius(md.getOuterPhysicalOcculterRadius());
            }
        }

        this.recaptureRequested = false;
        return region;
    }

    public double phi = 0.0;
    public double theta = 0.0;

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
        return shaderBuilder;
    }

    public GL3DImageFragmentShaderProgram getFragmentShader() {
        return this.fragmentShader;
    }
}
