package org.helioviewer.viewmodel.renderer.screen;

import java.awt.image.BufferedImage;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Abstract base class for ScreenRenderGraphics implementations.
 * 
 * <p>
 * This class implements some of the methods provided by ScreenRenderGraphics by
 * simply mapping them to other methods. That way, every specific implementation
 * only has to implement the essential methods.
 * 
 * @author Markus Langenberg
 * 
 */
public abstract class AbstractScreenRenderGraphics implements ScreenRenderGraphics {

    /**
     * {@inheritDoc}
     */
    public void drawLine(Vector2dInt p0, Vector2dInt p1) {
        drawLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawRectangle(Vector2dInt position, Vector2dInt size) {
        drawRectangle(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void fillRectangle(Vector2dInt position, Vector2dInt size) {
        fillRectangle(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawOval(Vector2dInt position, Vector2dInt size) {
        drawOval(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void fillOval(Vector2dInt position, Vector2dInt size) {
        fillOval(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Integer x, Integer y) {
        drawImage(image, x, y, image.getWidth(), image.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Vector2dInt position) {
        drawImage(image, position.getX(), position.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Integer x, Integer y, float scale) {
        drawImage(image, x, y, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale));
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Vector2dInt position, float scale) {
        drawImage(image, position.getX(), position.getY(), scale);
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Vector2dInt position, Vector2dInt size) {
        drawImage(image, position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawText(String text, Vector2dInt position) {
        drawText(text, position.getX(), position.getY());
    }
}
