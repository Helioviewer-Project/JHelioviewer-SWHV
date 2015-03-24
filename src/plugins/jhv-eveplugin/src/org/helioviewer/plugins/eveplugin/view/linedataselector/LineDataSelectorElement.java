package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.Color;

public interface LineDataSelectorElement {
    public abstract void removeLineData();

    public abstract void setVisibility(boolean visible);

    public abstract boolean isVisible();

    public abstract String getName();

    public abstract Color getDataColor();

    public abstract void setDataColor(Color c);

    public abstract boolean isDownloading();

    public abstract boolean isAvailable();

    public abstract String getUnitLabel();
}
