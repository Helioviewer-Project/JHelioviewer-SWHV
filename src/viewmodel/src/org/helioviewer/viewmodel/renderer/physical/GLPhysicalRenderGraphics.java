package org.helioviewer.viewmodel.renderer.physical;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector3dDouble;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.viewmodel.renderer.GLCommonRenderGraphics;
import org.helioviewer.viewmodel.view.View;

/**
 * Implementation of PhyscialRenderGraphics, using OpenGL for drawing.
 * 
 * <p>
 * Maps all methods to corresponding OpenGL methods.
 * 
 * @author Markus Langenberg
 * 
 * */
public class GLPhysicalRenderGraphics extends AbstractPhysicalRenderGraphics {

    private static final int edgesPerOval = 32; // has to be power of two
    private static double[] sinOval;

    private GL gl;
    private GLCommonRenderGraphics commonRenderGraphics;
    private Font font;

    /**
     * Default constructor.
     * 
     * <p>
     * The caller has to provide a gl object, which can be used by this
     * renderer.
     * 
     * @param _gl
     *            gl object, that should be used for drawing.
     * @param view
     *            View to access information about the physical coordinate
     *            system.
     */
    public GLPhysicalRenderGraphics(GL _gl, View view) {
        super(view);
        gl = _gl;
        commonRenderGraphics = new GLCommonRenderGraphics(_gl);

        if (sinOval == null) {
            sinOval = new double[edgesPerOval];
            for (int i = 0; i < edgesPerOval; i++) {
                sinOval[i] = Math.sin(Math.PI * 2 * i / edgesPerOval);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setColor(Color color) {
        gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    /**
     * {@inheritDoc}
     */
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * {@inheritDoc}
     */
    public void setLineWidth(float lineWidth) {
        gl.glLineWidth(lineWidth);
    }

    /**
     * {@inheritDoc}
     */
    public void drawLine(Double x0, Double y0, Double x1, Double y1) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_LINES);

        gl.glVertex2d(x0, -y0);
        gl.glVertex2d(x1, -y1);

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void drawRectangle(Double x, Double y, Double width, Double height) {
        y = -y;
        gl.glDisable(GL.GL_TEXTURE_2D);

        double width2 = width * 0.5;
        double height2 = height * 0.5;

        gl.glBegin(GL.GL_LINE_LOOP);

        gl.glVertex2d(x - width2, y - height2);
        gl.glVertex2d(x - width2, y + height2);
        gl.glVertex2d(x + width2, y + height2);
        gl.glVertex2d(x + width2, y - height2);

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void fillRectangle(Double x, Double y, Double width, Double height) {
        y = -y;
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        double width2 = width * 0.5;
        double height2 = height * 0.5;

        gl.glBegin(GL.GL_QUADS);

        gl.glVertex2d(x - width2, y - height2);
        gl.glVertex2d(x - width2, y + height2);
        gl.glVertex2d(x + width2, y + height2);
        gl.glVertex2d(x + width2, y - height2);

        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     */
    public void drawOval(Double x, Double y, Double width, Double height) {
        y = -y;
        gl.glDisable(GL.GL_TEXTURE_2D);

        double radiusX = width * 0.5;
        double radiusY = height * 0.5;

        gl.glBegin(GL.GL_LINE_LOOP);

        for (int i = 0; i < edgesPerOval; i++) {
            gl.glVertex2d(x + (int) (radiusX * sinOval[i]), y + (int) (radiusY * sinOval[(i + (edgesPerOval >> 2)) & (edgesPerOval - 1)]));
        }

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void fillOval(Double x, Double y, Double width, Double height) {
        y = -y;
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        double radiusX = width * 0.5;
        double radiusY = height * 0.5;

        if (width == height) {
            gl.glPointSize(width.floatValue());

            gl.glBegin(GL.GL_POINTS);
            gl.glVertex2d(x, y);
            gl.glEnd();
        } else {
            gl.glBegin(GL.GL_TRIANGLE_FAN);

            gl.glVertex2d(x, y);

            for (int i = 0; i < edgesPerOval; i++) {
                gl.glVertex2d(x + (radiusX * sinOval[i]), y + (radiusY * sinOval[(i + (edgesPerOval >> 2)) & (edgesPerOval - 1)]));
            }

            gl.glVertex2d(x, y + radiusY);

            gl.glEnd();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Double[] xCoords, Double[] yCoords) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_LINE_LOOP);

        for (int i = 0; i < xCoords.length; i++) {
            gl.glVertex2d(xCoords[i], -yCoords[i]);
        }

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Vector2dDouble[] points) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_LINE_LOOP);

        for (int i = 0; i < points.length; i++) {
            gl.glVertex2d(points[i].getX(), -points[i].getY());
        }

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void fillPolygon(Double[] xCoords, Double[] yCoords) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        gl.glBegin(GL.GL_POLYGON);

        for (int i = 0; i < xCoords.length; i++) {
            gl.glVertex2d(xCoords[i], -yCoords[i]);
        }

        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     */
    public void fillPolygon(Vector2dDouble[] points) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        gl.glBegin(GL.GL_POLYGON);

        for (int i = 0; i < points.length; i++) {
            gl.glVertex2d(points[i].getX(), -points[i].getY());
        }

        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Note, that the renderer buffers recently seen images, so it a good idea
     * to use the same image object every call, if the image data does not
     * change.
     */
    public void drawImage(BufferedImage image, Double x, Double y) {
        drawImage(image, x, y, 1.0f);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Note, that the renderer buffers recently seen images, so it a good idea
     * to use the same image object every call, if the image data does not
     * change.
     */
    public void drawImage(BufferedImage image, Double x, Double y, float scale) {
        Vector2dDouble imageSize = convertScreenToPhysical(image.getWidth(), image.getHeight());
        drawImage(image, x, y, imageSize.getX() * scale, imageSize.getY() * scale);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Note, that the renderer buffers recently seen images, so it a good idea
     * to use the same image object every call, if the image data does not
     * change.
     */
    public void drawImage(BufferedImage image, Double x, Double y, Double width, Double height) {
        y = -y;

        // commonRenderGraphics.bindScalingShader();
        commonRenderGraphics.bindImage(image);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
        double width2 = width * 0.5;
        double height2 = height * 0.5;

        gl.glBegin(GL.GL_QUADS);

        // commonRenderGraphics.setTexCoord(0.0f, 0.0f);
        gl.glTexCoord2d(0, 0);
        gl.glVertex2d(x - width2, y - height2);
        // commonRenderGraphics.setTexCoord(0.0f, 1.0f);
        gl.glTexCoord2d(0, 1);
        gl.glVertex2d(x - width2, y + height2);
        // commonRenderGraphics.setTexCoord(1.0f, 1.0f);
        gl.glTexCoord2d(1, 1);
        gl.glVertex2d(x + width2, y + height2);
        // commonRenderGraphics.setTexCoord(1.0f, 0.0f);
        gl.glTexCoord2d(1, 0);
        gl.glVertex2d(x + width2, y - height2);

        gl.glEnd();

        // commonRenderGraphics.unbindScalingShader();
    }

    /**
     * {@inheritDoc}
     */
    public void drawText(String text, Double x, Double y) {
        y = -y;

        commonRenderGraphics.bindScalingShader();
        commonRenderGraphics.bindString(text, font);

        Vector2dDouble size = convertScreenToPhysical(commonRenderGraphics.getStringDisplaySize(text, font));

        double width2 = size.getX() * 0.5;
        double height2 = size.getY() * 0.5;

        gl.glBegin(GL.GL_QUADS);

        commonRenderGraphics.setTexCoord(0.0f, 0.0f);
        gl.glVertex2d(x - width2, y - height2);
        commonRenderGraphics.setTexCoord(0.0f, 1.0f);
        gl.glVertex2d(x - width2, y + height2);
        commonRenderGraphics.setTexCoord(1.0f, 1.0f);
        gl.glVertex2d(x + width2, y + height2);
        commonRenderGraphics.setTexCoord(1.0f, 0.0f);
        gl.glVertex2d(x + width2, y - height2);

        gl.glEnd();

        commonRenderGraphics.unbindScalingShader();
    }

    @Override
    public void drawImage3d(BufferedImage image, Double x, Double y, Double z) {
        // TODO Auto-generated method stub
        drawImage3d(image, x, y, z, 1.0f);
    }

    @Override
    public void drawImage3d(BufferedImage image, Double x, Double y, Double z, float scale) {
        Vector2dDouble imageSize = convertScreenToPhysical(image.getWidth(), image.getHeight());
        drawImage3d(image, x, y, z, imageSize.getX() * scale, imageSize.getY() * scale);

    }

    @Override
    public void drawImage3d(BufferedImage image, Double x, Double y, Double z, Double width, Double height) {
        y = -y;

        gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);
        gl.glEnable(GL.GL_FRAGMENT_PROGRAM_ARB);

        commonRenderGraphics.bindImage(image);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
        double width2 = width / 2.0;
        double height2 = height / 2.0;

        GL3DVec3d sourceDir = new GL3DVec3d(0, 0, 1);
        GL3DVec3d targetDir = new GL3DVec3d(x, y, z);

        double angle = Math.acos(sourceDir.dot(targetDir) / (sourceDir.length() * targetDir.length()));
        GL3DVec3d axis = sourceDir.cross(targetDir);
        axis.normalize();
        GL3DMat4d r = GL3DMat4d.rotation(angle, axis);
        r.setTranslation(x, y, z);

        gl.glDisable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glEnable(GL.GL_TEXTURE_2D);

        GL3DVec3d p0 = new GL3DVec3d(-width2, -height2, 0);
        GL3DVec3d p1 = new GL3DVec3d(-width2, height2, 0);
        GL3DVec3d p2 = new GL3DVec3d(width2, height2, 0);
        GL3DVec3d p3 = new GL3DVec3d(width2, -height2, 0);
        p0 = r.multiply(p0);
        p1 = r.multiply(p1);
        p2 = r.multiply(p2);
        p3 = r.multiply(p3);

        gl.glBegin(GL.GL_QUADS);

        commonRenderGraphics.setTexCoord(0.0f, 0.0f);
        gl.glVertex3d(p0.x, p0.y, p0.z);
        commonRenderGraphics.setTexCoord(0.0f, 1.0f);
        gl.glVertex3d(p1.x, p1.y, p1.z);
        commonRenderGraphics.setTexCoord(1.0f, 1.0f);
        gl.glVertex3d(p2.x, p2.y, p2.z);
        commonRenderGraphics.setTexCoord(1.0f, 0.0f);
        gl.glVertex3d(p3.x, p3.y, p3.z);

        gl.glEnd();

        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glEnable(GL.GL_BLEND);
        gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
        gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);

        commonRenderGraphics.unbindScalingShader();

    }

    @Override
    public void fillPolygon(Vector3dDouble[] points) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_CULL_FACE);

        gl.glBegin(GL.GL_POLYGON);

        for (int i = 0; i < points.length; i++) {
            gl.glVertex3d(points[i].getX(), -points[i].getY(), points[i].getZ());
        }

        gl.glEnd();

        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_LIGHTING);

    }

    @Override
    public void drawLine3d(Double x0, Double y0, Double z0, Double x1, Double y1, Double z1) {
        // TODO Auto-generated method stub
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINES);

        gl.glVertex3d(x0, -y0, z0);
        gl.glVertex3d(x1, -y1, z1);

        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glDisable(GL.GL_LINE_SMOOTH);

    }

    @Override
    public void drawLine3d(Vector3dDouble p0, Vector3dDouble p1) {
        drawLine3d(p0.getX(), p0.getY(), p0.getZ(), p1.getX(), p1.getY(), p1.getZ());
        // TODO Auto-generated method stub

    }

    public void startDrawLines() {
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        // gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBegin(GL.GL_LINES);
    }

    public void stopDrawLines() {
        gl.glEnd();
        gl.glDisable(GL.GL_LINE_SMOOTH);

    }

    public void drawLines3d(Double x0, Double y0, Double z0, Double x1, Double y1, Double z1) {
        gl.glVertex3d(x0, y0, z0);
        gl.glVertex3d(x1, y1, z1);

    }

    public GL getGL() {
        return gl;
    }
}
