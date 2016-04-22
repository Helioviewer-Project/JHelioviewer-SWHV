package org.helioviewer.jhv.plugins.eveplugin.lines.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;

public class EVEDrawableElement implements DrawableElement {

    private final List<GraphPolyline> graphPolylines = new ArrayList<GraphPolyline>();
    private Band[] bands = new Band[0];
    private YAxis yAxis;
    private long lastMilliWithData;

    public EVEDrawableElement() {
        bands = new Band[0];
        yAxis = new YAxis();
        lastMilliWithData = -1;
    }

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.LINE;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxisG, Rectangle graphArea, Rectangle leftAxisArea, TimeAxis timeAxis, Point mousePosition) {
        updateGraphsData(graphArea, timeAxis);
        drawGraphs(g, graphArea);
    }

    private void updateGraphsData(Rectangle graphArea, TimeAxis timeAxis) {
        graphPolylines.clear();

        for (int i = 0; i < bands.length; ++i) {
            if (bands[i].isVisible()) {
                EVEValues values = EVEDrawController.getSingletonInstance().getValues(bands[i], graphArea);
                int num = values.getNumberOfValues();
                final ArrayList<Point> pointList = new ArrayList<Point>();
                final LinkedList<Integer> warnLevels = new LinkedList<Integer>();
                final LinkedList<String> warnLabels = new LinkedList<String>();
                HashMap<String, Double> unconvertedWarnLevels = bands[i].getBandType().getWarnLevels();

                Iterator<Map.Entry<String, Double>> it = unconvertedWarnLevels.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Double> pairs = it.next();
                    warnLevels.add(yAxis.value2pixel(graphArea.y, graphArea.height, pairs.getValue()));
                    warnLabels.add(pairs.getKey());
                }

                int counter = 0;
                for (int j = 0; j < num; j++) {
                    float value = values.maxValues[j];

                    if (value < 10e-32) {
                        if (counter > 1) {
                            graphPolylines.add(new GraphPolyline(pointList, bands[i].getGraphColor(), warnLevels, warnLabels, graphArea, timeAxis));
                        }
                        pointList.clear();
                        counter = 0;
                        continue;
                    }

                    long date = values.dates[j];
                    int x = timeAxis.value2pixel(graphArea.x, graphArea.width, date);
                    int y = yAxis.value2pixel(graphArea.y, graphArea.height, value);
                    if (date > lastMilliWithData) {
                        lastMilliWithData = date;
                    }

                    final Point point = new Point(x, y);
                    pointList.add(point);
                    counter++;
                }
                if (counter > 0) {
                    graphPolylines.add(new GraphPolyline(pointList, bands[i].getGraphColor(), warnLevels, warnLabels, graphArea, timeAxis));
                }
            }

        }
    }

    private void drawGraphs(final Graphics2D g, Rectangle graphArea) {
        for (GraphPolyline line : graphPolylines) {
            g.setColor(line.color);
            for (int k = 0; k < line.xPoints.size(); k++) {
                g.drawPolyline(line.xPointsArray.get(k), line.yPointsArray.get(k), line.yPoints.get(k).size());
            }

            for (int j = 0; j < line.warnLevels.length; j++) {
                g.drawLine(graphArea.x, line.warnLevels[j], graphArea.x + graphArea.width, line.warnLevels[j]);
                g.drawString(line.warnLabels[j], graphArea.x, line.warnLevels[j] - 2);
            }
        }
    }

    private class GraphPolyline {

        public final ArrayList<ArrayList<Integer>> xPoints;
        public final ArrayList<ArrayList<Integer>> yPoints;
        public final ArrayList<int[]> xPointsArray;
        public final ArrayList<int[]> yPointsArray;
        public final int[] warnLevels;
        public final String[] warnLabels;
        public final Color color;

        public GraphPolyline(final List<Point> points, final Color color, final List<Integer> warnLevels, final List<String> warnLabels, Rectangle graphArea, TimeAxis timeAxis) {
            xPoints = new ArrayList<ArrayList<Integer>>();
            yPoints = new ArrayList<ArrayList<Integer>>();
            xPointsArray = new ArrayList<int[]>();
            yPointsArray = new ArrayList<int[]>();
            this.color = color;

            int numberOfWarnLevels = warnLevels.size();
            this.warnLevels = new int[numberOfWarnLevels];
            this.warnLabels = new String[numberOfWarnLevels];
            int counter = -1;
            double localGraphWidth = graphArea.width > 0 ? graphArea.width : 10000;
            Integer previousX = null;
            int len = points.size();
            int jump = (int) (len / localGraphWidth);
            if (jump == 0) {
                jump = 1;
            }
            int index = 0;
            while (index < len) {
                Point point = points.get(index);
                double timediff = 0;
                if (previousX != null) {
                    timediff = timeAxis.value2pixel(graphArea.x, graphArea.width, point.x) - timeAxis.value2pixel(graphArea.x, graphArea.width, previousX);
                }
                if (previousX == null || timediff > 2 * TimeUtils.MINUTE_IN_MILLIS) {
                    xPoints.add(new ArrayList<Integer>());
                    yPoints.add(new ArrayList<Integer>());
                    counter++;
                }
                xPoints.get(counter).add(point.x);
                yPoints.get(counter).add(point.y);
                previousX = point.x;
                index += jump;
            }

            for (int i = 0; i < xPoints.size(); i++) {
                int[] xPointsArr = new int[xPoints.get(i).size()];
                int[] yPointsArr = new int[yPoints.get(i).size()];
                for (int j = 0; j < xPoints.get(i).size(); j++) {
                    xPointsArr[j] = xPoints.get(i).get(j);
                    yPointsArr[j] = yPoints.get(i).get(j);
                }
                xPointsArray.add(xPointsArr);
                yPointsArray.add(yPointsArr);
            }

            counter = 0;
            for (final Integer warnLevel : warnLevels) {
                this.warnLevels[counter] = warnLevel;
                counter++;
            }
            counter = 0;
            for (final String warnLabel : warnLabels) {
                this.warnLabels[counter] = warnLabel;
                counter++;
            }
        }
    }

    public void set(Band[] bands, YAxis _yAxis) {
        this.bands = bands;
        yAxis = _yAxis;
    }

    @Override
    public void setYAxis(YAxis _yAxis) {
        yAxis = _yAxis;
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public boolean hasElementsToDraw() {
        return bands.length > 0;
    }

    @Override
    public long getLastDateWithData() {
        return lastMilliWithData;
    }

}
