package org.helioviewer.jhv.timelines;

import javax.annotation.Nullable;

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

    @Nullable
    @Override
    public String getStringValue(long ts) {
        return null;
    }

    @Nullable
    @Override
    public ClickableDrawable getDrawableUnderMouse() {
        return null;
    }

}
