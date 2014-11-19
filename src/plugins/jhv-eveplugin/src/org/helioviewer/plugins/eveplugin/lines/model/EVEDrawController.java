package org.helioviewer.plugins.eveplugin.lines.model;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.download.DownloadedData;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.BandControllerListener;
import org.helioviewer.plugins.eveplugin.lines.data.EVECacheController;
import org.helioviewer.plugins.eveplugin.lines.data.EVECacheControllerListener;
import org.helioviewer.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.plugins.eveplugin.lines.gui.EVEDrawableElement;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * @author Stephan Pagel
 * */
public class EVEDrawController implements BandControllerListener, ZoomControllerListener, EVECacheControllerListener, LayersListener,
        PlotAreaSpaceListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final String identifier;

    private final LinkedList<EVEDrawControllerListener> listeners = new LinkedList<EVEDrawControllerListener>();
    private final Map<String, Map<Band, DownloadedData>> dataMapPerUnitLabel = new HashMap<String, Map<Band, DownloadedData>>();

    private Interval<Date> interval = new Interval<Date>(null, null);
    private final Map<String, Range> selectedRangeMap = new HashMap<String, Range>();
    private final Map<String, Range> availableRangeMap = new HashMap<String, Range>();
    private final DrawController drawController;

    private final Map<String, EVEDrawableElement> eveDrawableElementMap;
    private final Map<String, YAxisElement> yAxisElementMap;

    private final PlotAreaSpace plotAreaSpace;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public EVEDrawController(final String identifier) {
        this.identifier = identifier;

        BandController.getSingletonInstance().addBandControllerListener(this);
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        EVECacheController.getSingletonInstance().addControllerListener(this);
        LayersModel.getSingletonInstance().addLayersListener(this);
        drawController = DrawController.getSingletonInstance();
        eveDrawableElementMap = new HashMap<String, EVEDrawableElement>();
        yAxisElementMap = new HashMap<String, YAxisElement>();
        selectedRangeMap.put("", new Range());
        availableRangeMap.put("", new Range());
        yAxisElementMap.put("", new YAxisElement());
        eveDrawableElementMap.put("", new EVEDrawableElement());
        plotAreaSpace = PlotAreaSpaceManager.getInstance().getPlotAreaSpace(identifier);
        plotAreaSpace.addPlotAreaSpaceListener(this);
    }

    public void addDrawControllerListener(final EVEDrawControllerListener listener) {
        listeners.add(listener);
    }

    public void removeDrawControllerListener(final EVEDrawControllerListener listener) {
        listeners.remove(listener);
    }

    private void addToMap(final Band band) {
        DownloadedData data = retrieveData(band, interval);
        if (!dataMapPerUnitLabel.containsKey(band.getUnitLabel())) {
            dataMapPerUnitLabel.put(band.getUnitLabel(), new HashMap<Band, DownloadedData>());
        }
        if (data != null) {
            dataMapPerUnitLabel.get(band.getUnitLabel()).put(band, data);
        }
        fireRedrawRequest(true);
    }

    private void removeFromMap(final Band band) {
        if (dataMapPerUnitLabel.containsKey(band.getUnitLabel())) {
            if (dataMapPerUnitLabel.get(band.getUnitLabel()).containsKey(band)) {
                dataMapPerUnitLabel.get(band.getUnitLabel()).remove(band);
                if (dataMapPerUnitLabel.get(band.getUnitLabel()).isEmpty()) {
                    EVEDrawableElement removed = eveDrawableElementMap.remove(band.getUnitLabel());
                    availableRangeMap.remove(band.getUnitLabel());
                    selectedRangeMap.remove(band.getUnitLabel());
                    yAxisElementMap.remove(band.getUnitLabel());
                    drawController.removeDrawableElement(removed, identifier);
                }
                fireRedrawRequest(true);
            }
        }
    }

    private void updateBand(final Band band, boolean keepFullValueRange) {
        DownloadedData data = retrieveData(band, interval);
        boolean isLog = band.getBandType().isLogScale();
        if (!availableRangeMap.containsKey(band.getUnitLabel())) {
            availableRangeMap.put(band.getUnitLabel(), new Range());
            selectedRangeMap.put(band.getUnitLabel(), new Range());
            dataMapPerUnitLabel.put(band.getUnitLabel(), new HashMap<Band, DownloadedData>());
        }
        Range oldAvailableRange = new Range(availableRangeMap.get(band.getUnitLabel()));
        for (DownloadedData v : dataMapPerUnitLabel.get(band.getUnitLabel()).values()) {
            if (v != null) {
                availableRangeMap.get(band.getUnitLabel()).setMin(v.getMinimumValue());
                availableRangeMap.get(band.getUnitLabel()).setMax(v.getMaximumValue());
            }

        }
        availableRangeMap.get(band.getUnitLabel()).setMin(data.getMinimumValue());
        availableRangeMap.get(band.getUnitLabel()).setMax(data.getMaximumValue());
        if (oldAvailableRange.min != availableRangeMap.get(band.getUnitLabel()).min
                || oldAvailableRange.max != availableRangeMap.get(band.getUnitLabel()).max) {
            // Log.trace("update band available range changed so we change the plotareaSpace");
            checkSelectedRange(availableRangeMap.get(band.getUnitLabel()), selectedRangeMap.get(band.getUnitLabel()));
            updatePlotAreaSpace(availableRangeMap.get(band.getUnitLabel()), selectedRangeMap.get(band.getUnitLabel()), keepFullValueRange,
                    isLog);
        } else {
            // Log.trace("Same available range");
        }
        dataMapPerUnitLabel.get(band.getUnitLabel()).put(band, data);
    }

    private void updateBands(boolean keepFullValueRange) {
        for (String unit : dataMapPerUnitLabel.keySet()) {
            for (final Band band : dataMapPerUnitLabel.get(unit).keySet()) {
                updateBand(band, keepFullValueRange);
            }
        }
    }

    public void setSelectedRange(final Range newSelectedRange, String unitLabel) {
        selectedRangeMap.put(unitLabel, new Range(newSelectedRange));
        drawController.setSelectedRange(newSelectedRange);
        fireRedrawRequest(false);
    }

    public void setSelectedRangeMaximal() {
        fireRedrawRequest(true);
    }

    private void fireRedrawRequest(final boolean maxRange) {
        for (String unit : dataMapPerUnitLabel.keySet()) {
            final Band[] bands = dataMapPerUnitLabel.get(unit).keySet().toArray(new Band[0]);
            final LinkedList<DownloadedData> values = new LinkedList<DownloadedData>();

            String unitLabel = "";
            boolean isLog = false;
            if (bands.length > 0) {
                unitLabel = bands[0].getUnitLabel();
                isLog = bands[0].getBandType().isLogScale();
            }

            if (!availableRangeMap.containsKey(unitLabel)) {
                availableRangeMap.put(unitLabel, new Range());
                selectedRangeMap.put(unitLabel, new Range());
                eveDrawableElementMap.put(unitLabel, new EVEDrawableElement());
            }

            Range oldAvailableRange = new Range(availableRangeMap.get(unitLabel));

            for (DownloadedData v : dataMapPerUnitLabel.get(unit).values()) {
                if (v != null) {
                    availableRangeMap.get(unitLabel).setMin(v.getMinimumValue());
                    availableRangeMap.get(unitLabel).setMax(v.getMaximumValue());
                    values.add(v);
                }
            }

            if (maxRange) {
                selectedRangeMap.put(unitLabel, new Range());
            }
            checkSelectedRange(availableRangeMap.get(unitLabel), selectedRangeMap.get(unitLabel));
            if (oldAvailableRange.min != availableRangeMap.get(unitLabel).min
                    || oldAvailableRange.max != availableRangeMap.get(unitLabel).max) {
                Log.error("Available range changed in redraw request. So update plotAreaSpace");
                Log.error("old range : " + oldAvailableRange.toString());
                Log.error("new available range : " + availableRangeMap.get(unitLabel).toString());
                updatePlotAreaSpace(availableRangeMap.get(unitLabel), selectedRangeMap.get(unitLabel), false, isLog);

            }

            for (EVEDrawControllerListener listener : listeners) {
                listener.drawRequest(interval, bands, values.toArray(new EVEValues[0]), availableRangeMap.get(unitLabel),
                        selectedRangeMap.get(unitLabel));
            }
            YAxisElement yAxisElement = new YAxisElement();
            if (yAxisElementMap.containsKey(unitLabel)) {
                yAxisElement = yAxisElementMap.get(unitLabel);
            }
            yAxisElement.set(selectedRangeMap.get(unitLabel), availableRangeMap.get(unitLabel), unitLabel,
                    selectedRangeMap.get(unitLabel).min, selectedRangeMap.get(unitLabel).max, Color.PINK, isLog,
                    yAxisElement.getActivationTime());
            yAxisElementMap.put(unitLabel, yAxisElement);
            eveDrawableElementMap.get(unitLabel).set(interval, bands, values.toArray(new EVEValues[0]), yAxisElement);
            if (bands.length > 0) {
                drawController.updateDrawableElement(eveDrawableElementMap.get(unitLabel), identifier);
            } else {
                drawController.removeDrawableElement(eveDrawableElementMap.get(unitLabel), identifier);
            }
        }
    }

    private void updatePlotAreaSpace(Range availableRange, Range selectedRange, boolean keepFullValueSpace, boolean isLog) {
        if (!keepFullValueSpace) {
            if (isLog) {
                double diffAvailable = Math.log10(availableRange.max) - Math.log10(availableRange.min);
                double diffStart = Math.log10(selectedRange.min) - Math.log10(availableRange.min);
                double diffEnd = Math.log10(selectedRange.max) - Math.log10(availableRange.min);
                double startValue = plotAreaSpace.getScaledMinValue() + diffStart / diffAvailable;
                double endValue = plotAreaSpace.getScaledMinValue() + diffEnd / diffAvailable;
                plotAreaSpace.setScaledSelectedValue(startValue, endValue, true);
            } else {
                double diffAvailable = availableRange.max - availableRange.min;
                double diffStart = selectedRange.min - availableRange.min;
                double diffEnd = selectedRange.max - availableRange.min;
                double startValue = plotAreaSpace.getScaledMinValue() + diffStart / diffAvailable;
                double endValue = plotAreaSpace.getScaledMinValue() + diffEnd / diffAvailable;
                plotAreaSpace.setScaledSelectedValue(startValue, endValue, true);
            }
        } else {
            plotAreaSpace.setScaledSelectedValue(plotAreaSpace.getScaledMinValue(), plotAreaSpace.getScaledMaxValue(), true);
        }
    }

    private void adjustAvailableRangeBorders(final Range availableRange) {
        final double minLog10 = Math.log10(availableRange.min);
        final double maxLog10 = Math.log10(availableRange.max);

        if (minLog10 < 0) {
            availableRange.min = Math.pow(10, ((int) (minLog10 * 100 - 1)) / 100.0);
        } else {
            availableRange.min = Math.pow(10, ((int) (minLog10 * 100)) / 100.0);
        }

        if (maxLog10 < 0) {
            availableRange.max = Math.pow(10, ((int) (maxLog10 * 100)) / 100.0);
        } else {
            availableRange.max = Math.pow(10, ((int) (maxLog10 * 100 + 1)) / 100.0);
        }
    }

    private void checkSelectedRange(final Range availableRange, final Range selectedRange) {
        if (selectedRange.min > availableRange.max || selectedRange.max < availableRange.min) {
            selectedRange.min = availableRange.min;
            selectedRange.max = availableRange.max;

            return;
        }

        if (selectedRange.min < availableRange.min) {
            selectedRange.min = availableRange.min;
        }

        if (selectedRange.max > availableRange.max) {
            selectedRange.max = availableRange.max;
        }
    }

    private void fireRedrawRequestMovieFrameChanged(final Date time) {
        for (EVEDrawControllerListener listener : listeners) {
            listener.drawRequest(time);
        }
    }

    private final DownloadedData retrieveData(final Band band, final Interval<Date> interval) {
        return band.getBandType().getDataDownloader().downloadData(band, interval);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Zoom Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    @Override
    public void availableIntervalChanged(final Interval<Date> newInterval) {
    }

    @Override
    public void selectedIntervalChanged(final Interval<Date> newInterval, boolean keepFullValueRange) {
        interval = newInterval;

        updateBands(keepFullValueRange);
        fireRedrawRequest(false);
    }

    @Override
    public void selectedResolutionChanged(final API_RESOLUTION_AVERAGES newResolution) {
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void bandAdded(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            addToMap(band);
        }
    }

    @Override
    public void bandRemoved(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            removeFromMap(band);
        }
    }

    @Override
    public void bandUpdated(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            if (band.isVisible()) {
                addToMap(band);
            } else {
                removeFromMap(band);
            }
        }
    }

    @Override
    public void bandGroupChanged(final String identifier) {
        if (this.identifier.equals(identifier)) {
            dataMapPerUnitLabel.clear();

            final Band[] activeBands = BandController.getSingletonInstance().getBands(identifier);

            for (final Band band : activeBands) {
                if (!dataMapPerUnitLabel.containsKey(band.getUnitLabel())) {
                    dataMapPerUnitLabel.put(band.getUnitLabel(), new HashMap<Band, DownloadedData>());
                }
                dataMapPerUnitLabel.get(band.getUnitLabel()).put(band, retrieveData(band, interval));
            }

            fireRedrawRequest(true);
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // EVE Cache Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void dataAdded(final Band band) {
        if (dataMapPerUnitLabel.containsKey(band.getUnitLabel())) {
            if (dataMapPerUnitLabel.get(band.getUnitLabel()).containsKey(band)) {
                updateBand(band, false);
                fireRedrawRequest(true);
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Layers Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(int idx) {
    }

    @Override
    public void viewportGeometryChanged() {
    }

    @Override
    public void timestampChanged(final int idx) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                // long start = System.currentTimeMillis();
                final ImmutableDateTime timestamp = LayersModel.getSingletonInstance().getCurrentFrameTimestamp(idx);
                if (timestamp != null) {
                    fireRedrawRequestMovieFrameChanged(timestamp.getTime());
                }
                // Log.debug("timestamp changed time: " +
                // (System.currentTimeMillis() - start));
            }
        });

    }

    @Override
    public void subImageDataChanged() {
    }

    @Override
    public void layerDownloaded(int idx) {
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
            double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime,
            boolean forced) {
        for (String unitLabel : yAxisElementMap.keySet()) {
            double diffScaledAvailable = scaledMaxValue - scaledMinValue;
            double diffAvaliable = Math.log10(availableRangeMap.get(unitLabel).max) - Math.log10(availableRangeMap.get(unitLabel).min);
            double diffSelectedStart = scaledSelectedMinValue - scaledMinValue;
            double diffSelectedEnd = scaledSelectedMaxValue - scaledMinValue;
            double selectedStart = Math.pow(10, Math.log10(availableRangeMap.get(unitLabel).min) + diffSelectedStart / diffScaledAvailable
                    * diffAvaliable);
            double selectedEnd = Math.pow(10, Math.log10(availableRangeMap.get(unitLabel).min) + diffSelectedEnd / diffScaledAvailable
                    * diffAvaliable);
            if (selectedStart != selectedRangeMap.get(unitLabel).min || selectedEnd != selectedRangeMap.get(unitLabel).max) {
                setSelectedRange(new Range(selectedStart, selectedEnd), unitLabel);
            } else {
                fireRedrawRequest(false);
            }
        }
    }
}
