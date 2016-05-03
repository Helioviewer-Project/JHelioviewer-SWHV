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
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandCache.GraphPolyline;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class Band implements LineDataSelectorElement {

    private final BandType bandType;
    private final LineOptionPanel optionsPanel;

    private boolean isVisible = true;
    private Color graphColor = Color.BLACK;
    private YAxis yAxis;
    private final ArrayList<BandCache.GraphPolyline> graphPolylines = new ArrayList<BandCache.GraphPolyline>();
    private final RequestCache requestCache = new RequestCache();
    private int[] warnLevels;
    private String[] warnLabels;
    private final BandCache bandCache = new BandCache();

    public Band(BandType _bandType) {
        bandType = _bandType;
        optionsPanel = new LineOptionPanel(this);

        EVEPlugin.dc.fireRedrawRequest();
        EVEPlugin.ldsm.addLineData(this);
        yAxis = new YAxis(bandType.getMin(), bandType.getMax(), bandType.getUnitLabel(), bandType.isLogScale());
    }

    @Override
    public void resetAxis() {
        yAxis.reset(bandType.getMin(), bandType.getMax());
        updateGraphsData();
    }

    @Override
    public void zoomToFitAxis() {
        float[] bounds = bandCache.getBounds(EVEPlugin.dc.selectedAxis);
        if (bounds[0] != Float.MIN_VALUE && bounds[1] != Float.MIN_VALUE) {
            yAxis.reset(bounds[0], bounds[1]);
            updateGraphsData();
        }
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
            bandCache.createPolyLines(timeAxis, yAxis, graphPolylines);
        }
    }

    public String getStringValue(long ts) {
        float val = bandCache.getValue(ts);
        if (val == Float.MIN_VALUE)
            return "--";
        else if (!bandType.getName().contains("XRSB"))
            return DrawConstants.valueFormatter.format(yAxis.scale(val));
        else {
            double v = val * 1e8;
            if (v < 1e1)
                return String.format("A%.1f", v);
            else if (v < 1e2)
                return String.format("B%.1f", v * 1e-1);
            else if (v < 1e3)
                return String.format("C%.1f", v * 1e-2);
            else if (v < 1e4)
                return String.format("M%.1f", v * 1e-3);
            else
                return String.format("X%.1f", v * 1e-4);
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

    public void addToCache(float[] values, long[] dates) {
        bandCache.addToCache(values, dates);
        updateGraphsData();
        EVEPlugin.dc.fireRedrawRequest();
    }

}
