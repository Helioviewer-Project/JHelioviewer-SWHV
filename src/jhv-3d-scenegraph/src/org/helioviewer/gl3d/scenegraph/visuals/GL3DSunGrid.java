package org.helioviewer.gl3d.scenegraph.visuals;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DSunGrid extends GL3DGroup {

    public GL3DSunGrid(double radius, GL3DVec4f color) {
        super("Sun Grid");
        GL3DShape yAxis = new GL3DCircle(radius, color);

        GL3DShape xAxis = new GL3DCircle(radius, color);
        xAxis.modelView().rotate(Math.PI / 2, new GL3DVec3d(0, 0, 1));

        GL3DShape zAxis = new GL3DCircle(radius, color);
        zAxis.modelView().rotate(Math.PI / 2, new GL3DVec3d(1, 0, 0));

        addNode(xAxis);
        addNode(yAxis);
        addNode(zAxis);

    }

    public void shapeDraw(GL3DState state) {
        state.gl.glLineWidth(1.0f);
        super.shapeDraw(state);
        state.gl.glLineWidth(1.0f);
    }
}
