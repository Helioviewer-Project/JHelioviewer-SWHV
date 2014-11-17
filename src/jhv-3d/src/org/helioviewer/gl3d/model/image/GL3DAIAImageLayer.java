package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DAIAImageLayer extends GL3DImageLayer {
    private GL3DImageSphere sphere;
    private GL3DImageSphere corona;

    public GL3DAIAImageLayer(GL3DView mainView) {
        super("AIA Image Layer", mainView);
    }

    @Override
    protected void createImageMeshNodes(GL2 gl) {
        this.sphereFragmentShader = imageTextureView.getFragmentShader();
        GL3DImageVertexShaderProgram vertexShaderProgram = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, vertexShaderProgram);
        this.imageTextureView.setVertexShader(vertexShaderProgram);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();
        sphere = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader, this, true, false, false);
        corona = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader, this, false, true, true);
        this.addNode(sphere);
        this.addNode(corona);
    }

    @Override
    protected GL3DImageMesh getImageSphere() {
        return this.sphere;
    }

    @Override
    public GL3DImageMesh getImageCorona() {
        return this.corona;
    }

    @Override
    public void setCoronaVisibility(boolean visible) {
        if (!visible) {
            this.corona.getDrawBits().on(Bit.Hidden);
        } else {
            this.corona.getDrawBits().off(Bit.Hidden);
        }
    }

}
