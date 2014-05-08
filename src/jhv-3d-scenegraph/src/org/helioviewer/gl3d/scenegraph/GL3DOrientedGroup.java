package org.helioviewer.gl3d.scenegraph;

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

    @Override
    public void updateMatrix(GL3DState state) {
        // Log.debug("GL3DCoordinateSystemGroup: '"+this.getName()+"' updateMatrix");
        this.updateOrientation(state);
        super.updateMatrix(state);
    }

    private void updateOrientation(GL3DState state) {

    }

    public abstract CoordinateVector getOrientation();
}
