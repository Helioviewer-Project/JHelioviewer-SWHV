package org.helioviewer.gl3d.plugin.vectors;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.plugin.vectors.data.VectorData;
import org.helioviewer.gl3d.plugin.vectors.data.VectorField;
import org.helioviewer.gl3d.scenegraph.GL3DMergeMesh;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DTrianglesCone;

/**
 * Scene Graph representation of a Vectorfield mapped onto the surface of the
 * sun. It accelerates the rendering of multiple vectors by merging them
 * together into a GL3DMergeMesh, where all vectors are drawn in one draw call.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DVectorfield extends GL3DMergeMesh {
    private VectorField vectorfield;

    private static final double ARCSEC2RAD = Math.PI / 648000.0;
    private static final double DEG2RAD = Math.PI / 180.0;

    public GL3DVectorfield(VectorField vectorfield) {
        super("Vectorfield");
        this.vectorfield = vectorfield;

        Log.debug("GL3DVectorfield: " + vectorfield.posArcsec);
    }

    public void shapeInit(GL3DState state) {
        this.initVectorfield(state);

        super.shapeInit(state);
    }

    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }

    private void initVectorfield(GL3DState state) {
        int counter = 0;

        int width;
        int height;
        double startTheta;
        double startPhi;
        double deltaTheta;
        double deltaPhi;

        if (this.vectorfield == null) {
            width = 200;
            height = 100;
            startTheta = -Math.PI / 1.8;
            startPhi = -Math.PI / 1.8;
            deltaTheta = Math.PI / (1.6 * height);
            deltaPhi = Math.PI / (1.6 * width);
        } else {
            width = (int) vectorfield.sizePixel.x;
            height = (int) vectorfield.sizePixel.y;

            if (vectorfield.posArcsec == null) {
                startTheta = -Math.PI / 2;
                startPhi = -Math.PI / 2;
            } else {
                startTheta = vectorfield.posArcsec.y * ARCSEC2RAD;
                startPhi = vectorfield.posArcsec.x * ARCSEC2RAD;
            }
            if (vectorfield.sizeArcsec == null) {
                deltaTheta = Math.PI / height;
                deltaPhi = Math.PI / width;
            } else {
                deltaTheta = vectorfield.sizeArcsec.y * ARCSEC2RAD / height;
                deltaPhi = vectorfield.sizeArcsec.x * ARCSEC2RAD / width;
            }
        }

        double avgLength = 0.0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                VectorData v = null;
                GL3DMesh vectorMesh;
                if (this.vectorfield != null) {
                    int vectorIndex = y * width + x;
                    v = this.vectorfield.vectors[0][vectorIndex];
                } else {
                    vectorMesh = new GL3DTrianglesCone(Constants.SunRadius / 700.0, Constants.SunRadius / 10.0, 8, new GL3DVec4f(1.0f, 1.f, 0.f, 1.f));
                }
                if (v != null) {
                    avgLength += v.length;

                    if (v.length < 900) {
                        continue;
                    }

                    GL3DVec4f color;
                    if (v.inclination < 90) {// Outgoing
                        color = new GL3DVec4f(0.0f, 0.8f, 0.f, 1.f);
                    } else {
                        color = new GL3DVec4f(1.0f, 0.2f, 0.2f, 1.f);
                    }
                    vectorMesh = new GL3DTrianglesCone(Constants.SunRadius / 700.0, (v.length / 1000) * Constants.SunRadius / 50, 8, color);
                } else {
                    continue;
                }

                // //Vertical drift
                // vectorMesh.modelView().rotate(startTheta, 1., 0., 0.0);
                // //Horizontal drift
                // vectorMesh.modelView().rotate(startPhi, 0., 1., 0.0);
                // Vertical drift
                vectorMesh.modelView().rotate(startTheta + y * deltaTheta, 1., 0., 0.0);
                // Horizontal drift
                vectorMesh.modelView().rotate(startPhi + x * deltaPhi, 0., 1., 0.0);

                vectorMesh.modelView().translate(0, 0, Constants.SunRadius);

                if (v != null) {
                    // Make the vector rotate according to inclination and
                    // azimuth
                    if (v.inclination > 90)
                        vectorMesh.modelView().rotate((-(180 - v.inclination) + 90) * DEG2RAD, 1., 0., 0.);
                    else
                        vectorMesh.modelView().rotate((-(90.0 - v.inclination)) * DEG2RAD, 1., 0., 0.);
                    vectorMesh.modelView().rotate(v.azimuth * DEG2RAD, 0., 0., 1.);
                }
                counter++;
                this.addMesh(vectorMesh);
            }
        }

        Log.debug("GL3DVectorfield: Average Vector Length = " + (avgLength / counter));
        Log.debug("GL3DVectorfield: Created " + counter + " Vector Cones");
    }
}
