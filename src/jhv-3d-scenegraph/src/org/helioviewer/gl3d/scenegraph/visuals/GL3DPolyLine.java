package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DPolyLine extends GL3DMesh {

    private final GL3DVec4d color;

    private final List<GL3DVec3d> points;

    private final GL3DMeshPrimitive meshPrimitive;

    public GL3DPolyLine(List<GL3DVec3d> points, GL3DVec4f color, GL3DMeshPrimitive primitive) {
        super("PolyLine", color);
        if (primitive != GL3DMeshPrimitive.LINE_LOOP && primitive != GL3DMeshPrimitive.LINES && primitive != GL3DMeshPrimitive.LINE_STRIP) {
            throw new IllegalArgumentException("Primitive of a GL3DPolyLine must be one of LINE_LOOP, LINE_STRIP or LINES");
        }

        this.color = new GL3DVec4d(color.x, color.y, color.z, color.w);
        this.points = points;
        this.meshPrimitive = primitive;
    }

    public GL3DPolyLine(List<GL3DVec3d> points, GL3DVec4f color) {
        this(points, color, GL3DMeshPrimitive.LINE_LOOP);
    }

    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        int index = 0;
        for (GL3DVec3d p : points) {
            positions.add(p);
            colors.add(this.color);
            indices.add(index);
            index++;
        }

        return this.meshPrimitive;
    }

    @Override
    public void shapeDraw(GL3DState state) {
        state.gl.glDisable(GL2.GL_LIGHTING);
        super.shapeDraw(state);
        state.gl.glEnable(GL2.GL_LIGHTING);
    }
}
