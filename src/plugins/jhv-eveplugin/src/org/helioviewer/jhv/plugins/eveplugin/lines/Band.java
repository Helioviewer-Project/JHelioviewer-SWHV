package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.AbstractLineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

public class Band extends AbstractLineDataSelectorElement {

    private final BandType bandType;
    private final LineOptionPanel optionsPanel;

    private Color graphColor = Color.BLACK;
    private final YAxis yAxis;
    private final ArrayList<BandCache.GraphPolyline> graphPolylines = new ArrayList<>();
    private final RequestCache requestCache = new RequestCache();
    private int[] warnLevels;
    private String[] warnLabels;
    private final BandCache bandCache = new BandCache();

    public Band(BandType _bandType) {
        bandType = _bandType;
        optionsPanel = new LineOptionPanel(this);

        DrawController.fireRedrawRequest();
        LineDataSelectorModel.addLineData(this);
        yAxis = new YAxis(bandType.getMin(), bandType.getMax(), bandType.getUnitLabel(), bandType.isLogScale());
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void resetAxis() {
        yAxis.reset(bandType.getMin(), bandType.getMax());
        updateGraphsData();
    }

    @Override
    public void zoomToFitAxis() {
        float[] bounds = bandCache.getBounds(DrawController.selectedAxis);
        if (bounds[0] == bounds[1]) {
            resetAxis();
            return;
        }

        if (bounds[0] != Float.MIN_VALUE && bounds[1] != Float.MIN_VALUE) {
            yAxis.reset(bounds[0], bounds[1]);
            updateGraphsData();
        }
    }

    public BandType getBandType() {
        return bandType;
    }

    @Override
    public void removeLineData() {
        LineDataSelectorModel.removeLineData(this);
        BandColors.resetColor(getDataColor());
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
        DrawController.fireRedrawRequest();
    }

    @Override
    public boolean isDownloading() {
        return DownloadController.isDownloadActive(this);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public boolean hasData() {
        return bandCache.hasData();
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
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
        if (!isVisible) {
            return;
        }
        g.setColor(graphColor);
        for (BandCache.GraphPolyline line : graphPolylines) {
            g.drawPolyline(line.xPoints, line.yPoints, line.yPoints.length);
        }
        for (int j = 0; j < warnLevels.length; j++) {
            g.drawLine(graphArea.x, warnLevels[j], graphArea.x + graphArea.width, warnLevels[j]);
            g.drawString(warnLabels[j], graphArea.x, warnLevels[j] - 2);
        }
    }

    private void updateWarnLevels(Rectangle graphArea) {
        LinkedList<Integer> _warnLevels = new LinkedList<>();
        LinkedList<String> _warnLabels = new LinkedList<>();
        HashMap<String, Double> unconvertedWarnLevels = bandType.getWarnLevels();
        for (Map.Entry<String, Double> pairs : unconvertedWarnLevels.entrySet()) {
            _warnLevels.add(yAxis.value2pixel(graphArea.y, graphArea.height, pairs.getValue()));
            _warnLabels.add(pairs.getKey());
        }
        setWarn(_warnLevels, _warnLabels);
    }

    private void updateGraphsData() {
        Rectangle graphArea = DrawController.getGraphArea();
        TimeAxis timeAxis = DrawController.selectedAxis;
        if (isVisible) {
            updateWarnLevels(graphArea);
            graphPolylines.clear();
            bandCache.createPolyLines(graphArea, timeAxis, yAxis, graphPolylines);
        }
    }

    @Override
    public String getStringValue(long ts) {
        float val = bandCache.getValue(ts);
        if (val == Float.MIN_VALUE) {
            return "--";
        } else if (bandType.getName().contains("XRSB")) {
            return GOESLevel.getStringValue(val);
        } else {
            return DrawConstants.valueFormatter.format(yAxis.scale(val));
        }
    }

    private void setWarn(LinkedList<Integer> _warnLevels, LinkedList<String> _warnLabels) {
        int numberOfWarnLevels = _warnLevels.size();
        warnLevels = new int[numberOfWarnLevels];
        warnLabels = new String[numberOfWarnLevels];
        int counter = 0;
        for (Integer warnLevel : _warnLevels) {
            warnLevels[counter] = warnLevel;
            counter++;
        }
        counter = 0;
        for (String warnLabel : _warnLabels) {
            warnLabels[counter] = warnLabel;
            counter++;
        }
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    public List<Interval> addRequest(long start, long end) {
        return requestCache.adaptRequestCache(start, end);
    }

    public List<Interval> getMissingDaysInInterval(long start, long end) {
        return requestCache.getMissingIntervals(start, end);
    }

    @Override
    public void fetchData(TimeAxis selectedAxis) {
        DownloadController.updateBand(this, selectedAxis.start, selectedAxis.end);
        updateGraphsData();
    }

    @Override
    public void yaxisChanged() {
        updateGraphsData();
    }

    public void addToCache(float[] values, long[] dates) {
        bandCache.addToCache(values, dates);
        updateGraphsData();
        DrawController.fireRedrawRequest();
    }

    @Override
    public boolean highLightChanged(Point p) {
        return false;
    }

    @Override
    public void drawHighlighted(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
    }

    @Override
    public boolean hasDataColor() {
        return true;
    }

    @Override
    public boolean hasValueAsString() {
        return true;
    }

    @Override
    public Object getElementUnderMouse() {
        return null;
    }

}
