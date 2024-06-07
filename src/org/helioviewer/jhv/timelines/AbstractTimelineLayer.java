package org.helioviewer.jhv.timelines;

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

}
