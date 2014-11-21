package org.helioviewer.plugins.eveplugin.draw;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Date;

public interface DrawableElement {
    public abstract DrawableElementType getDrawableElementType();

    public abstract void draw(Graphics2D graphG, Graphics2D leftAxisG, Rectangle graphArea, Rectangle leftAxisArea);

    public abstract void setYAxisElement(YAxisElement yAxisElement);

    public abstract YAxisElement getYAxisElement();

    public abstract boolean hasElementsToDraw();

    public abstract Date getLastDateWithData();
}
