package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.shader.GL3DImageCoronaFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageSphereFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DStereoImageLayer extends GL3DImageLayer {
    private GL3DImageSphere sphere;
    private GL3DImageCorona corona;

    public GL3DStereoImageLayer(GL3DView mainView) {
        super("Stereo Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
        GLFragmentShaderProgram sphereFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, new GL3DImageSphereFragmentShaderProgram());
        GLFragmentShaderProgram coronaFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, new GL3DImageCoronaFragmentShaderProgram());
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, new GL3DImageVertexShaderProgram());

        sphere = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader);
        corona = new GL3DImageCorona(imageTextureView, vertexShader, coronaFragmentShader);

        this.accellerationShape = new GL3DHitReferenceShape();

        this.addNode(corona);
        this.addNode(sphere);
    }

    protected GL3DImageMesh getImageCorona() {
        return this.corona;
    }

    protected GL3DImageMesh getImageSphere() {
        return this.sphere;
    }
}
