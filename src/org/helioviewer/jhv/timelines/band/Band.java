package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.interval.RequestCache;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.timelines.AbstractTimelineLayer;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.propagation.PropagationModel;
import org.helioviewer.jhv.timelines.propagation.PropagationModelRadial;
import org.json.JSONArray;
import org.json.JSONObject;

public class Band extends AbstractTimelineLayer {

    private static final int SUPER_SAMPLE = 1; // 8 for dots

    private final BandType bandType;
    private final BandCache bandCache;
    private final BandOptionPanel optionsPanel;
    private final RequestCache requestCache = new RequestCache();

    private final YAxis yAxis;

    private Color graphColor;
    private int[] warnLevels;
    private String[] warnLabels;

    private final ArrayList<GraphPolyline> graphPolylines = new ArrayList<>();
    private PropagationModel propagationModel = new PropagationModelRadial(0);

    public Band(BandType _bandType) {
        if (_bandType.getBandCacheType().equals("BandCacheAll")) {
            bandCache = new BandCacheAll();
        } else {
            bandCache = new BandCacheMinute();
        }
        bandType = _bandType;
        optionsPanel = new BandOptionPanel(this);
        yAxis = new YAxis(bandType.getMin(), bandType.getMax(), YAxis.generateScale(bandType.getScale(), bandType.getUnitLabel()));
        graphColor = BandColors.getNextColor();
        fillWarnLevels();
    }

    private void fillWarnLevels() {
        Map<String, Double> unconvertedWarnLevels = bandType.getWarnLevels();
        int i = 0, size = unconvertedWarnLevels.size();
        warnLevels = new int[size];
        warnLabels = new String[size];
        for (String label : unconvertedWarnLevels.keySet()) {
            warnLabels[i] = label;
            i++;
        }
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("timeline", toString());

        TimeAxis timeAxis = DrawController.selectedAxis;
        long start = propagationModel.getObservationTime(timeAxis.start());
        long end = propagationModel.getObservationTime(timeAxis.end());
        float[] bounds = bandCache.getBounds(start, end);

        double multiplier = 1;
        if (bounds[0] != 0 && Float.isFinite(bounds[0]) && Float.isFinite(bounds[1])) {
            multiplier = bounds[0];
        }

        jo.put("multiplier", multiplier);
        bandCache.serialize(jo, 1 / multiplier);
        bandType.serialize(jo);
        return new JSONObject().put("org.helioviewer.jhv.request.timeline", new JSONArray().put(jo));
    }

    @Override
    public void serialize(JSONObject jo) {
        bandType.serialize(jo);
        jo.put("color", new JSONObject().put("r", graphColor.getRed()).put("g", graphColor.getGreen()).put("b", graphColor.getBlue()));
    }

    @Override
    public void resetAxis() {
        yAxis.reset(bandType.getMin(), bandType.getMax());
        updateGraphsData();
    }

    @Override
    public void zoomToFitAxis() {
        TimeAxis timeAxis = DrawController.selectedAxis;
        long start = propagationModel.getObservationTime(timeAxis.start());
        long end = propagationModel.getObservationTime(timeAxis.end());

        float[] bounds = bandCache.getBounds(start, end);
        if (bounds[0] == bounds[1]) {
            resetAxis();
            return;
        }

        if (Float.isFinite(bounds[0]) && Float.isFinite(bounds[1])) {
            yAxis.reset(bounds[0], bounds[1]);
            updateGraphsData();
        }
    }

    public BandType getBandType() {
        return bandType;
    }

    @Override
    public void remove() {
        BandDataProvider.stopDownloads(this);
        BandColors.resetColor(graphColor);
    }

    @Override
    public String getName() {
        return bandType.toString();
    }

    @Override
    public Color getDataColor() {
        return graphColor;
    }

    public void setDataColor(Color c) {
        graphColor = c;
        DrawController.drawRequest();
    }

