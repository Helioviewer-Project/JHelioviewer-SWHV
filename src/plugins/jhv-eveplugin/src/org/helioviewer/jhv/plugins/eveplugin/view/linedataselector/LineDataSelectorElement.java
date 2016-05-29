package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;

public interface LineDataSelectorElement {

    void removeLineData();

    void setVisibility(boolean visible);

    boolean isVisible();

    String getName();

    Color getDataColor();

    boolean isDownloading();

    boolean hasData();

    Component getOptionsPanel();

    boolean isDeletable();

    boolean showYAxis();

    void draw(Graphics2D graphG, Graphics2D fullG, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    YAxis getYAxis();

    void fetchData(TimeAxis selectedAxis);

    void yaxisChanged();

    void zoomToFitAxis();

    void resetAxis();

    boolean highLightChanged(Point p);

}
