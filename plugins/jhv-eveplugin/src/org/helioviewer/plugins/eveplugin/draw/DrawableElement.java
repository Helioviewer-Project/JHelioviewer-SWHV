package org.helioviewer.plugins.eveplugin.draw;

import java.awt.Graphics;
import java.awt.Rectangle;

public interface DrawableElement {
    public abstract DrawableElementType getDrawableElementType();

    public abstract void draw(Graphics g, Rectangle graphArea);

    public abstract void setYAxisElement(YAxisElement yAxisElement);

    public abstract YAxisElement getYAxisElement();

    public abstract boolean hasElementsToDraw();
}
