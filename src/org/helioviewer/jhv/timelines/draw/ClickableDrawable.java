package org.helioviewer.jhv.timelines.draw;

import java.awt.Point;

public interface ClickableDrawable {

    void clicked(Point locationOnScreen, long timestamp);

}
