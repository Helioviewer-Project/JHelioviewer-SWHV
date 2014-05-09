package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DStereoImageLayer extends GL3DImageLayer {
    private GL3DImageSphere sphere = null;

    public GL3DStereoImageLayer(GL3DView mainView) {
        super("Stereo Image Layer", mainView);
    }

    @Override
    protected void createImageMeshNodes(GL gl) {
        this.gl = gl;
        HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaDataView.getMetaData();

        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);

        GLFragmentShaderProgram coronaFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.coronaFragmentShader);

        this.imageTextureView.metadata = this.metaDataView.getMetaData();

        // Don't display sphere for corona images
        if (!hvMetaData.getDetector().startsWith("COR")) {
            this.sphereFragmentShader = new GL3DImageFragmentShaderProgram();
            GLFragmentShaderProgram sphereFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.sphereFragmentShader);
            sphere = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader, this);
            this.addNode(sphere);
            this.sphereFragmentShader.setCutOffRadius(Constants.SunRadius / this.imageTextureView.metadata.getPhysicalImageWidth());
        }

    }

    @Override
    protected GL3DImageMesh getImageSphere() {
        return this.sphere;
    }

}
