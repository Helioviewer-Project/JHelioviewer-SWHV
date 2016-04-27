package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;

public interface LineDataSelectorElement {

    public abstract void removeLineData();

    public abstract void setVisibility(boolean visible);

    public abstract boolean isVisible();

    public abstract String getName();

    public abstract Color getDataColor();

    public abstract boolean isDownloading();

    public abstract boolean hasData();

    public abstract Component getOptionsPanel();

    public abstract boolean isDeletable();

    boolean showYAxis();

    public abstract void draw(Graphics2D graphG, Rectangle graphArea, Rectangle leftAxisArea, TimeAxis timeAxis, Point mousePosition);

    public abstract void setYAxis(YAxis yAxis);

    public abstract YAxis getYAxis();

    public abstract boolean hasElementsToDraw();

    public void fetchData(TimeAxis selectedAxis, TimeAxis availableAxis);

    public abstract void yaxisChanged();

}
