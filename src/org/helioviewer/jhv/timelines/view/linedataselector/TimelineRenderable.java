package org.helioviewer.jhv.timelines.view.linedataselector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONObject;

public interface TimelineRenderable {

    void remove();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    String getName();

    Color getDataColor();

    boolean hasDataColor();

    boolean isDownloading();

    boolean hasData();

    Component getOptionsPanel();

    boolean isDeletable();

    boolean showYAxis();

    void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    void drawHighlighted(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    YAxis getYAxis();

    void fetchData(TimeAxis selectedAxis);

    void yaxisChanged();

    void zoomToFitAxis();

    void resetAxis();

    boolean highLightChanged(Point p);

    boolean hasValueAsString();

    String getStringValue(long ts);

    ClickableDrawable getDrawableUnderMouse();

    void serialize(JSONObject jo);

}
