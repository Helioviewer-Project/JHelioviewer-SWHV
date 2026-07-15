package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.LongUnaryOperator;

import javax.swing.JPanel;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.AbstractTimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

public final class Band extends AbstractTimelineLayer {

    record Data(BandType bandType, long[] dates, float[] values) {}

    private record Polyline(int[] xPoints, int[] yPoints, float[] values) {
        int length() {
            return xPoints.length;
        }
    }

    private record Bar(int x1, int y1, int x2, int y2, Color color) {}

    private static final Colors.Data bandColors = new Colors.Data();
    private static final HashMap<BandType, Band> bandMap = new HashMap<>();

    public static Band createFromType(BandType _bandType) {
        return bandMap.computeIfAbsent(_bandType, Band::new);
    }

    private static final int SUPER_SAMPLE = 1; // 8 for dots
    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    private final BandType bandType;
    private final BandOptions optionsPanel = new BandOptions(this);

    private final YAxis yAxis;
    private int[] warnPixels;
    private final List<Polyline> polylines = new ArrayList<>();
    private final List<Bar> bars = new ArrayList<>();
    private final LatestWorker<List<Object>> graphWorker = new LatestWorker<>("Timeline-Graph");

    private RequestCache requestCache;
    private BandCache bandCache;
    private Color graphColor = bandColors.getNextColor();
    private PropagationModel propagationModel = new PropagationModel.Delay(0);
    private boolean multicolor = true;
    private Runnable onColorChanged;

    public boolean drawWarnings = true;

    private Band(BandType _bandType) {
        bandType = _bandType;
        yAxis = new YAxis(bandType.getMin(), bandType.getMax(), YAxis.generateScale(bandType.getScale(), bandType.getUnitLabel()));
        warnPixels = new int[bandType.getWarningLevels().length];
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
            int r = Math.clamp(jcolor.optInt("r", 0), 0, 255);
            int g = Math.clamp(jcolor.optInt("g", 0), 0, 255);
            int b = Math.clamp(jcolor.optInt("b", 0), 0, 255);
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
        graphWorker.cancel();
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
        notifyColorChanged();
    }

    public boolean isMulticolor() {
        return multicolor;
    }

    public void setMulticolor(boolean _multicolor) {
        multicolor = _multicolor;
        updateGraph();
        notifyColorChanged();
    }

    public void setOnColorChanged(Runnable callback) {
        onColorChanged = callback;
    }

    private void notifyColorChanged() {
        if (onColorChanged != null)
            onColorChanged.run();
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

        if (!bars.isEmpty()) {
            for (Bar bar : bars) {
                g.setColor(bar.color());
                g.fillRect(bar.x1(), bar.y1(), bar.x2() - bar.x1(), bar.y2() - bar.y1());
            }
        } else if (multicolor && bandType.getLevels() != null) {
            for (Polyline line : polylines) {
                int[] xp = line.xPoints();
                int[] yp = line.yPoints();
                float[] vals = line.values();
                for (int i = 0; i < line.length() - 1; i++) {
                    Color segColor = vals != null ? bandType.getLevelColor(vals[i]) : null;
                    g.setColor(segColor != null ? segColor : graphColor);
                    g.drawLine(xp[i], yp[i], xp[i + 1], yp[i + 1]);
                }
            }
        } else {
            g.setColor(graphColor);
            polylines.forEach(line -> g.drawPolyline(line.xPoints(), line.yPoints(), line.length()));
        }

        if (drawWarnings) {
            BandType.WarningLevel[] wls = bandType.getWarningLevels();
            for (int i = 0; i < warnPixels.length; i++) {
                g.setColor(wls[i].color());
                g.drawLine(graphArea.x, warnPixels[i], graphArea.x + graphArea.width, warnPixels[i]);
                g.drawString(wls[i].label(), graphArea.x, warnPixels[i] - 2);
            }
        }
    }

