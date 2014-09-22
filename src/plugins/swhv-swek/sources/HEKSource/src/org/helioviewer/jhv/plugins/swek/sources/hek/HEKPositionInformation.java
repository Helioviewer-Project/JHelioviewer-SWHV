package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.util.List;

import org.helioviewer.jhv.data.datatype.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.JHVPoint;
import org.helioviewer.jhv.data.datatype.JHVPositionInformation;

/**
 * Defines the HEK event position information.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class HEKPositionInformation implements JHVPositionInformation {

    /** coordinate system */
    private final JHVCoordinateSystem coordinateSystem;

    /** bound box */
    private final List<JHVPoint> boundBox;

    /** central point */
    private final JHVPoint centralPoint;

    /**
     * Create a HEKPositionInformation for the given coordinate system, bound
     * box and central point.
     * 
     * @param coordinateSystem
     *            the coordinate system.
     * @param boundBox
     *            the bound box
     * @param centralPoint
     *            the central point
     */
    public HEKPositionInformation(JHVCoordinateSystem coordinateSystem, List<JHVPoint> boundBox, JHVPoint centralPoint) {
        this.coordinateSystem = coordinateSystem;
        this.boundBox = boundBox;
        this.centralPoint = centralPoint;
    }

    @Override
    public JHVCoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    @Override
    public List<JHVPoint> getBoundBox() {
        return boundBox;
    }

    @Override
    public JHVPoint centralPoint() {
        return centralPoint;
    }

}
