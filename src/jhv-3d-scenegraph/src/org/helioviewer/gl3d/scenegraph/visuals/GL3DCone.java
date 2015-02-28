package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DCone extends GL3DMesh {

    private int detail;
    private double radius;
    private double height;

    private GL3DVec4d color;

    public GL3DCone(double radius, double height, int detail, GL3DVec4f color) {
        this("Cone", radius, height, detail, color);
    }

    public GL3DCone(String name, double radius, double height, int detail, GL3DVec4f color) {
        super(name, new GL3DVec4f(1, 1, 1, 1));
        this.color = new GL3DVec4d(color.x, color.y, color.z, color.w);
        this.radius = radius;
        this.detail = detail;
        this.height = height;

        if (detail < 3) {
            throw new IllegalArgumentException("Cylinder must have at least detail=3!");
        }
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        positions.add(new GL3DVec3d(0, 0, this.height));
        indices.add(0);
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
            indices.add(i + 1);
        }

        return GL3DMeshPrimitive.TRIANGLE_FAN;
    }

}
