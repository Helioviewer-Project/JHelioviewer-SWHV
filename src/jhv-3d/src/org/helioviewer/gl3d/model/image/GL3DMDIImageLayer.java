package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DMDIorHMIImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DMDIImageLayer extends GL3DImageLayer {
    private GL3DImageSphere imageMesh;

    public GL3DMDIImageLayer(GL3DView mainView) {
        super("MDI Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
        GLFragmentShaderProgram fragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, new GL3DMDIorHMIImageFragmentShaderProgram());
        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram   vertexShader   = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);
        
        imageMesh = new GL3DImageSphere(imageTextureView, vertexShader, fragmentShader, this);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();
        
        this.accellerationShape = new GL3DHitReferenceShape();
        
        double xOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getX()+this.imageTextureView.metadata.getPhysicalLowerLeft().getX())/(2.0*this.imageTextureView.metadata.getPhysicalImageWidth());
        double yOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getY()+this.imageTextureView.metadata.getPhysicalLowerLeft().getY())/(2.0*this.imageTextureView.metadata.getPhysicalImageHeight());
        vertex.setDefaultOffset(xOffset, yOffset);

        this.fragmentShader.setCutOffRadius((Constants.SunRadius/this.imageTextureView.metadata.getPhysicalImageWidth()));


        this.addNode(imageMesh);
    }

    protected GL3DImageMesh getImageCorona() {
        return null;
    }

    protected GL3DImageMesh getImageSphere() {
        return imageMesh;
    }
    



}
