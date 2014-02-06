package org.helioviewer.viewmodel.renderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import org.helioviewer.base.math.Vector3dDouble;

/**
 * Interface for a renderer with basic graphical drawing functions.
 * 
 * The used types depend on the use of the RenderGraphics-Objekt
 * 
 * @author Markus Langenberg
 */
public interface RenderGraphics<BaseType extends Number, VectorType> {

    /**
     * Sets the color used for drawing lines and rectangles.
     * 
     * @param color
     *            new color
     */
    public void setColor(Color color);

    /**
     * Sets the font used to draw text.
     * 
     * @param font
     *            new Font
     */
    public void setFont(Font font);

    /**
     * Sets the line width used for drawing lines and rectangles.
     * 
     * @param lineWidth
     *            new line width
     */
    public void setLineWidth(float lineWidth);

    /**
     * Draws a line from one point to another.
     * 
     * Color and width of the line can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param x0
     *            x-coordinate of the starting point
     * @param y0
     *            y-coordinate of the starting point
     * @param x1
     *            x-coordinate of the ending point
     * @param y1
     *            y-coordinate of the ending point
     */
    public void drawLine(BaseType x0, BaseType y0, BaseType x1, BaseType y1);

    /**
     * Draws a line from one point to another.
     * 
     * Color and width of the line can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param p0
     *            starting point
     * @param p1
     *            ending point
     */
    public void drawLine(VectorType p0, VectorType p1);

    /**
     * Draws a rectangle.
     * 
     * Color and line width of the rectangle can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param width
     *            width of the rectangle
     * @param height
     *            height of the rectangle
     */
    public void drawRectangle(BaseType x, BaseType y, BaseType width, BaseType height);

    /**
     * Draws a rectangle.
     * 
     * Color and line width of the rectangle can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param position
     *            coordinate of reference point
     * @param size
     *            size of the rectangle
     */
    public void drawRectangle(VectorType position, VectorType size);

    /**
     * Fills a rectangle.
     * 
     * The color of the rectangle can be changed by calling
     * {@link #setColor(Color)} in advance.
     * 
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param width
     *            width of the rectangle
     * @param height
     *            height of the rectangle
     */
    public void fillRectangle(BaseType x, BaseType y, BaseType width, BaseType height);

    /**
     * Fills a rectangle.
     * 
     * The color of the rectangle can be changed by calling
     * {@link #setColor(Color)} in advance.
     * 
     * @param position
     *            coordinate of the reference point
     * @param size
     *            size of the rectangle
     */
    public void fillRectangle(VectorType position, VectorType size);

    /**
     * Draws an oval.
     * 
     * Color and line width of the oval can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param width
     *            width of the oval
     * @param height
     *            height of the oval
     */
    public void drawOval(BaseType x, BaseType y, BaseType width, BaseType height);

    /**
     * Draws an oval.
     * 
     * Color and line width of the oval can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param position
     *            coordinate of the reference point
     * @param size
     *            size of the oval
     */
    public void drawOval(VectorType position, VectorType size);

    /**
     * Fills an oval.
     * 
     * The color of the oval can be changed by calling {@link #setColor(Color)}
     * in advance.
     * 
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param width
     *            width of the oval
     * @param height
     *            height of the oval
     */
    public void fillOval(BaseType x, BaseType y, BaseType width, BaseType height);

    /**
     * Fills an oval.
     * 
     * The color of the oval can be changed by calling {@link #setColor(Color)}
     * in advance.
     * 
     * @param position
     *            coordinate of the reference point
     * @param size
     *            size of the oval
     */
    public void fillOval(VectorType position, VectorType size);

    /**
     * Draws a polygon.
     * 
     * Color and line width of the polygon can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param xCoords
     *            Array of the x-coordinates.
     * @param yCoords
     *            Array of the y-coordinates.
     */
    public void drawPolygon(BaseType[] xCoords, BaseType[] yCoords);

    /**
     * Draws a polygon.
     * 
     * Color and line width of the polygon can be changed by calling
     * {@link #setColor(Color)} and {@link #setLineWidth(float)} in advance.
     * 
     * @param points
     *            Array of the coordinates.
     */
    public void drawPolygon(VectorType[] points);

    /**
     * Fills an polygon.
     * 
     * The polygon has to be convex.
     * 
     * The color of the polygon can be changed by calling
     * {@link #setColor(Color)} in advance.
     * 
     * @param xCoords
     *            Array of the x-coordinates.
     * @param yCoords
     *            Array of the y-coordinates.
     */
    public void fillPolygon(BaseType[] xCoords, BaseType[] yCoords);

    /**
     * Fills an polygon.
     * 
     * The polygon has to be convex.
     * 
     * The color of the polygon can be changed by calling
     * {@link #setColor(Color)} in advance.
     * 
     * @param points
     *            Array of coordinates.
     */
    public void fillPolygon(VectorType[] points);

    /**
     * Draws an unscaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     */
    public void drawImage(BufferedImage image, BaseType x, BaseType y);

    /**
     * Draws an unscaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param position
     *            coordinate of the reference point
     */
    public void drawImage(BufferedImage image, VectorType position);

    /**
     * Draws an scaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param scale
     *            scale factor used for the image size
     */
    public void drawImage(BufferedImage image, BaseType x, BaseType y, float scale);

    /**
     * Draws a scaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param position
     *            coordinate of the reference point
     * @param scale
     *            scale factor used for the image size
     */
    public void drawImage(BufferedImage image, VectorType position, float scale);

    /**
     * Draws an scaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param width
     *            width of the rectangle the image is drawn to
     * @param height
     *            height of the rectangle the image is drawn to
     */
    public void drawImage(BufferedImage image, BaseType x, BaseType y, BaseType width, BaseType height);

    /**
     * Draws an scaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param position
     *            coordinate of the reference point
     * @param size
     *            size of the rectangle the image is drawn to
     */
    public void drawImage(BufferedImage image, VectorType position, VectorType size);

    /**
     * Draws an unscaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param z
     *            z-coordinate of the reference point
     */
    public void drawImage3d(BufferedImage image, BaseType x, BaseType y, BaseType z);

    /**
     * Draws an scaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param z
     *            z-coordinate of the reference point
     * @param scale
     *            scale factor used for the image size
     */
    public void drawImage3d(BufferedImage image, BaseType x, BaseType y, BaseType z, float scale);

    /**
     * Draws an scaled image.
     * 
     * @param image
     *            BufferedImage to draw
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     * @param z
     *            z-coordinate of the reference point
     * @param width
     *            width of the rectangle the image is drawn to
     * @param height
     *            height of the rectangle the image is drawn to
     */
    public void drawImage3d(BufferedImage image, BaseType x, BaseType y, BaseType z, BaseType width, BaseType height);
    
    /**
     * Draws a text.
     * 
     * @param text
     *            Text to draw
     * @param x
     *            x-coordinate of the reference point
     * @param y
     *            y-coordinate of the reference point
     */
    public void drawText(String text, BaseType x, BaseType y);

    /**
     * Draws a text.
     * 
     * @param text
     *            Text to draw
     * @param position
     *            coordinate of the reference point
     */
    public void drawText(String text, VectorType position);
    
    public void fillPolygon(Vector3dDouble[] points);

    public void drawLine3d(BaseType x0, BaseType y0, BaseType z0, BaseType x1, BaseType y1, BaseType z1);
    public void drawLine3d(Vector3dDouble p0, Vector3dDouble p1);
 
}
