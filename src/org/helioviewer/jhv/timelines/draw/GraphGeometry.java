package org.helioviewer.jhv.timelines.draw;

import java.awt.Point;
import java.awt.Rectangle;

public final class GraphGeometry {

    private Rectangle size = new Rectangle();
    private Rectangle area = new Rectangle();

    public void setSize(Rectangle graphSize) {
        size = new Rectangle(graphSize.x, graphSize.y, Math.max(1, graphSize.width), Math.max(1, graphSize.height));
    }

    public void layout(int propagatedAxisCount, int yAxisCount) {
        int height = size.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE + DrawConstants.GRAPH_BOTTOM_AXIS_SPACE * (propagatedAxisCount + 1));
        int rightAxisCount = Math.max(0, yAxisCount - 1);
        int width = size.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + rightAxisCount * DrawConstants.RIGHT_AXIS_WIDTH);
        area = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE, Math.max(1, width), Math.max(1, height));
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

    public int axisZoomY(Point p) {
        return size.height - p.y - area.y;
    }

    public YAxisHit yAxisHit(Point p) {
        boolean inYRange = p.y > area.y && p.y <= graphBottom();
        boolean rightAxes = inYRange && p.x > graphRight();
        boolean leftAxis = inYRange && p.x < area.x;
        int rightAxisNumber = (p.x - graphRight()) / DrawConstants.RIGHT_AXIS_WIDTH;
        return new YAxisHit(rightAxes, leftAxis, rightAxisNumber);
    }

    public record YAxisHit(boolean rightAxes, boolean leftAxis, int rightAxisNumber) {
        public boolean outsideAxes() {
            return !rightAxes && !leftAxis;
        }

        public boolean targets(int axisIndex) {
            return (rightAxes && rightAxisNumber == axisIndex) || (leftAxis && axisIndex == -1);
        }
    }

}
