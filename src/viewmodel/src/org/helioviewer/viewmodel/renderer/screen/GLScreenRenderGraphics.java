package org.helioviewer.viewmodel.renderer.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.renderer.GLCommonRenderGraphics;

/**
 * Implementation of ScreenRenderGraphics, using OpenGL for drawing.
 * 
 * <p>
 * Maps all methods to corresponding OpenGL methods.
 * 
 * @author Markus Langenberg
 * 
 * */
public class GLScreenRenderGraphics extends AbstractScreenRenderGraphics {

    private static final int edgesPerOval = 32; // has to be power of two
    private static float[] sinOval;

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
     */
    public GLScreenRenderGraphics(GL _gl) {
        gl = _gl;
        commonRenderGraphics = new GLCommonRenderGraphics(_gl);

        if (sinOval == null) {
            sinOval = new float[edgesPerOval];
            for (int i = 0; i < edgesPerOval; i++) {
                sinOval[i] = (float) Math.sin(Math.PI * 2 * i / edgesPerOval);
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
    public void drawLine(Integer x0, Integer y0, Integer x1, Integer y1) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_LINES);

        gl.glVertex2i(x0, y0);
        gl.glVertex2i(x1, y1);

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void drawRectangle(Integer x, Integer y, Integer width, Integer height) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_LINE_LOOP);

        gl.glVertex2i(x, y);
        gl.glVertex2i(x, y + height);
        gl.glVertex2i(x + width, y + height);
        gl.glVertex2i(x + width, y);

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void fillRectangle(Integer x, Integer y, Integer width, Integer height) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        gl.glBegin(GL.GL_QUADS);

        gl.glVertex2i(x, y);
        gl.glVertex2i(x, y + height);
        gl.glVertex2i(x + width, y + height);
        gl.glVertex2i(x + width, y);

        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     */
    public void drawOval(Integer x, Integer y, Integer width, Integer height) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        int radiusX = width >> 1;
        int radiusY = height >> 1;
        int centerX = x + radiusX;
        int centerY = y + radiusY;

        gl.glBegin(GL.GL_LINE_LOOP);

        for (int i = 0; i < edgesPerOval; i++) {
            gl.glVertex2i(centerX + (int) (radiusX * sinOval[i]), centerY + (int) (radiusY * sinOval[(i + (edgesPerOval >> 2)) & (edgesPerOval - 1)]));
        }

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void fillOval(Integer x, Integer y, Integer width, Integer height) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        int radiusX = width >> 1;
        int radiusY = height >> 1;
        int centerX = x + radiusX;
        int centerY = y + radiusY;

        if (width == height) {
            gl.glPointSize(width);

            gl.glBegin(GL.GL_POINTS);
            gl.glVertex2i(centerX, centerY);
            gl.glEnd();
        } else {
            gl.glBegin(GL.GL_TRIANGLE_FAN);

            gl.glVertex2i(centerX, centerY);

            for (int i = 0; i < edgesPerOval; i++) {
                gl.glVertex2f(centerX + (radiusX * sinOval[i]), centerY + (radiusY * sinOval[(i + (edgesPerOval >> 2)) & (edgesPerOval - 1)]));
            }

            gl.glVertex2f(centerX, centerY + radiusY);

            gl.glEnd();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Integer[] xCoords, Integer[] yCoords) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_LINE_LOOP);

        for (int i = 0; i < xCoords.length; i++) {
            gl.glVertex2i(xCoords[i], yCoords[i]);
        }

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Vector2dInt[] points) {
        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glBegin(GL.GL_LINE_LOOP);

        for (int i = 0; i < points.length; i++) {
            gl.glVertex2i(points[i].getX(), points[i].getY());
        }

        gl.glEnd();

        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    public void fillPolygon(Integer[] xCoords, Integer[] yCoords) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        gl.glBegin(GL.GL_POLYGON);

        for (int i = 0; i < xCoords.length; i++) {
            gl.glVertex2i(xCoords[i], yCoords[i]);
        }

        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     */
    public void fillPolygon(Vector2dInt[] points) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

        gl.glBegin(GL.GL_POLYGON);

        for (int i = 0; i < points.length; i++) {
            gl.glVertex2i(points[i].getX(), points[i].getY());
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
    public void drawImage(BufferedImage image, Integer x, Integer y, Integer width, Integer height) {

        commonRenderGraphics.bindScalingShader();
        commonRenderGraphics.bindImage(image);

        gl.glColor3f(1.0f, 1.0f, 1.0f);

        gl.glBegin(GL.GL_QUADS);

        commonRenderGraphics.setTexCoord(0.0f, 0.0f);
        gl.glVertex2i(x, y);
        commonRenderGraphics.setTexCoord(0.0f, 1.0f);
        gl.glVertex2i(x, y + height);
        commonRenderGraphics.setTexCoord(1.0f, 1.0f);
        gl.glVertex2i(x + width, y + height);
        commonRenderGraphics.setTexCoord(1.0f, 0.0f);
        gl.glVertex2i(x + width, y);

        gl.glEnd();

        commonRenderGraphics.unbindScalingShader();
    }

    /**
     * {@inheritDoc}
     */
    public void drawText(String text, Integer x, Integer y) {
        commonRenderGraphics.bindScalingShader();
        commonRenderGraphics.bindString(text, font);

        Vector2dInt size = commonRenderGraphics.getStringDisplaySize(text, font);

        gl.glBegin(GL.GL_QUADS);

        commonRenderGraphics.setTexCoord(0.0f, 0.0f);
        gl.glVertex2i(x, y);
        commonRenderGraphics.setTexCoord(0.0f, 1.0f);
        gl.glVertex2i(x, y + size.getY());
        commonRenderGraphics.setTexCoord(1.0f, 1.0f);
        gl.glVertex2i(x + size.getX(), y + size.getY());
        commonRenderGraphics.setTexCoord(1.0f, 0.0f);
        gl.glVertex2i(x + size.getX(), y);

        gl.glEnd();

        commonRenderGraphics.unbindScalingShader();
    }
}
