package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DLASCOImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DLascoImageLayer extends GL3DImageLayer {
    private GL3DImageCorona lascoImageMesh;

    public GL3DLascoImageLayer(GL3DView mainView) {
        super("LASCO Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
        HelioviewerOcculterMetaData hvMetaData = (HelioviewerOcculterMetaData) metaDataView.getMetaData();
        GLFragmentShaderProgram fragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, new GL3DLASCOImageFragmentShaderProgram(hvMetaData));
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, new GL3DImageVertexShaderProgram());

        lascoImageMesh = new GL3DImageCorona("LASCO", imageTextureView, vertexShader, fragmentShader);

        this.accellerationShape = new GL3DHitReferenceShape();

        this.addNode(lascoImageMesh);
    }

    protected GL3DImageMesh getImageCorona() {
        return lascoImageMesh;
    }

    protected GL3DImageMesh getImageSphere() {
        return null;
    }
}
