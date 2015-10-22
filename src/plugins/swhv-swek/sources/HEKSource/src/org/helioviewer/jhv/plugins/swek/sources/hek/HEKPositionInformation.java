package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.util.List;

import org.helioviewer.jhv.base.math.GL3DVec3d;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;

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
    private final List<GL3DVec3d> boundBox;

    /** bound cc */
    private final List<GL3DVec3d> boundCC;

    /** central point */
    private final GL3DVec3d centralPoint;

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
    public HEKPositionInformation(JHVCoordinateSystem coordinateSystem, List<GL3DVec3d> boundBox, List<GL3DVec3d> boundCC,
            GL3DVec3d centralPoint) {
        this.coordinateSystem = coordinateSystem;
        this.boundBox = boundBox;
        this.centralPoint = centralPoint;
        this.boundCC = boundCC;
    }

    @Override
    public JHVCoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    @Override
    public List<GL3DVec3d> getBoundBox() {
        return boundBox;
    }

    @Override
    public GL3DVec3d centralPoint() {
        return centralPoint;
    }

    @Override
    public List<GL3DVec3d> getBoundCC() {
        return boundCC;
    }

}
