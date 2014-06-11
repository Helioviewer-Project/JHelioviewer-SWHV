package org.helioviewer.gl3d.scenegraph.visuals;

import java.awt.Color;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DGrid extends GL3DGroup {
    private final int xticks;
    private final int yticks;
    private final GL3DVec4f color;
    private final GL3DVec4d textColor;
    private String font;

    public GL3DGrid(String name, int xticks, int yticks, GL3DVec4f color, GL3DVec4d textColor) {
        super(name);
        this.xticks = xticks;
        this.yticks = yticks;
        this.color = color;
        this.textColor = textColor;
        this.loadGrid();
    }

    private void loadGrid() {
        GL3DSphere sphere = new GL3DSphere(Constants.SunRadius * 1.02, this.xticks, this.yticks, this.color);
        sphere.getDrawBits().on(Bit.Wireframe);
        this.addNode(sphere);
        double letterSize = 0.05 * Constants.SunRadius;
        double size = Constants.SunRadius * 1.1;
        double zdist = Constants.SunRadius * 0.001;

        int len = 2 * (this.xticks - 1) + this.yticks - 1;
        GL3DVec3d[] positions3D = new GL3DVec3d[len];
        String str[] = new String[len];
        int counter = 0;
        for (int i = 1; i < this.xticks; i++) {
            double angle = i * Math.PI / this.xticks;
            str[counter] = "" + (int) (90 - 1.0 * i / this.xticks * 180);
            positions3D[counter] = new GL3DVec3d(Math.sin(angle) * size, Math.cos(angle) * size, zdist);
            counter++;

            str[counter] = "" + (int) (90 - 1.0 * i / this.xticks * 180);
            positions3D[counter] = new GL3DVec3d(-Math.sin(angle) * size, Math.cos(angle) * size, zdist);
            counter++;
        }
        for (int i = 1; i < this.yticks; i++) {
            str[counter] = "" + (int) (90 - 1.0 * i / (this.yticks / 2.0) * 180);
            double angle = i * Math.PI / (this.yticks / 2.0);
            positions3D[counter] = new GL3DVec3d(Math.cos(angle) * size, 0., Math.sin(angle) * size);
            counter++;
        }
        GL3DText txt = new GL3DText(letterSize, positions3D, str, "Serif", new Color(1.f, 0.f, 0.f, 1.f), new Color(0.f, 0.f, 0.f, 0.f));
        this.addNode(txt);

    }

    private void reloadGrid() {
        this.deleteAll(GL3DState.get());
        this.loadGrid();
    }

    @Override
    public void update(GL3DState state) {
        if (!this.isInitialised) {
            this.init(state);
        }
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
    public void shapeDraw(GL3DState state) {
        this.markAsChanged();
        state.gl.glColor3d(1., 1., 0.);
        super.shapeDraw(state);
    }
}
