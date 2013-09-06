package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DCircle extends GL3DMesh {
    private static final int circleResolution = 128;

    private double radius;
    private GL3DVec4d color;

    public GL3DCircle(double radius, GL3DVec4f color) {
        super("Circle", color);
        this.radius = radius;
        this.color = new GL3DVec4d((double) color.x, (double) color.y, (double) color.z, (double) color.w);
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        double dPhi = Math.PI * 2 / circleResolution;

        // Around Y-Axis
        for (int i = 0; i < circleResolution; i++) {
            double z = Math.sin(i * dPhi) * this.radius;
            double x = Math.cos(i * dPhi) * this.radius;
            double y = 0 * this.radius;

            positions.add(new GL3DVec3d(x, y, z));
            colors.add(this.color);
            indices.add(i);
        }

        return GL3DMeshPrimitive.LINE_LOOP;
    }

    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }
}
