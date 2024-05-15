package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.interval.RequestCache;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.AbstractTimelineLayer;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONArray;
import org.json.JSONObject;

public final class Band extends AbstractTimelineLayer {

    private static final HashMap<BandType, Band> externalLoad = new HashMap<>();

    public static Band createFromType(BandType _bandType) {
        return externalLoad.computeIfAbsent(_bandType, Band::new);
    }

    private static final int SUPER_SAMPLE = 1; // 8 for dots
    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    private final BandType bandType;
    private final BandCache bandCache;
    private final BandOptionPanel optionsPanel = new BandOptionPanel(this);
    private final RequestCache requestCache = new RequestCache();

    private final YAxis yAxis;
    private final int[] warnLevels;
    private final List<GraphPolyline> graphPolylines = new ArrayList<>();

    private Color graphColor = BandColors.getNextColor();
    private PropagationModel propagationModel = new PropagationModel.Delay(0);

    private Band(BandType _bandType) {
        bandType = _bandType;
        bandCache = bandType.getBandCacheType().equals("BandCacheAll") ? new BandCacheAll() : new BandCacheMinute();
        yAxis = new YAxis(bandType.getMin(), bandType.getMax(), YAxis.generateScale(bandType.getScale(), bandType.getUnitLabel()));
        warnLevels = new int[bandType.getWarnLevels().length];
    }

    public Band(JSONObject jo) { // used by load state
        this(jo.optJSONObject("bandType") == null ? BandType.getBandType("GOES_XRSB_ODI") : new BandType(jo.getJSONObject("bandType")));

        JSONObject jcolor = jo.optJSONObject("color");
        if (jcolor != null) {
            int r = MathUtils.clip(jcolor.optInt("r", 0), 0, 255);
            int g = MathUtils.clip(jcolor.optInt("g", 0), 0, 255);
            int b = MathUtils.clip(jcolor.optInt("b", 0), 0, 255);
            graphColor = new Color(r, g, b);
        }
    }

    JSONObject toJson() {
        TimeAxis timeAxis = DrawController.selectedAxis;
        long start = propagationModel.getObservationTime(timeAxis.start());
        long end = propagationModel.getObservationTime(timeAxis.end());
        float[] bounds = bandCache.getBounds(start, end);

        double multiplier = 1;
        if (bounds[0] != 0 && Float.isFinite(bounds[0]) && Float.isFinite(bounds[1])) {
            multiplier = bounds[0];
        }

        JSONObject jo = new JSONObject();
        jo.put("timeline", toString());
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
        updateGraph();
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
            updateGraph();
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

        String[] warnLabels = bandType.getWarnLabels();
        for (int i = 0; i < warnLevels.length; i++) {
            g.drawLine(graphArea.x, warnLevels[i], graphArea.x + graphArea.width, warnLevels[i]);
            g.drawString(warnLabels[i], graphArea.x, warnLevels[i] - 2);
        }
    }

    private void updateGraph() {
        if (enabled) {
            Rectangle graphArea = DrawController.getGraphArea();

            double[] unconvertedWarnLevels = bandType.getWarnLevels();
            for (int i = 0; i < warnLevels.length; i++) {
                warnLevels[i] = yAxis.value2pixel(graphArea.y, graphArea.height, unconvertedWarnLevels[i]);
            }

            graphPolylines.clear();

            TimeAxis timeAxis = DrawController.selectedAxis;
            long start = propagationModel.getObservationTime(timeAxis.start());
            long end = propagationModel.getObservationTime(timeAxis.end());
            for (List<DateValue> list : bandCache.getValues(SUPER_SAMPLE * GLInfo.pixelScale[0] * graphArea.width, start, end)) {
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

    private void updateData(long start, long end) {
        List<Interval> missingIntervals = requestCache.getMissingIntervals(start, end);
        if (!missingIntervals.isEmpty()) {
            // extend
            start -= 7 * TimeUtils.DAY_IN_MILLIS;
            end += 7 * TimeUtils.DAY_IN_MILLIS;

            List<Interval> intervals = new ArrayList<>();
            requestCache.adaptRequestCache(start, end).forEach(interval -> intervals.addAll(Interval.splitInterval(interval, DOWNLOADER_MAX_DAYS_PER_BLOCK)));
            BandDataProvider.addDownloads(this, intervals);
        }
    }

    @Override
    public void fetchData(TimeAxis timeAxis) {
        long start = propagationModel.getObservationTime(timeAxis.start());
        long end = propagationModel.getObservationTime(timeAxis.end());
        updateData(start, end);
        updateGraph(); //?
    }

    @Override
    public void yaxisChanged() {
        updateGraph();
    }

    void addToCache(float[] values, long[] dates) {
        bandCache.addToCache(yAxis, values, dates);
        updateGraph();
        DrawController.drawRequest();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
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
