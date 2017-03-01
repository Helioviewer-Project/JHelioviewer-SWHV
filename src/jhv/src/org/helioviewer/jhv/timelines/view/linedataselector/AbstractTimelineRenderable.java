package org.helioviewer.jhv.timelines.view.linedataselector;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

public abstract class AbstractTimelineRenderable implements TimelineRenderable {

    protected boolean isVisible = true;

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
    }

    @Override
    public void yaxisChanged() {
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean highLightChanged(Point p) {
        return false;
    }

    @Override
    public void drawHighlighted(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
    }

    @Override
    public boolean hasDataColor() {
        return false;
    }

    @Override
    public boolean hasValueAsString() {
        return false;
    }

    @Override
    public String getStringValue(long ts) {
        return null;
    }

    @Override
    public ClickableDrawable getElementUnderMouse() {
        return null;
    }
}
