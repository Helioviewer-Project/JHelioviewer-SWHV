package org.helioviewer.jhv.plugins.swek;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.JHVObservationGroup;

final class SWEKContext {

    private JHVObservationGroup mouseOverJHVEvent;
    private int mouseOverX;
    private int mouseOverY;
    private long mouseOverTime;

    @Nullable
    JHVObservationGroup mouseOverJHVEvent() {
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

    void setMouseOver(int x, int y, long time, @Nullable JHVObservationGroup event) {
        mouseOverX = x;
        mouseOverY = y;
        mouseOverTime = time;
        mouseOverJHVEvent = event;
    }

}
