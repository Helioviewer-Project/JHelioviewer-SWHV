package org.helioviewer.jhv.plugins.eveplugin.lines;

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
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class Band implements LineDataSelectorElement {

    private final BandType bandType;
    public final LineOptionPanel optionsPanel;

    private boolean isVisible = true;
    private Color graphColor = Color.BLACK;
    private YAxis yAxis;
    private final List<GraphPolyline> graphPolylines = new ArrayList<GraphPolyline>();
    private final RequestCache requestCache = new RequestCache();
    public int[] warnLevels;
    public String[] warnLabels;

    public Band(BandType _bandType) {
        bandType = _bandType;
        optionsPanel = new LineOptionPanel(this);

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
        return optionsPanel;
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
        return isVisible;
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, Rectangle leftAxisArea, TimeAxis timeAxis, Point mousePosition) {
        if (!isVisible) {
            return;
        }
        g.setColor(graphColor);
        for (GraphPolyline line : graphPolylines) {
            g.drawPolyline(line.xPoints, line.yPoints, line.yPoints.length);
        }
        for (int j = 0; j < warnLevels.length; j++) {
            g.drawLine(graphArea.x, warnLevels[j], graphArea.x + graphArea.width, warnLevels[j]);
            g.drawString(warnLabels[j], graphArea.x, warnLevels[j] - 2);
        }
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
        if (isVisible) {
            updateWarnLevels(graphArea);
            graphPolylines.clear();
            createPolyLines(timeAxis);
        }

    }

    private void createPolyLines(TimeAxis timeAxis) {
        long keyEnd = date2key(timeAxis.end);
        long key = date2key(timeAxis.start);
        int level = 0;
        double factor = 1;

        double elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
        double noelements = (timeAxis.end - timeAxis.start) / (elsz);
        double graphWidth = EVEPlugin.dc.getGraphSize().width;
        while (level < EVEDataOfChunk.MAX_LEVEL - 1 && noelements > graphWidth) {
            level++;
            factor *= EVEDataOfChunk.FACTOR_STEP;
            elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
            noelements = (timeAxis.end - timeAxis.start) / (elsz);
        }
        Rectangle graphArea = EVEPlugin.dc.getGraphArea();
        ArrayList<Integer> tvalues = new ArrayList<Integer>();
        ArrayList<Integer> tdates = new ArrayList<Integer>();
        while (key <= keyEnd) {
            EVEDataOfChunk cache = cacheMap.get(key);
            key++;
            if (cache == null)
                continue;
            float[] values = cache.getValues(level);
            long[] dates = cache.getDates(level);
            int i = 0;
            while (i < values.length) {
                float value = values[i];
                if (value <= Float.MIN_VALUE && !tvalues.isEmpty()) {
                    graphPolylines.add(new GraphPolyline(tdates, tvalues));
                    tvalues.clear();
                    tdates.clear();
                } else if (value > Float.MIN_VALUE) {
                    tdates.add(timeAxis.value2pixel(graphArea.x, graphArea.width, dates[i]));
                    tvalues.add(yAxis.value2pixel(graphArea.y, graphArea.height, value));
                }
                i++;
            }
        }
        if (!tvalues.isEmpty()) {
            graphPolylines.add(new GraphPolyline(tdates, tvalues));
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

        public final int[] xPoints;
        public final int[] yPoints;

        public GraphPolyline(List<Integer> dates, List<Integer> values) {
            int llen = dates.size();
            xPoints = new int[llen];
            yPoints = new int[llen];
            for (int j = 0; j < llen; j++) {
                xPoints[j] = dates.get(j);
                yPoints[j] = values.get(j);
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
    private static long DAYS_PER_CHUNK = 8;
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
                cache.setValue((int) ((dates[i] % (MILLIS_PER_CHUNK)) / MILLIS_PER_TICK), values[i]);
            }
        }
        updateGraphsData();
        EVEPlugin.dc.fireRedrawRequest();
    }

}
