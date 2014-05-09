package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DAIAImageLayer extends GL3DImageLayer {
    private GL3DImageSphere sphere;

    public GL3DAIAImageLayer(GL3DView mainView) {
        super("AIA Image Layer", mainView);
    }

    @Override
    protected void createImageMeshNodes(GL gl) {
        this.sphereFragmentShader = imageTextureView.getFragmentShader();

        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();

        sphere = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader, this);

        this.sphereFragmentShader.setCutOffRadius((float) (Constants.SunRadius / this.imageTextureView.metadata.getPhysicalImageWidth()));

        this.addNode(sphere);
        this.gl = gl;
    }

    @Override
    protected GL3DImageMesh getImageSphere() {
        return this.sphere;
    }

}
