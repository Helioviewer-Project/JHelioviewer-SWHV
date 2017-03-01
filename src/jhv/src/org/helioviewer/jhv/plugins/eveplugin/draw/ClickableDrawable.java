package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Point;

public interface ClickableDrawable {
    public abstract void clicked(Point locationOnScreen, long timestamp);
}
