package org.helioviewer.jhv.opengl;

import java.awt.Point;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

public class GLHelper {

    public static void initCircleFront(GL3 gl, GLSLShape circle, double x, double y, double r, int segments, byte[] color) {
        int no_points = 2 * (segments + 1);
        BufVertex vexBuf = new BufVertex(no_points * GLSLShape.stride);
        for (int i = 0; i <= segments; ++i) {
            double t = 2 * Math.PI * i / segments;
            vexBuf.putVertex((float) (x + Math.sin(t) * r), (float) (y + Math.cos(t) * r), 0, 1, color);
            vexBuf.putVertex((float) x, (float) y, 0, 1, color);
        }
        circle.setVertex(gl, vexBuf);
    }

    public static void initRectangleFront(GL3 gl, GLSLShape rectangle, double x0, double y0, double w, double h, byte[] color) {
        BufVertex vexBuf = new BufVertex(4 * GLSLShape.stride);
        vexBuf.putQuad2DStrip((float) x0, (float) y0, (float) (x0 + w), (float) (y0 + h), color);
        rectangle.setVertex(gl, vexBuf);
    }

    public static Point GL2AWTPoint(int x, int y) {
        return new Point((int) (x / GLInfo.pixelScale[0] + .5), (int) (y / GLInfo.pixelScale[1] + .5));
    }

    public static GLCanvas createGLCanvas() {
        GLCanvas canvas = null;
        try {
            GLProfile profile = GLProfile.get(GLProfile.GL3);
            GLCapabilities capabilities = getGLCapabilities(profile);
            canvas = new GLCanvas(capabilities);
            // GUI events can lead to context destruction and invalidation of GL objects and state
            canvas.setSharedAutoDrawable(getSharedDrawable(profile, capabilities));
        } catch (Exception e) {
            String msg = e.getMessage();
            GLInfo.glVersionError(msg == null ? "Unknown OpenGL error." : msg);
        }
        return canvas;
    }

    private static GLCapabilities getGLCapabilities(GLProfile profile) {
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(GLInfo.GLSAMPLES);
        capabilities.setRedBits(8);
        capabilities.setGreenBits(8);
        capabilities.setBlueBits(8);
        capabilities.setAlphaBits(8);
        capabilities.setDepthBits(32);
        return capabilities;
    }

    private static GLAutoDrawable getSharedDrawable(GLProfile profile, GLCapabilities capabilities) {
        GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(profile).createDummyAutoDrawable(null, true, capabilities, null);
        sharedDrawable.display();
        return sharedDrawable;
    }

}
