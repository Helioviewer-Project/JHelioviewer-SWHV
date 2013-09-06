package org.helioviewer.viewmodel.renderer.physical;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.view.View;

/**
 * Implementation of ScreenRenderGraphics operating on Java BufferedImages.
 * 
 * <p>
 * Maps all methods to corresponding methods of Graphics2D.
 * 
 * @author Stephan Pagel
 * @author Markus Langenberg
 * 
 */
public class BufferedImagePhysicalRenderGraphics extends AbstractPhysicalRenderGraphics {

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
     * @param view
     *            View to access information about the physical coordinate
     *            system.
     */
    public BufferedImagePhysicalRenderGraphics(Graphics g, View view) {
        super(view);
        graphics = (Graphics2D) g;
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
    public void drawLine(Double x0, Double y0, Double x1, Double y1) {
        Vector2dInt p0 = convertPhysicalToScreen(x0, y0);
        Vector2dInt p1 = convertPhysicalToScreen(x1, y1);
        graphics.drawLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawRectangle(Double x, Double y, Double width, Double height) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        Vector2dInt size = convertPhysicalToScreen(width, height);
        graphics.drawRect(pos.getX() - (size.getX() >> 1), pos.getY() - (size.getY() >> 1), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void fillRectangle(Double x, Double y, Double width, Double height) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        Vector2dInt size = convertPhysicalToScreen(width, height);
        graphics.fillRect(pos.getX() - (size.getX() >> 1), pos.getY() - (size.getY() >> 1), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawOval(Double x, Double y, Double width, Double height) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        Vector2dInt size = convertPhysicalToScreen(width, height);
        graphics.drawOval(pos.getX() - (size.getX() >> 1), pos.getY() - (size.getY() >> 1), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void fillOval(Double x, Double y, Double width, Double height) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        Vector2dInt size = convertPhysicalToScreen(width, height);
        graphics.fillOval(pos.getX() - (size.getX() >> 1), pos.getY() - (size.getY() >> 1), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Double[] xCoords, Double[] yCoords) {
        Vector2dInt[] convertedPoints = new Vector2dInt[xCoords.length];
        for (int i = 0; i < xCoords.length; i++) {
            convertedPoints[i] = convertPhysicalToScreen(xCoords[i], yCoords[i]);
        }
        drawPolygon(convertedPoints);
    }

    /**
     * {@inheritDoc}
     */
    public void drawPolygon(Vector2dDouble[] points) {
        Vector2dInt[] convertedPoints = new Vector2dInt[points.length];
        for (int i = 0; i < points.length; i++) {
            convertedPoints[i] = convertPhysicalToScreen(points[i].getX(), points[i].getY());
        }
        drawPolygon(convertedPoints);
    }

    /**
     * Internal function to draw a polygon.
     * 
     * The coordinates already have to be converted to screen coordinates.
     * 
     * @param points
     *            Array of the coordinates
     */
    private void drawPolygon(Vector2dInt[] points) {
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
    public void fillPolygon(Double[] xCoords, Double[] yCoords) {
        Vector2dInt[] convertedPoints = new Vector2dInt[xCoords.length];
        for (int i = 0; i < xCoords.length; i++) {
            convertedPoints[i] = convertPhysicalToScreen(xCoords[i], yCoords[i]);
        }
        fillPolygon(convertedPoints);
    }

    /**
     * {@inheritDoc}
     */
    public void fillPolygon(Vector2dDouble[] points) {
        Vector2dInt[] convertedPoints = new Vector2dInt[points.length];
        for (int i = 0; i < points.length; i++) {
            convertedPoints[i] = convertPhysicalToScreen(points[i].getX(), points[i].getY());
        }
        fillPolygon(convertedPoints);
    }

    /**
     * Internal function to draw a polygon.
     * 
     * The coordinates already have to be converted to screen coordinates.
     * 
     * @param points
     *            Array of the coordinates
     */
    private void fillPolygon(Vector2dInt[] points) {
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
    public void drawImage(BufferedImage image, Double x, Double y) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        graphics.drawImage(image, pos.getX() - image.getWidth() / 2, pos.getY() - image.getHeight() / 2, null);
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Double x, Double y, float scale) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        int width = (int) (image.getWidth() * scale);
        int height = (int) (image.getHeight() * scale);
        graphics.drawImage(image, pos.getX() - width / 2, pos.getY() - height / 2, width, height, null);
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Double x, Double y, Double width, Double height) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        Vector2dInt size = convertPhysicalToScreen(width, height);
        graphics.drawImage(image, pos.getX() - size.getX() / 2, pos.getY() - size.getY() / 2, size.getX(), size.getY(), null);
    }

    /**
     * {@inheritDoc}
     */
    public void drawText(String text, Double x, Double y) {
        Vector2dInt pos = convertPhysicalToScreen(x, y);
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, pos.getX() - metrics.getAscent() + metrics.getHeight() / 2, pos.getY() + metrics.stringWidth(text) / 2);
    }
}
