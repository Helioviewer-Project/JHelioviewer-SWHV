package org.helioviewer.gl3d.plugin.pfss;

import java.util.LinkedList;
import java.util.List;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.GL3DHelper;
import org.helioviewer.gl3d.plugin.pfss.data.PfssCurve;
import org.helioviewer.gl3d.plugin.pfss.data.PfssDimension;
import org.helioviewer.gl3d.scenegraph.GL3DMesh.GL3DMeshPrimitive;
import org.helioviewer.gl3d.scenegraph.GL3DOrientedGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DPolyLine;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.HeliocentricCartesianCoordinateSystem;

/**
 * Scene Graph representation of a PFSS Model.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DPfssModel extends GL3DOrientedGroup {

    private CoordinateSystem coordinateSystem;
    private CoordinateVector orientation;

    private int pointReductionModulo = 1;

    public GL3DPfssModel(PfssDimension pfss) {
        super("PFSS Model");

        CoordinateSystem pfssCoordinateSystem = pfss.getCoordinateSystem();

        this.coordinateSystem = new HeliocentricCartesianCoordinateSystem();
        this.orientation = this.coordinateSystem.createCoordinateVector(0, 0, 1);

        CoordinateConversion conversion = pfssCoordinateSystem.getConversion(this.coordinateSystem);

        for (PfssCurve curve : pfss.curves) {
            List<GL3DVec3d> points = new LinkedList<GL3DVec3d>();
            for (int i = 0; i < curve.points.size(); i++) {
                if (i % this.pointReductionModulo == 0) {

                    CoordinateVector pfssCoordinate = curve.points.get(i);
                    CoordinateVector convertedCoordinate = conversion.convert(pfssCoordinate);

                    points.add(GL3DHelper.toVec(convertedCoordinate).multiply(Constants.SunRadius));
                }
            }
            this.addNode(new GL3DPolyLine(points, new GL3DVec4f(curve.color.x, curve.color.y, curve.color.z, 1.0f), GL3DMeshPrimitive.LINE_STRIP));
        }
    }

    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }

    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystem;
    }

    public CoordinateVector getOrientation() {
        return this.orientation;
    }
}
