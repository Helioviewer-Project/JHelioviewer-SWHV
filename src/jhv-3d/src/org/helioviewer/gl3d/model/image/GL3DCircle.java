package org.helioviewer.gl3d.model.image;

import java.util.List;

import org.helioviewer.gl3d.GL3DHelper;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateVector;

public class GL3DCircle extends GL3DMesh {
    private double radius;
    private GL3DVec4d color;
    private GL3DImageLayer layer;
    private GL3DMat4d phiRotation = null;
    
    public GL3DCircle(double radius, GL3DVec4f color, String name, GL3DImageLayer layer) {
        super(name);
        this.radius = radius*0.999;
        this.color = new GL3DVec4d((double) color.x, (double) color.y, (double) color.z, (double) color.w);
        this.layer = layer;
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
    	int counter = 0;
    	
    	CoordinateVector orientationVector = this.layer.getOrientation();
        CoordinateConversion toViewSpace = this.layer.getCoordinateSystem().getConversion(state.getActiveCamera().getViewSpaceCoordinateSystem());

        GL3DVec3d orientation = GL3DHelper.toVec(toViewSpace.convert(orientationVector)).normalize();

    	
    	if (!(orientation.equals(new GL3DVec3d(0, 1, 0)))) {
            GL3DVec3d orientationXZ = new GL3DVec3d(orientation.x, 0, orientation.z);
            double phi = Math.acos(orientationXZ.z);
            if (orientationXZ.x < 0) {
                phi = 0 - phi;
            }
            
            phiRotation = GL3DMat4d.rotation(phi, new GL3DVec3d(0, 1, 0));
            
        }
    	
    	for (double i = 0; i < 2*Math.PI; i += 0.1){
    		double x = Math.sin(i)*radius;
    	    double y = Math.cos(i)*radius;
    	    
    	    double cx = x * phiRotation.m[0] + y * phiRotation.m[4] + phiRotation.m[12];
            double cy = x * phiRotation.m[1] + y * phiRotation.m[5] + phiRotation.m[13];
            double cz = x * phiRotation.m[2] + y * phiRotation.m[6] + phiRotation.m[14];
           
    	    double vx = phiRotation.m[8] * (-1) + phiRotation.m[12];
    	    double vy = phiRotation.m[9] * (-1) + phiRotation.m[13];
    	    double vz = phiRotation.m[10] * (-1) + phiRotation.m[14];
    	    
    		positions.add(new GL3DVec3d(cx,cy,cz));
        	indices.add(counter++);
        	normals.add(new GL3DVec3d(vx,vy,vz));
        	colors.add(color);

    	}
    	return GL3DMeshPrimitive.TRIANGLE_FAN;
    }

    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }
}
