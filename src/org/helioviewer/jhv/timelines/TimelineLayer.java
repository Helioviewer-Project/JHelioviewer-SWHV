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

public interface TimelineLayer {

    void remove();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    String getName();

    @Nullable
    Color getDataColor();

    boolean isDownloading();

    boolean hasData();

    @Nullable
    JPanel getOptionsPanel();

    boolean isDeletable();

    boolean showYAxis();

    void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition);

    YAxis getYAxis();

    void fetchData(TimeAxis selectedAxis);

    default void yaxisChanged() {
    }

    default void zoomToFitAxis() {
    }

    default void resetAxis() {
    }

    default boolean highLightChanged(Point p) {
        return false;
    }

    @Nullable
    default String getStringValue(long ts) {
        return null;
    }

    @Nullable
    default ClickableDrawable getDrawableUnderMouse() {
        return null;
    }

    void serialize(JSONObject jo);

    default boolean isPropagated() {
        return false;
    }

    default long getObservationTime(long ts) {
        return ts;
    }

}
