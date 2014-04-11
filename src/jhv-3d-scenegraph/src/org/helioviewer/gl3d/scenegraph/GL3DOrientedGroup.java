package org.helioviewer.gl3d.scenegraph;

import org.helioviewer.gl3d.GL3DHelper;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateVector;

/**
 * An oriented Group rotates this group according to the orientation vector
 * provided by its superclass. Extend this class to implement Coordinate System
 * sensitive 3D content.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DOrientedGroup extends GL3DCoordinateSystemGroup {

    public GL3DOrientedGroup(String name) {
        super(name);
    }

    public void updateMatrix(GL3DState state) {
        // Log.debug("GL3DCoordinateSystemGroup: '"+this.getName()+"' updateMatrix");
        this.updateOrientation(state);
        super.updateMatrix(state);
    }

    private void updateOrientation(GL3DState state) {
        CoordinateVector orientationVector = getOrientation();
        CoordinateConversion toViewSpace = getCoordinateSystem().getConversion(state.getActiveCamera().getViewSpaceCoordinateSystem());
        
        GL3DVec3d orientation = GL3DHelper.toVec(toViewSpace.convert(orientationVector));
        orientation.normalize();

        this.m.set(GL3DMat4d.identity());
        
        
        if (!orientation.equals(new GL3DVec3d(0, 0, 1))) {
        	GL3DVec3d orientationXY = new GL3DVec3d(orientation.x, orientation.y, 0);
            double theta = Math.asin(orientationXY.y);
            GL3DMat4d thetaRotation = GL3DMat4d.rotation(theta, new GL3DVec3d(1, 0, 0));
            // Log.debug("GL3DOrientedGroup: Rotating Theta "+theta);
            this.m.multiply(thetaRotation);
        }
        
        //if (!(orientation.equals(new GL3DVec3d(0, 1, 0)))) {
            GL3DVec3d orientationXZ = new GL3DVec3d(orientation.x, 0, orientation.z);
            double phi = Math.acos(orientationXZ.z);
            if (orientationXZ.x < 0) {
                phi = 0 - phi;
            }
            phi += 0.7;
            // Log.debug("GL3DOrientedGroup: Rotating Phi "+phi);
            GL3DMat4d phiRotation = GL3DMat4d.rotation(phi, new GL3DVec3d(0, 1, 0));
            this.m.multiply(phiRotation);
        //}
        
    }
    
    

    public abstract CoordinateVector getOrientation();
}
