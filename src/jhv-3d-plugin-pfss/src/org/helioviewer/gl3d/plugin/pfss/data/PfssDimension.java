package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.LinkedList;
import java.util.List;

import org.helioviewer.gl3d.wcs.CarringtonCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateSystem;

/**
 * PFSS Dimension DataStructure
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * @author Simon Spšrri (simon.spoerri@fhnw.ch): Added b0, l0 and
 *         CoordinateSystem
 * 
 */
public class PfssDimension {

    private CoordinateSystem coordinateSystem;

    private double b0;
    private double l0;

    /**
     * Contains the curves
     */
    public List<PfssCurve> curves;

    /**
     * Constructor
     */
    public PfssDimension(double b0, double l0) {
        this.coordinateSystem = new CarringtonCoordinateSystem(b0, l0);
        this.b0 = b0;
        this.l0 = l0;
        this.curves = new LinkedList<PfssCurve>();
    }

    /**
     * Get all curves
     * 
     * @return List<PfssCurve> list of all curves
     */
    public List<PfssCurve> getCurves() {
        return curves;
    }

    public double getB0() {
        return b0;
    }

    public double getL0() {
        return l0;
    }

    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }
}
