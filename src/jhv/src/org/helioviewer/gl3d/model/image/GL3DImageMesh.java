package org.helioviewer.gl3d.model.image;

import org.helioviewer.gl3d.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

/**
 * A {@link GL3DImageMesh} is used to map a image that was rendered in the 2D
 * sub-chain onto a mesh. The image is provided as a texture that was created by
 * a {@link GL3DImageTextureView}.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DImageMesh extends GL3DMesh {

    protected GL3DImageTextureView imageTextureView;

    protected Region capturedRegion;

    private boolean reshapeRequested = false;

    public GL3DImageMesh(String name, GL3DImageTextureView _imageTextureView) {
        super(name, new GL3DVec4f(0, 1, 0, 0.5f), new GL3DVec4f(0, 0, 0, 0));
        this.imageTextureView = _imageTextureView;

        imageTextureView.addViewListener(new ViewListener() {
            @Override
            public void viewChanged(View sender, ChangeEvent aEvent) {
                ImageTextureRecapturedReason reason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
                if (reason != null) {
                    reshapeRequested = true;
                    capturedRegion = reason.getCapturedRegion();
                    markAsChanged();
                    // Log.debug("GL3DImageMesh.reshape: "+getName()+" Reason="+reason+", Event="+aEvent);
                }
            }
        });
        this.reshapeRequested = true;
        this.markAsChanged();
    }

    public GL3DImageMesh(String name, GL3DImageTextureView _imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram, boolean viewListener) {
        super(name, new GL3DVec4f(0, 1, 0, 0.5f), new GL3DVec4f(0, 0, 0, 0));
        this.imageTextureView = _imageTextureView;

        this.reshapeRequested = true;
        this.markAsChanged();
    }

    @Override
    public void shapeInit(GL3DState state) {
        super.shapeInit(state);
        this.imageTextureView.forceUpdate();
        // Log.debug("GL3DImageMesh.shapeInit: "+getName()+" Forcing image texture to update!");
    }

    @Override
    public void shapeUpdate(GL3DState state) {
        if (this.reshapeRequested) {
            // Reshape Mesh
            //recreateMesh(state);
            // Log.debug("GL3DImageMesh.reshape: "+getName()+" Recreated Mesh!");
            this.reshapeRequested = false;
        }
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GLFilterView glfilter = this.imageTextureView.getAdapter(GLFilterView.class);
        if (glfilter != null) {
            glfilter.renderGL(state.gl, true);
        }

        GLVertexShaderProgram.pushShader(state.gl);

        ShaderFactory.bindVertexShader(state.gl);
        ShaderFactory.bindFragmentShader(state.gl);

        super.shapeDraw(state);

        GLVertexShaderProgram.popShader(state.gl);
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }
}
