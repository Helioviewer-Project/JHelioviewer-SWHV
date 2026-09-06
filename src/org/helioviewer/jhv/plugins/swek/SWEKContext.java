package org.helioviewer.jhv.plugins.swek;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.RelatedEvents;

final class SWEKContext {

    private RelatedEvents mouseOverEvents;
    private int mouseOverX;
    private int mouseOverY;
    private long mouseOverTime;

    @Nullable
    RelatedEvents mouseOverEvents() {
        return mouseOverEvents;
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
        mouseOverEvents = null;
    }

    void setMouseOver(int x, int y, long time, @Nullable RelatedEvents event) {
        mouseOverX = x;
        mouseOverY = y;
        mouseOverTime = time;
        mouseOverEvents = event;
    }

}
