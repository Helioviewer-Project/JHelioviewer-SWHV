package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DMDIorHMIImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DHMIImageLayer extends GL3DImageLayer {
    private GL3DImageSphere imageMesh;

    public GL3DHMIImageLayer(GL3DView mainView) {
        super("HMI Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
        GLFragmentShaderProgram fragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, new GL3DMDIorHMIImageFragmentShaderProgram());
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, new GL3DImageVertexShaderProgram());

        imageMesh = new GL3DImageSphere(imageTextureView, vertexShader, fragmentShader);

        this.accellerationShape = new GL3DHitReferenceShape();

        this.addNode(imageMesh);
    }

    protected GL3DImageMesh getImageCorona() {
        return null;
    }

    protected GL3DImageMesh getImageSphere() {
        return imageMesh;
    }
}
