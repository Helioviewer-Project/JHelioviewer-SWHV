package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Rectangle;
import java.util.Date;

public interface ZoomManagerListener {
    public abstract void displaySizeChanged(Rectangle area);

    public abstract void XValuesChanged(Date minX, Date maxX);
}
