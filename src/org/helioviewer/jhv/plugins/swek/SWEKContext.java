package org.helioviewer.jhv.plugins.swek;

import javax.annotation.Nullable;

import org.helioviewer.jhv.events.JHVRelatedEvents;

final class SWEKContext {

    private boolean enabled;
    private JHVRelatedEvents mouseOverJHVEvent;
    private int mouseOverX;
    private int mouseOverY;
    private long mouseOverTime;

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

    long mouseOverTime() {
        return mouseOverTime;
    }

    void clearHover() {
        mouseOverJHVEvent = null;
    }

    void setMouseOver(int x, int y, long time, @Nullable JHVRelatedEvents event) {
        mouseOverX = x;
        mouseOverY = y;
        mouseOverTime = time;
        mouseOverJHVEvent = event;
    }

}
