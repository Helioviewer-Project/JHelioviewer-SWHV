package org.helioviewer.gl3d.scenegraph.visuals;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DSunGrid extends GL3DGroup {

    public GL3DSunGrid(double radius, int resolutionX, int resolutionY, GL3DVec4f color) {
        super("Sun Grid");
        
        // Create a Sphere
        GL3DShape sphere = new GL3DSphere(radius, resolutionX, resolutionY, color);
        GL3DShape yAxis = new GL3DCircle(radius * 0.98, color, "yAxis");

        GL3DShape xAxis = new GL3DCircle(radius * 0.98, color, "xAxis");
        xAxis.modelView().rotate(Math.PI / 2, new GL3DVec3d(0, 0, 1));

        GL3DShape zAxis = new GL3DCircle(radius * 0.98, color, "zAxis");
        zAxis.modelView().rotate(Math.PI / 2, new GL3DVec3d(1, 0, 0));
        xAxis.getDrawBits().on(Bit.Wireframe);
        //addNode(xAxis);
        yAxis.getDrawBits().on(Bit.Wireframe);
        //addNode(yAxis);
        zAxis.getDrawBits().on(Bit.Wireframe);
        //addNode(zAxis);
        // Add the sphere as Node
        sphere.getDrawBits().on(Bit.Wireframe);
        //sphere.getDrawBits().on(Bit.Hidden);
        addNode(sphere);
    }

    public void shapeDraw(GL3DState state) {
        state.gl.glLineWidth(1.0f);
        super.shapeDraw(state);
        state.gl.glLineWidth(1.0f);
    }
}
