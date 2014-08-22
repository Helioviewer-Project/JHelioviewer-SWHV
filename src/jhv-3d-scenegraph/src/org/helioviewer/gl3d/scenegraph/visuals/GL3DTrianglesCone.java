package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

/**
 * Visual representation of a cone, but not made up of a triangle fan but of
 * trianges, as to have mergable cones (e.g. to merge multiple cones into a
 * GL3DMergeMesh).
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTrianglesCone extends GL3DMesh {

    private int detail;
    private double radius;
    private double height;

    private GL3DVec4d color;

    public GL3DTrianglesCone(double radius, double height, int detail, GL3DVec4f color) {
        this("Cone", radius, height, detail, color);
    }

    public GL3DTrianglesCone(String name, double radius, double height, int detail, GL3DVec4f color) {
        super(name, new GL3DVec4f(1, 1, 1, 1));
        this.color = new GL3DVec4d(color.x, color.y, color.z, color.w);
        this.radius = radius;
        this.detail = detail;
        this.height = height;

        if (detail < 3) {
            throw new IllegalArgumentException("Cone must have at least detail=3!");
        }
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        positions.add(new GL3DVec3d(0, 0, this.height));
        normals.add(new GL3DVec3d(0, 0, 1));
        colors.add(this.color);

        double dPhi = 2 * Math.PI / this.detail;
        for (int i = 0; i <= this.detail; i++) {
            double phi = i * dPhi;
            double nx = Math.cos(phi);
            double ny = Math.sin(phi);
            double nz = 0;

            double x = nx * radius;
            ;
            double y = ny * radius;
            double z = 0;

            positions.add(new GL3DVec3d(x, y, z));
            normals.add(new GL3DVec3d(nx, ny, nz));
            colors.add(this.color);

            if (i > 0) {
                indices.add(0);
                indices.add(i);
                indices.add(i + 1);
            }
        }

        return GL3DMeshPrimitive.TRIANGLES;
    }

    public void shapeDraw(GL3DState state) {
        state.gl.glDisable(GL2.GL_LIGHTING);
        super.shapeDraw(state);
        state.gl.glEnable(GL2.GL_LIGHTING);

    }
}
