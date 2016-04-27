package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.lines.gui.LineOptionPanel;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class Band implements LineDataSelectorElement {

    private final BandType bandType;

    private boolean isVisible = true;
    private Color graphColor = Color.BLACK;
    private YAxis yAxis;
    private final List<GraphPolyline> graphPolylines = new ArrayList<GraphPolyline>();
    private long lastMilliWithData;
    private final RequestCache requestCache = new RequestCache();
    public int[] warnLevels;
    public String[] warnLabels;

    public Band(BandType _bandType) {
        bandType = _bandType;
        EVEPlugin.dc.fireRedrawRequest();
        EVEPlugin.ldsm.addLineData(this);
        yAxis = new YAxis(Math.pow(10, -7), Math.pow(10, -3), bandType.getUnitLabel(), true);
    }

    public final BandType getBandType() {
        return bandType;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void removeLineData() {
        EVEPlugin.ldsm.removeLineData(this);
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        EVEPlugin.dc.fireRedrawRequest();
    }

    @Override
    public String getName() {
        return bandType.getLabel();
    }

    @Override
    public Color getDataColor() {
        return graphColor;
    }

    public void setDataColor(Color c) {
        graphColor = c;
        EVEPlugin.dc.fireRedrawRequest();
    }

    @Override
    public boolean isDownloading() {
        return DownloadController.getSingletonInstance().isDownloadActive(this);
    }

    public String getUnitLabel() {
        return bandType.getUnitLabel();
    }

    @Override
    public Component getOptionsPanel() {
        return new LineOptionPanel(this);
    }

    @Override
    public boolean hasData() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public boolean showYAxis() {
        return true;
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, Rectangle leftAxisArea, TimeAxis timeAxis, Point mousePosition) {
        drawGraphs(g, graphArea);
    }

    private void updateWarnLevels(Rectangle graphArea) {
        final LinkedList<Integer> warnLevels = new LinkedList<Integer>();
        final LinkedList<String> warnLabels = new LinkedList<String>();
        HashMap<String, Double> unconvertedWarnLevels = bandType.getWarnLevels();
        Iterator<Map.Entry<String, Double>> it = unconvertedWarnLevels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Double> pairs = it.next();
            warnLevels.add(yAxis.value2pixel(graphArea.y, graphArea.height, pairs.getValue()));
            warnLabels.add(pairs.getKey());
        }
        setWarn(warnLevels, warnLabels);
    }

    private void updateGraphsData() {
        Rectangle graphArea = EVEPlugin.dc.getGraphArea();
        TimeAxis timeAxis = EVEPlugin.dc.selectedAxis;
        graphPolylines.clear();
        if (isVisible) {
            updateWarnLevels(graphArea);
            EVEValues values = getValuesInInterval(timeAxis, graphArea);
            final ArrayList<Point> pointList = new ArrayList<Point>();

            int num = values.getNumberOfValues();

            for (int j = 0; j < num; j++) {
                float value = values.maxValues[j];
                long date = values.dates[j];
                int x = timeAxis.value2pixel(graphArea.x, graphArea.width, date);
                int y = yAxis.value2pixel(graphArea.y, graphArea.height, value);

                if (date > lastMilliWithData) {
                    lastMilliWithData = date;
                }

                final Point point = new Point(x, y);
                pointList.add(point);
            }
            graphPolylines.add(new GraphPolyline(pointList, graphColor, graphArea, timeAxis));
        }

    }

    private void drawGraphs(final Graphics2D g, Rectangle graphArea) {
        for (GraphPolyline line : graphPolylines) {
            g.setColor(line.color);
            for (int k = 0; k < line.xPoints.size(); k++) {
                g.drawPolyline(line.xPointsArray.get(k), line.yPointsArray.get(k), line.yPoints.get(k).size());
            }

        }
        for (int j = 0; j < warnLevels.length; j++) {
            g.drawLine(graphArea.x, warnLevels[j], graphArea.x + graphArea.width, warnLevels[j]);
            g.drawString(warnLabels[j], graphArea.x, warnLevels[j] - 2);
        }
    }

    private void setWarn(LinkedList<Integer> _warnLevels, LinkedList<String> _warnLabels) {
        int numberOfWarnLevels = _warnLevels.size();
        warnLevels = new int[numberOfWarnLevels];
        warnLabels = new String[numberOfWarnLevels];
        int counter = 0;
        for (final Integer warnLevel : _warnLevels) {
            warnLevels[counter] = warnLevel;
            counter++;
        }
        counter = 0;
        for (final String warnLabel : _warnLabels) {
            warnLabels[counter] = warnLabel;
            counter++;
        }
    }

    private static class GraphPolyline {

        private final ArrayList<ArrayList<Integer>> xPoints;
        private final ArrayList<ArrayList<Integer>> yPoints;
        private final ArrayList<int[]> xPointsArray;
        private final ArrayList<int[]> yPointsArray;

        private final Color color;

        public GraphPolyline(final List<Point> points, final Color color, Rectangle graphArea, TimeAxis timeAxis) {
            xPoints = new ArrayList<ArrayList<Integer>>();
            yPoints = new ArrayList<ArrayList<Integer>>();
            xPointsArray = new ArrayList<int[]>();
            yPointsArray = new ArrayList<int[]>();
            this.color = color;

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
                index += jump;
                if (point.y < 10E-32) {
                    continue;
                }
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
        }
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
        return true;
    }

    public List<Interval> addRequest(Band band, Interval interval) {
        return requestCache.adaptRequestCache(interval.start, interval.end);
    }

    public List<Interval> getMissingDaysInInterval(long start, long end) {
        return requestCache.getMissingIntervals(start, end);
    }

    @Override
    public void fetchData(TimeAxis selectedAxis, TimeAxis availableAxis) {
        DownloadController.getSingletonInstance().updateBand(this, availableAxis.start, availableAxis.end);
        updateGraphsData();
    }

    @Override
    public void yaxisChanged() {
        updateGraphsData();
    }

    private static final double DISCARD_LOG_LEVEL_LOW = 1e-10;
    private static final double DISCARD_LOG_LEVEL_HIGH = 1e+4;
    private static long DAYS_PER_CHUNK = 10;

    static final long MILLIS_PER_TICK = 60000;
    static final long CHUNKED_SIZE = TimeUtils.DAY_IN_MILLIS / MILLIS_PER_TICK * DAYS_PER_CHUNK;
    static final long MILLIS_PER_CHUNK = TimeUtils.DAY_IN_MILLIS * DAYS_PER_CHUNK;

    private final HashMap<Long, EVEDataOfChunk> cacheMap = new HashMap<Long, EVEDataOfChunk>();

    private static long date2key(long date) {
        return date / MILLIS_PER_CHUNK;
    }

    public void addToCache(final float[] values, final long[] dates) {
        for (int i = 0; i < values.length; i++) {
            long key = date2key(dates[i]);

            EVEDataOfChunk cache = cacheMap.get(key);
            if (cache == null) {
                cache = new EVEDataOfChunk(key);
                cacheMap.put(key, cache);
            }
            if (values[i] > DISCARD_LOG_LEVEL_LOW && values[i] < DISCARD_LOG_LEVEL_HIGH) {
                cache.setValue((int) ((dates[i] % (MILLIS_PER_CHUNK)) / MILLIS_PER_TICK), values[i], dates[i]);
            }
        }
        updateGraphsData();
    }

    public EVEValues getValuesInInterval(TimeAxis timeAxis, Rectangle space) {
        long intervalStart = timeAxis.start;
        long intervalEnd = timeAxis.end;
        long intervalWidth = intervalEnd - intervalStart;
        int spaceWidth = space.width;
        long binStart;
        long binEnd;

        int numberOfBins;
        long timePerBin;
        if (space.width < intervalWidth / MILLIS_PER_TICK) {
            binStart = intervalStart - (intervalWidth / spaceWidth / 2);
            binEnd = intervalEnd + (intervalWidth / spaceWidth / 2);
            numberOfBins = spaceWidth + 1;
            timePerBin = intervalWidth / spaceWidth;
        } else {
            numberOfBins = (int) (intervalWidth / MILLIS_PER_TICK) + 1;
            timePerBin = intervalWidth / numberOfBins;
            binStart = intervalStart - timePerBin / 2;
            binEnd = intervalEnd + timePerBin / 2;
        }

        EVEValues result = new EVEValues(binStart, binEnd, intervalStart, numberOfBins, timePerBin);

        long keyEnd = date2key(binEnd);
        long key = date2key(binStart);
        while (key <= keyEnd) {
            EVEDataOfChunk cache = cacheMap.get(key);
            if (cache != null) {
                cache.fillResult(result);
            }
            key++;
        }

        return result;
    }

}
