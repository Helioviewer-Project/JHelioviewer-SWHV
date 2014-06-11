package org.helioviewer.gl3d.camera;

import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;

public class GL3DCameraFOV extends GL3DMesh {
    public GL3DCameraFOV() {
        this("CameraFOV");
    }

    public GL3DCameraFOV(String name) {
        super(name);
    }

    @Override
    public void shapeDraw(GL3DState state) {
        this.markAsChanged();
        state.gl.glColor3d(1., 0., 0.);
        super.shapeDraw(state);
    }

    @Override
    public void update(GL3DState state) {
        if (!this.isInitialised) {
            this.init(state);
        }
        this.getDrawBits().on(Bit.Wireframe);
        state.pushMV();
        GL3DQuatd differentialRotation = state.getActiveCamera().getLocalRotation();
        this.m = differentialRotation.toMatrix().inverse();
        this.wm = (this.m);
        state.buildInverseAndNormalMatrix();
        this.wmI = new GL3DMat4d(state.getMVInverse());
        this.shapeUpdate(state);
        state.popMV();
    }

    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        int numberOfPositions = 0;
        double helper = 1.28;
        double epsilon = 0.01;
        int beginPositionNumber = numberOfPositions;
        positions.add(new GL3DVec3d(-helper, helper, epsilon));
        colors.add(new GL3DVec4d(1., 0., 0., 1.));
        numberOfPositions++;
        positions.add(new GL3DVec3d(helper, helper, epsilon));
        colors.add(new GL3DVec4d(1., 0., 0., 1.));
        numberOfPositions++;
        positions.add(new GL3DVec3d(helper, -helper, epsilon));
        colors.add(new GL3DVec4d(1., 0., 0., 1.));
        numberOfPositions++;
        positions.add(new GL3DVec3d(-helper, -helper, epsilon));
        colors.add(new GL3DVec4d(1., 0., 0., 1.));
        numberOfPositions++;

        indices.add(beginPositionNumber + 0);
        indices.add(beginPositionNumber + 2);
        indices.add(beginPositionNumber + 1);

        indices.add(beginPositionNumber + 2);
        indices.add(beginPositionNumber + 0);
        indices.add(beginPositionNumber + 3);
        return GL3DMeshPrimitive.TRIANGLES;

    }
}
