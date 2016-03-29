package org.helioviewer.jhv.camera.annotate;

import java.awt.Color;
import java.awt.Point;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Displayer.DisplayMode;
import org.helioviewer.jhv.renderable.components.RenderableGrid.GridChoiceType;

import com.jogamp.opengl.GL2;

public abstract class AbstractAnnotateable implements Annotateable {

    private static final Color colorActive = Color.red;
    private static final Color colorDrag = Color.yellow;
    private static final Color colorBase = Color.blue;

    protected static final float[] activeColor = new float[] { colorActive.getRed() / 255f, colorActive.getGreen() / 255f, colorActive.getBlue() / 255f };
    protected static final float[] dragColor = new float[] { colorDrag.getRed() / 255f, colorDrag.getGreen() / 255f, colorDrag.getBlue() / 255f };
    protected static final float[] baseColor = new float[] { colorBase.getRed() / 255f, colorBase.getGreen() / 255f, colorBase.getBlue() / 255f };

    protected static final float lineWidth = 2;
    protected static final double radius = 1.01;

    protected final Camera camera;

    public AbstractAnnotateable(Camera _camera) {
        camera = _camera;
    }

    protected static Vec3 toSpherical(Vec3 p) {
        Vec3 pt = new Vec3();
        pt.x = p.length();
        pt.y = Math.acos(p.y / pt.x);
        pt.z = Math.atan2(p.x, p.z);

        return pt;
    }

    protected static Vec3 toCart(double x, double y, double z) {
        return new Vec3(x * Math.sin(y) * Math.sin(z),
                x * Math.cos(y),
                x * Math.sin(y) * Math.cos(z));
    }

    protected Vec3 computePoint(Point p) {
        Vec3 pt;
        if (Displayer.mode == DisplayMode.ORTHO) {
            pt = CameraHelper.getVectorFromSphere(camera, Displayer.getActiveViewport(), p);
        } else {
            pt = GridScale.current.transformInverse(GridScale.current.mouseToGrid(p, Displayer.getActiveViewport(), camera, GridChoiceType.VIEWPOINT));
        }
        return pt;
    }

    protected Vec2 drawVertex(GL2 gl, Vec3 current, Vec2 previous) {
        Vec3 pt = camera.getViewpoint().orientation.rotateVector(current);
        Vec2 tf = GridScale.current.transform(pt);
        if (previous != null) {
            if (tf.x <= 0 && previous.x >= 0 && Math.abs(previous.x - tf.x) > 0.5) {
                gl.glVertex2f((float) (0.5 * Displayer.getActiveViewport().aspect), (float) tf.y);
                gl.glEnd();
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex2f((float) (-0.5 * Displayer.getActiveViewport().aspect), (float) tf.y);
                gl.glVertex2f((float) (tf.x * Displayer.getActiveViewport().aspect), (float) tf.y);
            }
            else if (tf.x >= 0 && previous.x <= 0 && Math.abs(previous.x - tf.x) > 0.5) {
                gl.glVertex2f((float) (-0.5 * Displayer.getActiveViewport().aspect), (float) tf.y);
                gl.glEnd();
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex2f((float) (0.5 * Displayer.getActiveViewport().aspect), (float) tf.y);
                gl.glVertex2f((float) (tf.x * Displayer.getActiveViewport().aspect), (float) tf.y);
            }
            else {
                gl.glVertex2f((float) (tf.x * Displayer.getActiveViewport().aspect), (float) tf.y);
            }
        }
        else {
            gl.glVertex2f((float) (tf.x * Displayer.getActiveViewport().aspect), (float) tf.y);
        }
        return tf;
    }
}
