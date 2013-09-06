package org.helioviewer.viewmodel.renderer.physical;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dDouble;
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

        commonRenderGraphics.bindScalingShader();
        commonRenderGraphics.bindImage(image);

        gl.glColor3f(1.0f, 1.0f, 1.0f);

        double width2 = width * 0.5;
        double height2 = height * 0.5;

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
}