    private void updateGraph() {
        if (!enabled) {
            graphWorker.cancel();
            return;
        }

        GraphGeometry geometry = DrawController.getGeometry();
        Rectangle graphArea = geometry.area();
        Rectangle drawArea = TimelineLayers.getDrawArea(this, graphArea);
        YAxis.Mapper yMapper = geometry.yMapper(yAxis, drawArea);

        BandType.WarningLevel[] wls = bandType.getWarningLevels();
        for (int i = 0; i < warnPixels.length; i++) {
            warnPixels[i] = yMapper.dataToPixel(wls[i].value());
        }

        TimeAxis timeAxis = DrawController.selectedAxis;
        long start = propagationModel.getObservationTime(timeAxis.start());
        long end = propagationModel.getObservationTime(timeAxis.end());

        LongUnaryOperator viewpointTime = propagationModel.viewpointTimeMapper();
        TimeAxis.Mapper xMapper = geometry.xMapper(timeAxis);
        final boolean isBar = "bar".equals(bandType.getPlotType());
        final long barWidthMillis = isBar ? bandType.getBarWidth() * 1000 : 0;
        List<List<BandCache.DateValue>> rawData = bandCache.getValues(SUPER_SAMPLE * Display.pixelScale[0] * drawArea.width, start - barWidthMillis, end + barWidthMillis);
        final int baselineY = yMapper.dataToPixel(0);
        final boolean useMulticolor = multicolor;

        graphWorker.submit(() -> {
                    List<Object> result = new ArrayList<>();
                    for (List<BandCache.DateValue> list : rawData) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }
                        int size = list.size();
                        if (size == 0) {
                            continue;
                        }

                        int[] dates = new int[size];
                        int[] yPixels = new int[size];
                        float[] floatValues = new float[size];
                        for (int i = 0; i < size; i++) {
                            BandCache.DateValue dv = list.get(i);
                            dates[i] = xMapper.toPixel(viewpointTime.applyAsLong(dv.milli));
                            yPixels[i] = yMapper.dataToPixel(dv.value);
                            floatValues[i] = dv.value;
                        }

                        if (isBar) {
                            for (int i = 0; i < size; i++) {
                                Color barColor = useMulticolor ? bandType.getLevelColor(floatValues[i]) : null;
                                if (barColor == null)
                                    barColor = graphColor;
      int right = dates[i];
                                 int left = xMapper.toPixel(viewpointTime.applyAsLong(list.get(i).milli) - barWidthMillis);
                                 int barWidth = Math.max(1, right - left);
                                 int gap = barWidth / 40;
                                int top = Math.min(yPixels[i], baselineY);
                                int bottom = Math.max(yPixels[i], baselineY);
                                result.add(new Bar(left + gap, top, right, bottom, barColor));
                            }
                        } else {
                            result.add(new Polyline(dates, yPixels, (useMulticolor && bandType.getLevels() != null) ? floatValues : null));
                        }
                    }
                    return result;
                },
                (result, fresh) -> {
                    if (!fresh)
                        return;

                    polylines.clear();
                    bars.clear();
                    for (Object o : result) {
                        if (o instanceof Polyline pl)
                            polylines.add(pl);
                        else if (o instanceof Bar bar)
                            bars.add(bar);
                    }
                    DrawController.drawRequest();
                });
    }

    @Override
    public String getStringValue(long ts) {
        float val = bandCache.getValue(propagationModel.getObservationTime(ts));
        if (val == YAxis.BLANK) {
            return "--";
        }
        return DrawConstants.valueFormatter.format(yAxis.scale(val));
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
        updateGraph();
    }

    @Override
    public void yaxisChanged() {
        updateGraph();
    }

    boolean addToCache(float[] values, long[] dates) {
        boolean hadData = bandCache.hasData();
        bandCache.addToCache(yAxis, values, dates);
        updateGraph();
        DrawController.drawRequest();
        return !hadData && bandCache.hasData();
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
