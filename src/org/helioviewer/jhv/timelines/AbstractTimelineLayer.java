package org.helioviewer.jhv.timelines;

import java.awt.Point;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;

public abstract class AbstractTimelineLayer implements TimelineLayer {

    protected boolean enabled = true;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean _enabled) {
        enabled = _enabled;
    }

    @Override
    public void yaxisChanged() {
    }

    @Override
    public boolean highLightChanged(Point p) {
        return false;
    }

    @Override
    public boolean hasDataColor() {
        return false;
    }

    @Override
    public String getStringValue(long ts) {
        return null;
    }

    @Override
    public ClickableDrawable getDrawableUnderMouse() {
        return null;
    }

}
