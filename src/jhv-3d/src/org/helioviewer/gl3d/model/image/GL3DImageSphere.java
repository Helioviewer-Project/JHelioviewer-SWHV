package org.helioviewer.gl3d.model.image;

import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.GL3DHelper;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.TextureCoordinateSystem;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;



/**
 * Maps the solar disc part of an image layer onto an adaptive mesh that either
 * covers the entire solar disc or the just the part that is visible in the view
 * frustum.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageSphere extends GL3DImageMesh {
	protected Vector2dInt mappingResolution = new Vector2dInt((int)Math.pow(2, 6), (int)Math.pow(2, 6));

    private TextureCoordinateSystem textureCoordinateSystem;
    private SolarImageToSolarSphereConversion solarImageToSolarSphereConversion;

    
    private SolarImageCoordinateSystem solarImageCS = new SolarImageCoordinateSystem();
    private SolarSphereCoordinateSystem solarSphereCS = new SolarSphereCoordinateSystem();
    
    private GL3DImageLayer layer;
    
    private GL3DMat4d phiRotation = null;

    public GL3DImageSphere(
    		GL3DImageTextureView imageTextureView,
    		GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram,
    		GL3DImageLayer imageLayer)
    {
        super("Sphere", imageTextureView, vertexShaderProgram, fragmentShaderProgram);
        layer = imageLayer;
        
    }

    public void shapeDraw(GL3DState state) {
      state.gl.glDisable(GL.GL_CULL_FACE);
      state.gl.glEnable(GL.GL_DEPTH_TEST);
      state.gl.glEnable(GL.GL_BLEND);
      
        super.shapeDraw(state);
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
    	if (this.capturedRegion != null) {
    		Region region = this.capturedRegion;
    		textureCoordinateSystem = new TextureCoordinateSystem(textureScale, region);
             
            solarImageToSolarSphereConversion = (SolarImageToSolarSphereConversion) solarImageCS.getConversion(solarSphereCS);
            solarImageToSolarSphereConversion.setAutoAdjustToValidValue(true);
                         
            CoordinateVector orientationVector = this.layer.getOrientation();
            CoordinateConversion toViewSpace = this.layer.getCoordinateSystem().getConversion(state.getActiveCamera().getViewSpaceCoordinateSystem());

            GL3DVec3d orientation = GL3DHelper.toVec(toViewSpace.convert(orientationVector)); //.normalize(); - not needed for atan2

            phiRotation = GL3DMat4d.rotation(Math.atan2(orientation.x, orientation.z), new GL3DVec3d(0, 1, 0));
    		 int resolutionX = 50;
    		 int resolutionY = 100;

             
    		 for (int latNumber = 0; latNumber <= resolutionX; latNumber++) {
    	            double theta = latNumber * Math.PI / resolutionX;
    	            double sinTheta = Math.sin(theta);
    	            double cosTheta = Math.cos(theta);

    	            for (int longNumber = 0; longNumber <= resolutionY; longNumber++) {
    	                double phi = longNumber * 2 * Math.PI / resolutionY;
    	                double sinPhi = Math.sin(phi);
    	                double cosPhi = Math.cos(phi);

    	                double x = cosPhi * sinTheta;
    	                double y = cosTheta;
    	                double z = sinPhi * sinTheta;
    	                
    	                
    	                positions.add(new GL3DVec3d(Constants.SunRadius * x, Constants.SunRadius * y, Constants.SunRadius * z));
    	                
    	                
    	                createVertex(solarSphereCS.createCoordinateVector(Constants.SunRadius * x, Constants.SunRadius * y, Constants.SunRadius * z), normals, textCoords, colors);
    	                
    	                
    	            }
    	        }

    		 GL3DVec3d tmpSolarSphereVec;
    	        for (int latNumber = 0; latNumber < resolutionX; latNumber++) {
    	            for (int longNumber = 0; longNumber < resolutionY; longNumber++) {
    	                int first = (latNumber * (resolutionY + 1)) + longNumber;
    	                int second = first + resolutionY + 1;
    	                
    	                
    	                
    	                tmpSolarSphereVec = positions.get(first);
    	                /*
    	                double z0 = tmpSolarSphereVec.z;
    	                if (phiRotation != null){
    	                	z0 = tmpSolarSphereVec.x * phiRotation.m[2] + tmpSolarSphereVec.y * phiRotation.m[6] + tmpSolarSphereVec.z * phiRotation.m[10] + phiRotation.m[14];
    	                }
    	                
    	                tmpSolarSphereVec = positions.get(first+1);
    	                double z1 = tmpSolarSphereVec.z;
    	                if (phiRotation != null){
    	                	z1 = tmpSolarSphereVec.x * phiRotation.m[2] + tmpSolarSphereVec.y * phiRotation.m[6] + tmpSolarSphereVec.z * phiRotation.m[10] + phiRotation.m[14];
    	                }
    	                
    	                tmpSolarSphereVec = positions.get(second + 1);
    	                double z2 = tmpSolarSphereVec.z;
    	                if (phiRotation != null){
    	                	z2 = tmpSolarSphereVec.x * phiRotation.m[2] + tmpSolarSphereVec.y * phiRotation.m[6] + tmpSolarSphereVec.z * phiRotation.m[10] + phiRotation.m[14];
    	                }
    	                
    	                tmpSolarSphereVec = positions.get(second);
    	                double z3 = tmpSolarSphereVec.z;
    	                if (phiRotation != null){
    	                	z3 = tmpSolarSphereVec.x * phiRotation.m[2] + tmpSolarSphereVec.y * phiRotation.m[6] + tmpSolarSphereVec.z * phiRotation.m[10] + phiRotation.m[14];
    	                }
    	                */
    	                //if (z0 >= 0 && z1 >= 0 && z2 >= 0 && z3 >= 0) {
    	                	
        	                indices.add(first);
        	                indices.add(first + 1);
        	                indices.add(second + 1);
        	                indices.add(first);
        	                
        	                indices.add(second);
        	                indices.add(second + 1);
        	                
    	                //}
    	                
    	            }
    	        }
    	 }
    	        return GL3DMeshPrimitive.TRIANGLES;
    	 }  
    		 
       
 
   private GL3DVec3d createVertex(CoordinateVector solarSphereCoordinate,List<GL3DVec3d> normals, List<GL3DVec2d> texCoords, List<GL3DVec4d> colors) {
        double x = solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.X_COORDINATE);
        double y = solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.Y_COORDINATE);
        double z = solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.Z_COORDINATE);
        
        GL3DVec3d position = new GL3DVec3d(x, y, z);
        colors.add(new GL3DVec4d(0, 0, 0, 1.0));

        double cx = x*phiRotation.m[0] + y*phiRotation.m[4] + z*phiRotation.m[8] + phiRotation.m[12];
        double cy = x*phiRotation.m[1] + y*phiRotation.m[5] + z*phiRotation.m[9] + phiRotation.m[13];
        MetaData metaData = this.layer.metaDataView.getMetaData();
        
        double tx = (cx-metaData.getPhysicalLowerLeft().getX())/(metaData.getPhysicalImageWidth());
        double ty = (cy-metaData.getPhysicalLowerLeft().getY())/(metaData.getPhysicalImageHeight());


        texCoords.add(new GL3DVec2d(tx, ty));

        return position;
    }    	
   
   

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }
    
}
