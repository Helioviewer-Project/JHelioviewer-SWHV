package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JPanel;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.base.GOESLevel;
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

    record Data(BandType bandType, long[] dates, float[] values) {
    }

    private record Polyline(int[] xPoints, int[] yPoints) {
        int length() {
            return xPoints.length;
        }
    }

    private static final Colors.BrightColors bandColors = new Colors.BrightColors();
    private static final HashMap<BandType, Band> bandMap = new HashMap<>();

    public static Band createFromType(BandType _bandType) {
        return bandMap.computeIfAbsent(_bandType, Band::new);
    }

    private static final int SUPER_SAMPLE = 1; // 8 for dots
    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    private final BandType bandType;
    private final BandOptions optionsPanel = new BandOptions(this);

    private final YAxis yAxis;
    private final int[] warnLevels;
    private final List<Polyline> polylines = new ArrayList<>();

    private RequestCache requestCache;
    private BandCache bandCache;
    private Color graphColor = bandColors.getNextColor();
    private PropagationModel propagationModel = new PropagationModel.Delay(0);

    private Band(BandType _bandType) {
        bandType = _bandType;
        yAxis = new YAxis(bandType.getMin(), bandType.getMax(), YAxis.generateScale(bandType.getScale(), bandType.getUnitLabel()));
        warnLevels = new int[bandType.getWarnLevels().length];
        // those should be cleared
        requestCache = new RequestCache();
        bandCache = createBandCache(bandType.getBandCacheType());
    }

    private static BandCache createBandCache(String cacheType) {
        return "BandCacheAll".equals(cacheType) ? new BandCacheAll() : new BandCacheMinute();
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

    public static AbstractTimelineLayer deserialize(JSONObject jo) throws Exception { // has to be implemented for state
        JSONObject jobt = jo.optJSONObject("bandType");
        if (jobt == null)
            throw new Exception("Missing bandType: " + jo);
        Band band = createFromType(new BandType(jobt));

        JSONObject jcolor = jo.optJSONObject("color");
        if (jcolor != null) {
            int r = MathUtils.clip(jcolor.optInt("r", 0), 0, 255);
            int g = MathUtils.clip(jcolor.optInt("g", 0), 0, 255);
            int b = MathUtils.clip(jcolor.optInt("b", 0), 0, 255);
            band.setDataColor(new Color(r, g, b));
        }
        return band;
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
        // clear caches
        requestCache = new RequestCache();
        bandCache = createBandCache(bandType.getBandCacheType());
    }

    @Override
    public String getName() {
        return bandType.toString();
    }

    @Override
    public Color getDataColor() {
        return graphColor;
    }

    void setDataColor(Color c) {
        graphColor = c;
        DrawController.drawRequest();
    }

    @Override
    public boolean isDownloading() {
        return BandDataProvider.isDownloadActive(this);
    }

    @Override
    public JPanel getOptionsPanel() {
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
        polylines.forEach(line -> g.drawPolyline(line.xPoints(), line.yPoints(), line.length())); // polylines

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

            polylines.clear();

            TimeAxis timeAxis = DrawController.selectedAxis;
            long start = propagationModel.getObservationTime(timeAxis.start());
            long end = propagationModel.getObservationTime(timeAxis.end());
            for (List<BandCache.DateValue> list : bandCache.getValues(SUPER_SAMPLE * GLInfo.pixelScale[0] * graphArea.width, start, end)) {
                int size = list.size();
                if (size == 0)
                    continue;

                int[] dates = new int[size];
                int[] values = new int[size];
                for (int i = 0; i < size; i++) {
                    BandCache.DateValue dv = list.get(i);
                    dates[i] = timeAxis.value2pixel(graphArea.x, graphArea.width, propagationModel.getViewpointTime(dv.milli));
                    values[i] = yAxis.value2pixel(graphArea.y, graphArea.height, dv.value);
                }
                polylines.add(new Polyline(dates, values));
            }
        }
    }

    @Override
    public String getStringValue(long ts) {
        float val = bandCache.getValue(propagationModel.getObservationTime(ts));
        if (val == YAxis.BLANK) {
            return "--";
        } else if (bandType.isXRSB()) {
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
