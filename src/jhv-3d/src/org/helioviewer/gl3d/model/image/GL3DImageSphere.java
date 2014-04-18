package org.helioviewer.gl3d.model.image;

import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dDouble;
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

	private GL3DImageLayer layer;

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
			int resolutionX = 20;
			int resolutionY = 70;
			int numberOfPositions = 0;
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
					numberOfPositions ++;    	                
				}
			}

			for (int latNumber = 0; latNumber < resolutionX; latNumber++) {
				for (int longNumber = 0; longNumber < resolutionY; longNumber++) {
					int first = (latNumber * (resolutionY + 1)) + longNumber;
					int second = first + resolutionY + 1;
					indices.add(first);
					indices.add(first + 1);
					indices.add(second + 1);
					indices.add(first);
					indices.add(second);
					indices.add(second + 1);
				}
			}
			MetaData metaData = this.layer.metaDataView.getMetaData();
			Vector2dDouble ul = metaData.getPhysicalUpperLeft();
			Vector2dDouble ur = metaData.getPhysicalUpperRight();
			Vector2dDouble lr = metaData.getPhysicalLowerRight();
			Vector2dDouble ll = metaData.getPhysicalLowerLeft();
			System.out.println("UL:" +ul);
			System.out.println("UR:" +ur);
			System.out.println("LR:" +lr);
			System.out.println("LL: "+ll);

			int beginPositionNumberCorona = numberOfPositions; 
			positions.add(new GL3DVec3d(ul.getX(), ul.getY(), 0.));
			numberOfPositions++;
			positions.add(new GL3DVec3d(ur.getX(), ur.getY(), 0.));
			numberOfPositions++;
			positions.add(new GL3DVec3d(lr.getX(), lr.getY(), 0.));
			numberOfPositions++;
			positions.add(new GL3DVec3d(ll.getX(), ll.getY(), 0.));
			numberOfPositions++;

			indices.add(beginPositionNumberCorona + 0);
			indices.add(beginPositionNumberCorona + 1);
			indices.add(beginPositionNumberCorona + 2);
			indices.add(beginPositionNumberCorona + 2);
			indices.add(beginPositionNumberCorona + 3);
			indices.add(beginPositionNumberCorona + 0);

		}
		return GL3DMeshPrimitive.TRIANGLES;
	}  
}
