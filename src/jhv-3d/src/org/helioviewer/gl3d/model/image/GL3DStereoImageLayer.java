package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.shader.GL3DImageCoronaFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageCoronaVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DStereoImageLayer extends GL3DImageLayer {
    private GL3DImageSphere sphere = null;
    private GL3DImageCorona corona = null;
    
    public GL3DStereoImageLayer(GL3DView mainView) {
        super("Stereo Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
    	this.gl = gl;
		HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaDataView.getMetaData();
		
		GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram   vertexShader   = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        GL3DImageCoronaVertexShaderProgram vertexCorona = new GL3DImageCoronaVertexShaderProgram();
        GLVertexShaderProgram  vertexCoronaShader   = GL3DShaderFactory.createVertexShaderProgram(gl, vertexCorona);        
        this.imageTextureView.setVertexShader(vertex, vertexCorona);        
 
		this.coronaFragmentShader = new GL3DImageCoronaFragmentShaderProgram();        
        GLFragmentShaderProgram coronaFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.coronaFragmentShader);
        
        corona = new GL3DImageCorona(imageTextureView, vertexShader, coronaFragmentShader, this);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();
        
		GL3DImageCoronaFragmentShaderProgram fragmentShader = new GL3DImageCoronaFragmentShaderProgram();
        
		this.addNode(corona);
		
		
        
        double xOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getX()+this.imageTextureView.metadata.getPhysicalLowerLeft().getX())/(2.0*this.imageTextureView.metadata.getPhysicalImageWidth());
        double yOffset = -(this.imageTextureView.metadata.getPhysicalUpperLeft().getY()+this.imageTextureView.metadata.getPhysicalLowerLeft().getY())/(2.0*this.imageTextureView.metadata.getPhysicalImageHeight());
		
		// Don't display sphere for corona images
		if(!hvMetaData.getDetector().startsWith("COR"))
		{
	    	this.sphereFragmentShader = new GL3DImageFragmentShaderProgram();
	    	GLFragmentShaderProgram sphereFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.sphereFragmentShader);
	    	sphere = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader, this);
	    	this.addNode(sphere);
	    	this.sphereFragmentShader.setCutOffRadius(Constants.SunRadius/this.imageTextureView.metadata.getPhysicalImageWidth());
		}
        vertex.setDefaultOffset(xOffset, yOffset);
        
        this.coronaFragmentShader.setCutOffRadius(0.99*(Constants.SunRadius/this.imageTextureView.metadata.getPhysicalImageWidth()));
        
        
    }

    protected GL3DImageMesh getImageCorona() {
        return this.corona;
    }

    protected GL3DImageMesh getImageSphere() {
        return this.sphere;
    }
    



}
