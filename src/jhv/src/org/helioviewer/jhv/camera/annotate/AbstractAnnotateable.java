package org.helioviewer.jhv.camera.annotate;

import java.awt.Color;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Displayer.DisplayMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.renderable.components.RenderableGrid.GridChoiceType;

import com.jogamp.opengl.GL2;

abstract class AbstractAnnotateable implements Annotateable {

    private static final Color colorActive = Color.red;
    private static final Color colorDrag = Color.yellow;
    private static final Color colorBase = Color.blue;

    static final float[] activeColor = new float[] { colorActive.getRed() / 255f, colorActive.getGreen() / 255f, colorActive.getBlue() / 255f };
    static final float[] dragColor = new float[] { colorDrag.getRed() / 255f, colorDrag.getGreen() / 255f, colorDrag.getBlue() / 255f };
    static final float[] baseColor = new float[] { colorBase.getRed() / 255f, colorBase.getGreen() / 255f, colorBase.getBlue() / 255f };

    static final float lineWidth = 2;
    static final double radius = Sun.Radius * 1.01;
    private static final double lineWidthSR = Sun.Radius * 0.002;

    final Camera camera;

    AbstractAnnotateable(Camera _camera) {
        camera = _camera;
    }

    static Vec3 toSpherical(Vec3 p) {
        Vec3 pt = new Vec3();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);

        return pt;
    }

    static Vec3 toCart(double y, double z) {
        return new Vec3(radius * Math.sin(y) * Math.sin(z),
                        radius * Math.cos(y),
                        radius * Math.sin(y) * Math.cos(z));
    }

    Vec3 computePoint(int x, int y) {
        if (Displayer.mode == DisplayMode.ORTHO) {
            return CameraHelper.getVectorFromSphere(camera, Displayer.getActiveViewport(), x, y, camera.getViewpoint().orientation, true);
        } else {
            return GridScale.current.transformInverse(GridScale.current.mouseToGrid(x, y, Displayer.getActiveViewport(), camera, GridChoiceType.VIEWPOINT));
        }
    }

    void interpolatedLineDraw(Viewport vp, GL2 gl, Vec3 p1s, Vec3 p2s, int subdivisions) {
        if (Displayer.mode != Displayer.DisplayMode.ORTHO) {
            gl.glBegin(GL2.GL_LINE_STRIP);

            Vec2 previous = null;
            for (double i = 0; i <= subdivisions; i++) {
                double t = i / subdivisions;
                double y0 = (1 - t) * p1s.y + t * p2s.y;
                double z0 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p0 = toCart(y0, z0);
                p0.y = -p0.y;
                previous = GLHelper.drawVertex(camera, vp, gl, p0, previous);
            }

            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);

            for (double i = 0; i < subdivisions; i++) {
                double t = i / subdivisions;
                double y0 = (1 - t) * p1s.y + t * p2s.y;
                double z0 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p0 = toCart(y0, z0);

                t = (i + 1) / subdivisions;
                double y1 = (1 - t) * p1s.y + t * p2s.y;
                double z1 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p1 = toCart(y1, z1);

                Vec3 p1minusp0 = Vec3.subtract(p1, p0);
                Vec3 v = Vec3.cross(p0, p1minusp0);
                v.normalize();

                v.multiply(lineWidthSR);
                Vec3 p0plusv = Vec3.add(p0, v);
                p0plusv.normalize();
                gl.glVertex3f((float) p0plusv.x, (float) p0plusv.y, (float) p0plusv.z);
                Vec3 p0minusv = Vec3.subtract(p0, v);
                p0minusv.normalize();
                gl.glVertex3f((float) p0minusv.x, (float) p0minusv.y, (float) p0minusv.z);
                if (i == subdivisions - 1) {
                    Vec3 p1plusv = Vec3.add(p1, v);
                    p1plusv.normalize();
                    gl.glVertex3f((float) p1plusv.x, (float) p1plusv.y, (float) p1plusv.z);
                    Vec3 p1minusv = Vec3.subtract(p1, v);
                    p1minusv.normalize();
                    gl.glVertex3f((float) p1minusv.x, (float) p1minusv.y, (float) p1minusv.z);
                }
            }

            gl.glEnd();
        }
    }

}
