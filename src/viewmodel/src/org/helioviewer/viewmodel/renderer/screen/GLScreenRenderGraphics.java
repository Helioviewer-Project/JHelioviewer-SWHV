package org.helioviewer.viewmodel.renderer.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL2;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.math.Vector3dDouble;
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

    private final GL2 gl;
    private final GLCommonRenderGraphics commonRenderGraphics;
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
    public GLScreenRenderGraphics(GL2 _gl) {
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
    @Override
    public void setColor(Color color) {
        gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLineWidth(float lineWidth) {
        gl.glLineWidth(lineWidth);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawLine(Integer x0, Integer y0, Integer x1, Integer y1) {
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glBegin(GL2.GL_LINES);
        {
            gl.glVertex2i(x0, y0);
            gl.glVertex2i(x1, y1);
        }
        gl.glEnd();

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawRectangle(Integer x, Integer y, Integer width, Integer height) {
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glBegin(GL2.GL_LINE_LOOP);
        {
            gl.glVertex2i(x, y);
            gl.glVertex2i(x, y + height);
            gl.glVertex2i(x + width, y + height);
            gl.glVertex2i(x + width, y);
        }
        gl.glEnd();

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillRectangle(Integer x, Integer y, Integer width, Integer height) {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glVertex2i(x, y);
            gl.glVertex2i(x, y + height);
            gl.glVertex2i(x + width, y + height);
            gl.glVertex2i(x + width, y);
        }
        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawOval(Integer x, Integer y, Integer width, Integer height) {
        gl.glDisable(GL2.GL_TEXTURE_2D);

        int radiusX = width >> 1;
        int radiusY = height >> 1;
        int centerX = x + radiusX;
        int centerY = y + radiusY;

        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; i < edgesPerOval; i++) {
            gl.glVertex2i(centerX + (int) (radiusX * sinOval[i]), centerY + (int) (radiusY * sinOval[(i + (edgesPerOval >> 2)) & (edgesPerOval - 1)]));
        }
        gl.glEnd();

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillOval(Integer x, Integer y, Integer width, Integer height) {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

        int radiusX = width >> 1;
        int radiusY = height >> 1;
        int centerX = x + radiusX;
        int centerY = y + radiusY;

        if (width == height) {
            gl.glPointSize(width);

            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex2i(centerX, centerY);
            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
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
    @Override
    public void drawPolygon(Integer[] xCoords, Integer[] yCoords) {
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; i < xCoords.length; i++) {
            gl.glVertex2i(xCoords[i], yCoords[i]);
        }
        gl.glEnd();

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawPolygon(Vector2dInt[] points) {
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; i < points.length; i++) {
            gl.glVertex2i(points[i].getX(), points[i].getY());
        }
        gl.glEnd();

        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillPolygon(Integer[] xCoords, Integer[] yCoords) {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

        gl.glBegin(GL2.GL_POLYGON);
        for (int i = 0; i < xCoords.length; i++) {
            gl.glVertex2i(xCoords[i], yCoords[i]);
        }
        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillPolygon(Vector2dInt[] points) {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

        gl.glBegin(GL2.GL_POLYGON);
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
    @Override
    public void drawImage(BufferedImage image, Integer x, Integer y, Integer width, Integer height) {
        commonRenderGraphics.bindImage(image);

        gl.glColor3f(1.0f, 1.0f, 1.0f);

        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex2i(x, y);
            gl.glTexCoord2f(0.0f, 1.0f);
            gl.glVertex2i(x, y + height);
            gl.glTexCoord2f(1.0f, 1.0f);
            gl.glVertex2i(x + width, y + height);
            gl.glTexCoord2f(1.0f, 0.0f);
            gl.glVertex2i(x + width, y);
        }
        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawText(String text, Integer x, Integer y) {
        commonRenderGraphics.bindString(text, font);

        Vector2dInt size = commonRenderGraphics.getStringDisplaySize(text, font);

        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex2i(x, y);
            gl.glTexCoord2f(0.0f, 1.0f);
            gl.glVertex2i(x, y + size.getY());
            gl.glTexCoord2f(1.0f, 1.0f);
            gl.glVertex2i(x + size.getX(), y + size.getY());
            gl.glTexCoord2f(1.0f, 0.0f);
            gl.glVertex2i(x + size.getX(), y);
        }
        gl.glEnd();
    }

    @Override
    public void drawImage3d(BufferedImage image, Integer x, Integer y, Integer z) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawImage3d(BufferedImage image, Integer x, Integer y, Integer z, float scale) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawImage3d(BufferedImage image, Integer x, Integer y, Integer z, Integer width, Integer height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void fillPolygon(Vector3dDouble[] points) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawLine3d(Integer x0, Integer y0, Integer z0, Integer x1, Integer y1, Integer z1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawLine3d(Vector3dDouble p0, Vector3dDouble p1) {
        // TODO Auto-generated method stub

    }

}
