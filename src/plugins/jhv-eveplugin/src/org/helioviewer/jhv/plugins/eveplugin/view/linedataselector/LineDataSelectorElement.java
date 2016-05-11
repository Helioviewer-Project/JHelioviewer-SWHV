package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;

public interface LineDataSelectorElement {

    public void removeLineData();

    public void setVisibility(boolean visible);

    public boolean isVisible();

    public String getName();

    public Color getDataColor();

    public boolean isDownloading();

    public boolean hasData();

    public Component getOptionsPanel();

    public boolean isDeletable();

    public boolean showYAxis();

    public void draw(Graphics2D graphG, Graphics2D fullG, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    public YAxis getYAxis();

    public void fetchData(TimeAxis selectedAxis);

    public void yaxisChanged();

    public void zoomToFitAxis();

    public void resetAxis();

    public boolean highLightChanged(Point p);

}
