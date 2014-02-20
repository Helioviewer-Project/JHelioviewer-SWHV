package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.LinkedList;
import java.util.List;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;
import org.helioviewer.gl3d.wcs.CoordinateVector;

/**
 * PFSS Curve
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * @author Simon Spoerri (simon.spoerri@fhnw.ch), Changed to Coordinate Vectors
 */
public class PfssCurve {

    /**
     * Color of the curve
     */
    public GL3DVec3f color;

    /**
     * Contains all Vector3f points
     */
    public List<CoordinateVector> points;

    /**
     * Constructor
     */
    public PfssCurve() {
        points = new LinkedList<CoordinateVector>();
    }
}
