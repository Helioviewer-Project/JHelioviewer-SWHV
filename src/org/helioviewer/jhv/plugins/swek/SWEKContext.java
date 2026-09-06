package org.helioviewer.jhv.plugins.swek;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.ObservationGroup;

final class SWEKContext {

    private ObservationGroup mouseOverGroup;
    private int mouseOverX;
    private int mouseOverY;
    private long mouseOverTime;

    @Nullable
    ObservationGroup mouseOverGroup() {
        return mouseOverGroup;
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
        mouseOverGroup = null;
    }

    void setMouseOver(int x, int y, long time, @Nullable ObservationGroup event) {
        mouseOverX = x;
        mouseOverY = y;
        mouseOverTime = time;
        mouseOverGroup = event;
    }

}
