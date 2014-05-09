package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DHMIImageLayer extends GL3DImageLayer {
    private GL3DImageSphere imageMesh;

    public GL3DHMIImageLayer(GL3DView mainView) {
        super("HMI Image Layer", mainView);
    }

    @Override
    protected void createImageMeshNodes(GL gl) {
        this.sphereFragmentShader = new GL3DImageFragmentShaderProgram();
        GLFragmentShaderProgram fragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.sphereFragmentShader);
        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);

        imageMesh = new GL3DImageSphere(imageTextureView, vertexShader, fragmentShader, this);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();

        this.sphereFragmentShader.setCutOffRadius((float) (Constants.SunRadius / this.imageTextureView.metadata.getPhysicalImageWidth()));

        this.addNode(imageMesh);
    }

    @Override
    protected GL3DImageMesh getImageSphere() {
        return imageMesh;
    }

}
