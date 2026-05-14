package org.helioviewer.jhv.plugins.swek;

import javax.annotation.Nullable;

import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.time.TimeListener;

final class SWEKContext implements TimeListener.Change {

    private boolean enabled;
    private long currentTime;
    private JHVRelatedEvents mouseOverJHVEvent;
    private int mouseOverX;
    private int mouseOverY;

    @Override
    public void timeChanged(long milli) {
        currentTime = milli;
    }

    long currentTime() {
        return currentTime;
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean _enabled) {
        enabled = _enabled;
        if (!enabled)
            clearHover();
    }

    @Nullable
    JHVRelatedEvents mouseOverJHVEvent() {
        return mouseOverJHVEvent;
    }

    int mouseOverX() {
        return mouseOverX;
    }

    int mouseOverY() {
        return mouseOverY;
    }

    void clearHover() {
        mouseOverJHVEvent = null;
    }

    void setMouseOver(int x, int y, @Nullable JHVRelatedEvents event) {
        mouseOverX = x;
        mouseOverY = y;
        mouseOverJHVEvent = event;
    }

}
