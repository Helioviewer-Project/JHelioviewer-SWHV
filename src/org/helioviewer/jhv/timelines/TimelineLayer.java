package org.helioviewer.jhv.timelines;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import javax.annotation.Nullable;
import javax.swing.JPanel;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONObject;

public abstract class TimelineLayer {

    protected boolean enabled = true;

    public abstract void remove();

    public void setEnabled(boolean _enabled) {
        enabled = _enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public abstract String getName();

    @Nullable
    public abstract Color getDataColor();

    public abstract boolean isDownloading();

    public abstract boolean hasData();

    @Nullable
    public abstract JPanel getOptionsPanel();

    public abstract boolean isDeletable();

    public abstract boolean hasYAxis();

    public abstract void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    public abstract YAxis getYAxis();

    public abstract void fetchData(TimeAxis selectedAxis);

    public void graphGeometryChanged() {}

    public void yaxisChanged() {}

    public void zoomToFitAxis() {}

    public void resetAxis() {}

    public boolean highlightChanged(Point p) {
        return false;
    }

    @Nullable
    public String getStringValue(long ts) {
        return null;
    }

    @Nullable
    public ClickableDrawable getDrawableUnderMouse() {
        return null;
    }

    public abstract void serialize(JSONObject jo);

    public boolean isPropagated() {
        return false;
    }

    public long getObservationTime(long ts) {
        return ts;
    }

}
