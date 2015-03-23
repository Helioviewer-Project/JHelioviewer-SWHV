package org.helioviewer.gl3d.camera;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DState;

public class GL3DCameraFOV extends GL3DNode {

    private final double width;
    private final double height;

    private final double epsilon = 0.01;
    private double scale;

    public GL3DCameraFOV(double width, double height) {
        this("CameraFOV", width, height);
    }

    public GL3DCameraFOV(String name, double width, double height) {
        super(name);
        this.width = width;
        this.height = height;
    }

    @Override
    public void shapeDraw(GL3DState state) {
        this.markAsChanged();

        GL2 gl = state.gl;
        gl.glLineWidth(2.5f);
        gl.glColor3d(0.0f, 1.0f, 0.0f);
        gl.glBegin(GL2.GL_LINE_LOOP);

        double bw = width * scale / 2.;
        double bh = height * scale / 2.;
        int subdivisions = 10;
        for (int i = 0; i <= subdivisions; i++) {
            double x = -bw + 2 * bw / subdivisions * i;
            double y = bh;
            double z = epsilon;
            if (x * x + y * y < 1) {
                z += Math.sqrt(1 - x * x - y * y);
            }
            gl.glVertex3d(x, y, z);
        }
        for (int i = 0; i <= subdivisions; i++) {
            double x = bw;
            double y = bh - 2 * bh / subdivisions * i;
            double z = epsilon;
            if (x * x + y * y < 1) {
                z += Math.sqrt(1 - x * x - y * y);
            }
            gl.glVertex3d(x, y, z);
        }
        for (int i = 0; i <= subdivisions; i++) {
            double x = bw - 2 * bw / subdivisions * i;
            double y = -bh;
            double z = epsilon;
            if (x * x + y * y < 1) {
                z += Math.sqrt(1 - x * x - y * y);
            }
            gl.glVertex3d(x, y, z);
        }
        for (int i = 0; i <= subdivisions; i++) {
            double x = -bw;
            double y = -bh + 2 * bh / subdivisions * i;
            double z = epsilon;
            if (x * x + y * y < 1) {
                z += Math.sqrt(1 - x * x - y * y);
            }
            gl.glVertex3d(x, y, z);
        }
        gl.glEnd();
    }

    public void scale(double s) {
        this.scale = s;
    }

    public void setAngles(double currentB, double currentL) {
        this.m = GL3DMat4d.identity();
        this.m.rotate(-currentL, 0., 1., 0.);
        this.m.rotate(currentB, 1., 0., 0.);
    }

    @Override
    public void shapeDelete(GL3DState state) {
    }

    @Override
    public void shapeInit(GL3DState state) {
    }

    @Override
    public void shapeUpdate(GL3DState state) {
    }

}
