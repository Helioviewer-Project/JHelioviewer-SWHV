package org.helioviewer.jhv.timelines.draw;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.timelines.TimelineLayer;

public final class GraphGeometry {

    private static final int MINIMUM_HEIGHT = 50;
    private static final int MINIMUM_STACKED_LAYER_HEIGHT = 64;
    private static final int STACKED_SEPARATOR = 8;

    private Rectangle size = new Rectangle();
    private Rectangle area = new Rectangle();
    private boolean stacked;
    private final ArrayList<LayerLayout> layerLayouts = new ArrayList<>();
    private final ArrayList<TimelineLayer> propagatedLayers = new ArrayList<>();
    private final List<LayerLayout> exposedLayerLayouts = Collections.unmodifiableList(layerLayouts);
    private final List<TimelineLayer> exposedPropagatedLayers = Collections.unmodifiableList(propagatedLayers);

    public record LayerLayout(TimelineLayer layer, Rectangle area, int axisIndex) {}

    public void setSize(Rectangle graphSize) {
        size = new Rectangle(graphSize.x, graphSize.y, Math.max(1, graphSize.width), Math.max(1, graphSize.height));
    }

    void setStacked(boolean _stacked) {
        stacked = _stacked;
    }

    public void layout(List<TimelineLayer> layers) {
        layerLayouts.clear();
        propagatedLayers.clear();

        int yAxisCount = 0;
        for (TimelineLayer layer : layers) {
            if (layer.isPropagated())
                propagatedLayers.add(layer);
            if (layer.isEnabled() && layer.hasYAxis())
                yAxisCount++;
        }

        int height = size.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE
                + DrawConstants.GRAPH_BOTTOM_AXIS_SPACE * (propagatedLayers.size() + 1));

        if (stacked && yAxisCount > 0) {
            int totalSeparatorHeight = STACKED_SEPARATOR * (yAxisCount - 1);
            int availableForStrips = Math.max(yAxisCount, height - totalSeparatorHeight);
            int stripHeight = Math.max(1, availableForStrips / yAxisCount);

            int totalHeight = stripHeight * yAxisCount + totalSeparatorHeight;
            int width = size.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE);
            area = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE,
                    Math.max(1, width), Math.max(1, totalHeight));

            int y = DrawConstants.GRAPH_TOP_SPACE;
            int axisIndex = -1;
            for (TimelineLayer layer : layers) {
                if (!layer.isEnabled() || !layer.hasYAxis())
                    continue;
                Rectangle layerArea = new Rectangle(area.x, y, area.width, stripHeight);
                layerLayouts.add(new LayerLayout(layer, layerArea, axisIndex));
                y += stripHeight + STACKED_SEPARATOR;
                axisIndex++;
            }
        } else {
            int rightAxisCount = Math.max(0, yAxisCount - 1);
            int width = size.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + rightAxisCount * DrawConstants.RIGHT_AXIS_WIDTH);
            area = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE, Math.max(1, width), Math.max(1, height));

            int axisIndex = -1;
            for (TimelineLayer layer : layers) {
                if (layer.isEnabled() && layer.hasYAxis()) {
                    layerLayouts.add(new LayerLayout(layer, area, axisIndex));
                    axisIndex++;
                }
            }
        }
    }

    public boolean isStacked() {
        return stacked;
    }

    public int minimumHeight() {
        if (!stacked || layerLayouts.isEmpty())
            return MINIMUM_HEIGHT;

        return DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE
                + DrawConstants.GRAPH_BOTTOM_AXIS_SPACE * (propagatedLayers.size() + 1)
                + MINIMUM_STACKED_LAYER_HEIGHT * layerLayouts.size()
                + STACKED_SEPARATOR * (layerLayouts.size() - 1);
    }

    public List<LayerLayout> getLayerLayouts() {
        return exposedLayerLayouts;
    }

    public List<TimelineLayer> getPropagatedLayers() {
        return exposedPropagatedLayers;
    }

    @Nullable
    public Rectangle getLayerArea(TimelineLayer layer) {
        for (LayerLayout layout : layerLayouts) {
            if (layout.layer == layer)
                return layout.area;
        }
        return null;
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

    @Nullable
    public LayerLayout getLayerLayout(Point p) {
        if (!stacked)
            return null;
        for (LayerLayout layout : layerLayouts) {
            Rectangle r = layout.area;
            if (p.x <= r.x + r.width && p.y >= r.y && p.y <= r.y + r.height)
                return layout;
        }
        return null;
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
        for (LayerLayout layout : layerLayouts) {
            Rectangle r = layout.area;
            if (p.y >= r.y && p.y <= r.y + r.height) {
                boolean leftAxis = p.x < r.x;
                return new YAxisHit(leftAxis, layout.axisIndex);
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
