package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LineDataSelectorElement {

    void removeLineData();

    void setVisibility(boolean visible);

    boolean isVisible();

    String getName();

    @Nullable Color getDataColor();

    boolean isDownloading();

    boolean hasData();

    @Nullable Component getOptionsPanel();

    boolean isDeletable();

    boolean showYAxis();

    void draw(@NotNull Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    void drawHighlighted(@NotNull Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    @Nullable YAxis getYAxis();

    void fetchData(TimeAxis selectedAxis);

    void yaxisChanged();

    void zoomToFitAxis();

    void resetAxis();

    boolean highLightChanged(Point p);

    boolean isEmpty();

}
