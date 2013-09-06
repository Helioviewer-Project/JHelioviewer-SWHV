package org.helioviewer.viewmodel.renderer.screen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Implementation of ScreenRenderGraphics operating on Java BufferedImages.
 * 
 * <p>
 * Maps all methods to corresponding methods of Graphics2D.
 * 
 * @author Stephan Pagel
 * 
 */
public class BufferedImageScreenRenderGraphics extends AbstractScreenRenderGraphics {

    private Graphics2D graphics;

    /**
     * Default constructor.
     * 
     * <p>
     * The caller has to provide a Java Graphics2D object, which can be used by
     * this renderer.
     * 
     * @param g
     *            Java Graphics2D object, that should be used for drawing.
     */
    public BufferedImageScreenRenderGraphics(Graphics2D g) {
        graphics = g;
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * {@inheritDoc}
     */
    public void setColor(Color color) {
        graphics.setColor(color);
    }

    /**
     * {@inheritDoc}
     */
    public void setFont(Font font) {
        graphics.setFont(font);
    }

    /**
     * {@inheritDoc}
     */
    public void setLineWidth(float lineWidth) {
        graphics.setStroke(new BasicStroke(lineWidth));
    }

    /**
     * {@inheritDoc}
     */
    public void drawLine(Integer x0, Integer y0, Integer x1, Integer y1) {
        graphics.drawLine(x0, y0, x1, y1);
    }

    /**
     * {@inheritDoc}
     */
    public void drawRectangle(Integer x, Integer y, Integer width, Integer height) {
        graphics.drawRect(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     */
    public void fillRectangle(Integer x, Integer y, Integer width, Integer height) {
        graphics.fillRect(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     */
    public void drawOval(Integer x, Integer y, Integer width, Integer height) {
        graphics.drawOval(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     */
    public void fillOval(Integer x, Integer y, Integer width, Integer height) {
        graphics.fillOval(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Integer[] xCoords, Integer[] yCoords) {
        int[] copyedXCoords = new int[xCoords.length];
        int[] copyedYCoords = new int[yCoords.length];
        for (int i = 0; i < xCoords.length; i++) {
            copyedXCoords[i] = xCoords[i];
            copyedYCoords[i] = yCoords[i];
        }
        graphics.drawPolygon(copyedXCoords, copyedYCoords, xCoords.length);
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Vector2dInt[] points) {
        int[] copyedXCoords = new int[points.length];
        int[] copyedYCoords = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            copyedXCoords[i] = points[i].getX();
            copyedYCoords[i] = points[i].getY();
        }
        graphics.drawPolygon(copyedXCoords, copyedYCoords, points.length);
    }

    /**
     * {@inheritDoc}
     */
    public void fillPolygon(Integer[] xCoords, Integer[] yCoords) {
        int[] copyedXCoords = new int[xCoords.length];
        int[] copyedYCoords = new int[yCoords.length];
        for (int i = 0; i < xCoords.length; i++) {
            copyedXCoords[i] = xCoords[i];
            copyedYCoords[i] = yCoords[i];
        }
        graphics.fillPolygon(copyedXCoords, copyedYCoords, xCoords.length);
    }

    /**
     * {@inheritDoc}
     */
    public void fillPolygon(Vector2dInt[] points) {
        int[] copyedXCoords = new int[points.length];
        int[] copyedYCoords = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            copyedXCoords[i] = points[i].getX();
            copyedYCoords[i] = points[i].getY();
        }
        graphics.fillPolygon(copyedXCoords, copyedYCoords, points.length);
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Integer x, Integer y, Integer width, Integer height) {
        graphics.drawImage(image, x, y, width, height, null);
    }

    /**
     * {@inheritDoc}
     */
    public void drawText(String text, Integer x, Integer y) {
        graphics.drawString(text, x, y + graphics.getFontMetrics().getAscent());
    }
}