    @Override
    public boolean isDownloading() {
        return BandDataProvider.isDownloadActive(this);
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
        return enabled;
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
        if (!enabled)
            return;

        g.setColor(graphColor);
        graphPolylines.forEach(line -> g.drawPolyline(line.xPoints(), line.yPoints(), line.length())); // polylines

//      for (GraphPolyline line : graphPolylines) { // dots
//          int length = line.length();
//          int[] xPoints = line.xPoints();
//          int[] yPoints = line.yPoints();
//          for (int i = 0; i < length; i++)
//              g.drawLine(xPoints[i], yPoints[i], xPoints[i], yPoints[i]);
//      }

        for (int j = 0; j < warnLevels.length; j++) {
            g.drawLine(graphArea.x, warnLevels[j], graphArea.x + graphArea.width, warnLevels[j]);
            g.drawString(warnLabels[j], graphArea.x, warnLevels[j] - 2);
        }
    }

    private void updateWarnLevels(Rectangle graphArea) {
        Map<String, Double> unconvertedWarnLevels = bandType.getWarnLevels();
        for (int i = 0; i < warnLabels.length; i++)
            warnLevels[i] = yAxis.value2pixel(graphArea.y, graphArea.height, unconvertedWarnLevels.get(warnLabels[i]));
    }

    private void updateGraphsData() {
        if (enabled) {
            Rectangle graphArea = DrawController.getGraphArea();
            updateWarnLevels(graphArea);
            graphPolylines.clear();

            TimeAxis timeAxis = DrawController.selectedAxis;
            long start = propagationModel.getObservationTime(timeAxis.start());
            long end = propagationModel.getObservationTime(timeAxis.end());
            for (List<DateValue> list : bandCache.getValues(SUPER_SAMPLE * GLInfo.pixelScaleFloat[0] * graphArea.width, start, end)) {
                if (!list.isEmpty()) {
                    IntArray dates = new IntArray(list.size());
                    IntArray values = new IntArray(list.size());
                    for (DateValue dv : list) {
                        dates.put(timeAxis.value2pixel(graphArea.x, graphArea.width, propagationModel.getViewpointTime(dv.milli)));
                        values.put(yAxis.value2pixel(graphArea.y, graphArea.height, dv.value));
                    }
                    graphPolylines.add(new GraphPolyline(dates, values));
                }
            }
        }
    }

    @Override
    public String getStringValue(long ts) {
        float val = bandCache.getValue(propagationModel.getObservationTime(ts));
        if (val == YAxis.BLANK) {
            return "--";
        } else if (bandType.getName().contains("XRSB")) {
            return GOESLevel.getStringValue(val);
        } else {
            return DrawConstants.valueFormatter.format(yAxis.scale(val));
        }
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    List<Interval> addRequest(long start, long end) {
        return requestCache.adaptRequestCache(start, end);
    }

    List<Interval> getMissingDaysInInterval(long start, long end) {
        return requestCache.getMissingIntervals(start, end);
    }

    @Override
    public void fetchData(TimeAxis timeAxis) {
        long start = propagationModel.getObservationTime(timeAxis.start());
        long end = propagationModel.getObservationTime(timeAxis.end());
        BandDataProvider.updateBand(this, start, end);
        updateGraphsData();
    }

    @Override
    public void yaxisChanged() {
        updateGraphsData();
    }

    void addToCache(float[] values, long[] dates) {
        bandCache.addToCache(yAxis, values, dates);
        updateGraphsData();
        DrawController.drawRequest();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Band && o.toString().equals(toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return bandType.getName();
    }

    @Override
    public boolean isPropagated() {
        return propagationModel.isPropagated();
    }

    @Override
    public long getObservationTime(long ts) {
        return propagationModel.getObservationTime(ts);
    }

    void setPropagationModel(PropagationModel _propagationModel) {
        propagationModel = _propagationModel;
        DrawController.graphAreaChanged();
    }

}
