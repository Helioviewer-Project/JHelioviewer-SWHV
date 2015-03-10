package org.helioviewer.gl3d.scenegraph.visuals;

import org.helioviewer.gl3d.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;

public class GL3DArrow extends GL3DGroup {

    public GL3DArrow(double radius, double offset, double length, int detail, GL3DVec4f color) {
        this("Arrow", radius, offset, length, detail, color);
    }

    public GL3DArrow(String name, double radius, double offset, double length, int detail, GL3DVec4f color) {
        super(name);
        GL3DShape cylinder = new GL3DCylinder(radius / 2, length / 2, detail, color);
        cylinder.modelView().setTranslation(0.0, 0, offset + length / 4);
        addNode(cylinder);

        GL3DShape cone = new GL3DCone(radius, length / 2, detail, color);
        cone.modelView().setTranslation(0, 0, offset + length / 2);
        addNode(cone);
    }

    @Override
    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }

}
