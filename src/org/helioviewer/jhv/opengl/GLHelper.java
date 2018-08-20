package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.awt.Point;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.position.Position;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;
//import com.jogamp.opengl.awt.GLCanvas;

public class GLHelper {

    public static void initCircleFront(GL2 gl, GLSLShape circle, double x, double y, double r, int segments, byte[] color) {
        int no_points = 2 * 4 * (segments + 1);
        Buf vexBuf = new Buf(no_points * GLSLShape.stride);
        for (int i = 0; i <= segments; ++i) {
            double t = 2 * Math.PI * i / segments;
            vexBuf.put4f((float) (x + Math.sin(t) * r), (float) (y + Math.cos(t) * r), 0, 1).put4b(color);
            vexBuf.put4f((float) x, (float) y, 0, 1).put4b(color);
        }
        circle.setData(gl, vexBuf);
    }

    public static void initRectangleFront(GL2 gl, GLSLShape rectangle, double x0, double y0, double w, double h, byte[] color) {
        Buf vexBuf = new Buf(4 * GLSLShape.stride);
        float x1 = (float) (x0 + w);
        float y1 = (float) (y0 + h);

        vexBuf.put4f((float) x0, (float) y0, 0, 1).put4b(color);
        vexBuf.put4f(x1, (float) y0, 0, 1).put4b(color);
        vexBuf.put4f((float) x0, y1, 0, 1).put4b(color);
        vexBuf.put4f(x1, y1, 0, 1).put4b(color);

        rectangle.setData(gl, vexBuf);
    }

    public static Point GL2AWTPoint(int x, int y) {
        return new Point((int) (x / GLInfo.pixelScaleFloat[0]), (int) (y / GLInfo.pixelScaleFloat[1]));
    }

    public static Dimension GL2AWTDimension(int x, int y) {
        return new Dimension((int) (x / GLInfo.pixelScaleFloat[0]), (int) (y / GLInfo.pixelScaleFloat[1]));
    }

    public static Vec2 drawVertex(Camera camera, Viewport vp, Vec3 current, Vec2 previous, Buf vexBuf, byte[] color) {
        Position viewpoint = camera.getViewpoint();
        Vec3 pt = viewpoint.toQuat().rotateVector(current);
        Vec2 tf = Display.mode.xform.transform(viewpoint, pt, Display.mode.scale);

        float x;
        float y = (float) tf.y;
        if (previous != null && Math.abs(previous.x - tf.x) > 0.5) {
            if (tf.x <= 0 && previous.x >= 0) {
                x = (float) (0.5 * vp.aspect);
                vexBuf.put4f(x, y, 0, 1).put4b(color);

                vexBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
                vexBuf.put4f(-x, y, 0, 1).put4b(Colors.Null);

                vexBuf.put4f(-x, y, 0, 1).put4b(color);
            } else if (tf.x >= 0 && previous.x <= 0) {
                x = (float) (-0.5 * vp.aspect);
                vexBuf.put4f(x, y, 0, 1).put4b(color);

                vexBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
                vexBuf.put4f(-x, y, 0, 1).put4b(Colors.Null);

                vexBuf.put4f(-x, y, 0, 1).put4b(color);
            }
        }
        x = (float) (tf.x * vp.aspect);
        vexBuf.put4f(x, y, 0, 1).put4b(color);
        return tf;
    }
    /*
        public static GLCanvas createGLCanvas() {
            GLProfile profile = GLProfile.getDefault();
            GLCapabilities capabilities = getGLCapabilities(profile);
            GLCanvas canvas = new GLCanvas(capabilities);

            // GUI events can lead to context destruction and invalidation of GL objects and state
            canvas.setSharedAutoDrawable(getSharedDrawable(profile, capabilities));

            return canvas;
        }
    */
    public static GLWindow createGLWindow() {
        GLProfile profile = GLProfile.getGL2GL3();
        GLCapabilities capabilities = getGLCapabilities(profile);
        GLWindow window = GLWindow.create(capabilities);

        // GUI events can lead to context destruction and invalidation of GL objects and state
        window.setSharedAutoDrawable(getSharedDrawable(profile, capabilities));

        return window;
    }

    private static GLCapabilities getGLCapabilities(GLProfile profile) {
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(GLInfo.GLSAMPLES);
        return capabilities;
    }

    private static GLAutoDrawable getSharedDrawable(GLProfile profile, GLCapabilities capabilities) {
        GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(profile).createDummyAutoDrawable(null, true, capabilities, null);
        sharedDrawable.display();
        return sharedDrawable;
    }

}
