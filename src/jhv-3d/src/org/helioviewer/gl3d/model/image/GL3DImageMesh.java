package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.gl3d.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * A {@link GL3DImageMesh} is used to map a image that was rendered in the 2D
 * sub-chain onto a mesh. The image is provided as a texture that was created by
 * a {@link GL3DImageTextureView}.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DImageMesh extends GL3DMesh {

    protected GL3DImageTextureView imageTextureView;

    private GLTextureHelper th = new GLTextureHelper();

    private GLVertexShaderProgram vertexShaderProgram;
    private GLFragmentShaderProgram fragmentShaderProgram;

    protected Region capturedRegion;
    protected Vector2dDouble textureScale;

    private boolean reshapeRequested = false;

    public GL3DImageMesh(String name, GL3DImageTextureView _imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram) {
        super(name, new GL3DVec4f(0, 1, 0, 0.5f), new GL3DVec4f(0, 0, 0, 0));
        this.imageTextureView = _imageTextureView;

        this.vertexShaderProgram = vertexShaderProgram;
        this.fragmentShaderProgram = fragmentShaderProgram;

        imageTextureView.addViewListener(new ViewListener() {

            public void viewChanged(View sender, ChangeEvent aEvent) {
                ImageTextureRecapturedReason reason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
                if (reason != null) {
                    reshapeRequested = true;
                    capturedRegion = reason.getCapturedRegion();
                    textureScale = reason.getTextureScale();
                    markAsChanged();
                    // Log.debug("GL3DImageMesh.reshape: "+getName()+" Reason="+reason+", Event="+aEvent);
                }
            }
        });
        this.reshapeRequested = true;
        this.markAsChanged();
    }

    public void shapeInit(GL3DState state) {
        super.shapeInit(state);
        this.imageTextureView.forceUpdate();
        // Log.debug("GL3DImageMesh.shapeInit: "+getName()+" Forcing image texture to update!");
    }

    public void shapeUpdate(GL3DState state) {
        if (this.reshapeRequested) {
            // Reshape Mesh
            recreateMesh(state);
            // Log.debug("GL3DImageMesh.reshape: "+getName()+" Recreated Mesh!");
            this.reshapeRequested = false;
        }
    }

    public void shapeDraw(GL3DState state) {
        th.bindTexture(state.gl, this.imageTextureView.getTextureId());
        state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        // state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
        // GL.GL_LINEAR);

        GLVertexShaderProgram.pushShader(state.gl);
        GLFragmentShaderProgram.pushShader(state.gl);
        this.vertexShaderProgram.bind(state.gl);
        this.fragmentShaderProgram.bind(state.gl);

        super.shapeDraw(state);

        GLVertexShaderProgram.popShader(state.gl);
        GLFragmentShaderProgram.popShader(state.gl);

        th.bindTexture(state.gl, 0);
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }
}
