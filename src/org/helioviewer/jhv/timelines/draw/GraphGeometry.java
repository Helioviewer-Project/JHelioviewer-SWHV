package org.helioviewer.jhv.timelines.draw;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helioviewer.jhv.timelines.TimelineLayer;

public final class GraphGeometry {

    private static final int STACKED_SEPARATOR = 2;

    private Rectangle size = new Rectangle();
    private Rectangle area = new Rectangle();
    private boolean stacked;
    private final ArrayList<Rectangle> layerAreas = new ArrayList<>();

    public void setSize(Rectangle graphSize) {
        size = new Rectangle(graphSize.x, graphSize.y, Math.max(1, graphSize.width), Math.max(1, graphSize.height));
    }

    public void layout(int propagatedAxisCount, int yAxisCount, boolean _stacked, List<TimelineLayer> visibleYAxisLayers) {
        stacked = _stacked;
        layerAreas.clear();

        int height = size.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE + DrawConstants.GRAPH_BOTTOM_AXIS_SPACE * (propagatedAxisCount + 1));

        if (stacked && visibleYAxisLayers.size() > 1) {
            int nLayers = visibleYAxisLayers.size();
            int totalSeparatorHeight = STACKED_SEPARATOR * (nLayers - 1);
            int availableForStrips = Math.max(nLayers, height - totalSeparatorHeight);
            int stripHeight = Math.max(1, availableForStrips / nLayers);

            int totalHeight = stripHeight * nLayers + totalSeparatorHeight;
            int width = size.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE);
            area = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE,
                    Math.max(1, width), Math.max(1, totalHeight));

            int y = DrawConstants.GRAPH_TOP_SPACE;
            for (int i = 0; i < nLayers; i++) {
                layerAreas.add(new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, y, Math.max(1, width), stripHeight));
                y += stripHeight + STACKED_SEPARATOR;
            }
        } else {
            int rightAxisCount = Math.max(0, yAxisCount - 1);
            int width = size.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + rightAxisCount * DrawConstants.RIGHT_AXIS_WIDTH);
            area = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE, Math.max(1, width), Math.max(1, height));
        }
    }

    // legacy overload for backward compatibility (overlaid mode)
    public void layout(int propagatedAxisCount, int yAxisCount) {
        layout(propagatedAxisCount, yAxisCount, false, Collections.emptyList());
    }

    public boolean isStacked() {
        return stacked;
    }

    public List<Rectangle> getLayerAreas() {
        return Collections.unmodifiableList(layerAreas);
    }

    public Rectangle getLayerArea(int index) {
        if (index >= 0 && index < layerAreas.size()) {
            return layerAreas.get(index);
        }
        return area;
    }

    public Rectangle size() {
        return size;
    }

    public Rectangle area() {
        return area;
    }

    public int graphWidth() {
        return area.width;
    }

    public int graphHeight() {
        return area.height;
    }

    public int rightEdge() {
        return size.width - DrawConstants.GRAPH_RIGHT_SPACE;
    }

    public int graphRight() {
        return area.x + area.width;
    }

    public int graphBottom() {
        return area.y + area.height;
    }

    public boolean inGraph(Point p) {
        return p.x >= area.x && p.x <= graphRight() && p.y > area.y && p.y <= graphBottom();
    }

    public boolean inXAxisOrAboveGraph(Point p) {
        return p.x >= area.x && p.x <= graphRight() && (p.y <= area.y || p.y >= graphBottom());
    }

    public TimeAxis.Mapper xMapper(TimeAxis axis) {
        return axis.mapper(area.x, area.width);
    }

    public YAxis.Mapper yMapper(YAxis axis) {
        return axis.mapper(area.y, area.height);
    }

    public YAxis.Mapper yMapper(YAxis axis, Rectangle subArea) {
        return axis.mapper(subArea.y, subArea.height);
    }

    public int axisZoomY(Point p) {
        return size.height - p.y - area.y;
    }

    public int layerIndexAtPoint(Point p) {
        if (!stacked) {
            return -1;
        }
        for (int i = 0; i < layerAreas.size(); i++) {
            Rectangle r = layerAreas.get(i);
            if (p.x >= r.x && p.x <= r.x + r.width && p.y >= r.y && p.y <= r.y + r.height) {
                return i;
            }
        }
        return -1;
    }

    public YAxisHit yAxisHit(Point p) {
        if (stacked) {
            return yAxisHitStacked(p);
        }
        boolean inYRange = p.y > area.y && p.y <= graphBottom();
        boolean rightAxes = inYRange && p.x > graphRight();
        boolean leftAxis = inYRange && p.x < area.x;
        int rightAxisNumber = (p.x - graphRight()) / DrawConstants.RIGHT_AXIS_WIDTH;
        if (leftAxis)
            return new YAxisHit(true, -1);
        if (rightAxes)
            return new YAxisHit(true, rightAxisNumber);
        return new YAxisHit(false, -1);
    }

    private YAxisHit yAxisHitStacked(Point p) {
        for (int i = 0; i < layerAreas.size(); i++) {
            Rectangle r = layerAreas.get(i);
            if (p.y >= r.y && p.y <= r.y + r.height) {
                boolean leftAxis = p.x < r.x;
                return new YAxisHit(leftAxis, i - 1);
            }
        }
        return new YAxisHit(false, -1);
    }

    public record YAxisHit(boolean onAxis, int axisIndex) {
        public boolean outsideAxes() {
            return !onAxis;
        }

        public boolean targets(int axisIndex) {
            return onAxis && this.axisIndex == axisIndex;
        }
    }

}
