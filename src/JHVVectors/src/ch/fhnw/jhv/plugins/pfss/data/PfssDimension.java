package ch.fhnw.jhv.plugins.pfss.data;

import java.util.LinkedList;
import java.util.List;

/**
 * PFSS Dimension DataStructure
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PfssDimension {

    /**
     * Contains the curves
     */
    public List<PfssCurve> curves;

    /**
     * Constructor
     */
    public PfssDimension() {
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
}
