package ch.fhnw.jhv.plugins.pfss.data;

import java.util.LinkedList;
import java.util.List;

import javax.vecmath.Vector3f;

/**
 * PFSS Curve
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PfssCurve {

    /**
     * Color of the curve
     */
    public Vector3f color;

    /**
     * Contains all Vector3f points
     */
    public List<Vector3f> points;

    /**
     * Constructor
     */
    public PfssCurve() {
        points = new LinkedList<Vector3f>();
    }
}
