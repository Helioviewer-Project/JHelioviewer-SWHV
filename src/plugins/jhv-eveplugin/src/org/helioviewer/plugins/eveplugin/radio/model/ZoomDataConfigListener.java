package org.helioviewer.plugins.eveplugin.radio.model;

import java.util.Date;

public interface ZoomDataConfigListener {
    public abstract void requestData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio, long ID);
}
