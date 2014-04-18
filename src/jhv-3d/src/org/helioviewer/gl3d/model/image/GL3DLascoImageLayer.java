package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
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

    protected void createImageMeshNodes(GL gl) {
        HelioviewerOcculterMetaData hvMetaData = (HelioviewerOcculterMetaData) metaDataView.getMetaData();
        
        GLFragmentShaderProgram fragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.coronaFragmentShader);
        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram   vertexShader   = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);        
 
        
        this.imageTextureView.metadata = this.metaDataView.getMetaData();
        
        this.accellerationShape = new GL3DHitReferenceShape();
        
        double xOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getX()+this.imageTextureView.metadata.getPhysicalLowerLeft().getX())/(2.0*this.imageTextureView.metadata.getPhysicalImageWidth());
        double yOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getY()+this.imageTextureView.metadata.getPhysicalLowerLeft().getY())/(2.0*this.imageTextureView.metadata.getPhysicalImageHeight());
        vertex.setDefaultOffset(xOffset, yOffset);

    }

    protected GL3DImageMesh getImageSphere() {
        return null;
    }
    



}
