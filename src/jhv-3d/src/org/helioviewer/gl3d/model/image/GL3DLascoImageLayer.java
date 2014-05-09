package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DLascoImageLayer extends GL3DImageLayer {

    public GL3DLascoImageLayer(GL3DView mainView) {
        super("LASCO Image Layer", mainView);
    }

    @Override
    protected void createImageMeshNodes(GL gl) {
        HelioviewerOcculterMetaData hvMetaData = (HelioviewerOcculterMetaData) metaDataView.getMetaData();

        GLFragmentShaderProgram fragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.coronaFragmentShader);
        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram vertexShader = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);

        this.imageTextureView.metadata = this.metaDataView.getMetaData();

        this.accellerationShape = new GL3DHitReferenceShape();

    }

    @Override
    protected GL3DImageMesh getImageSphere() {
        return null;
    }

}
