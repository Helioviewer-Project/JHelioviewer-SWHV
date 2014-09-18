package org.helioviewer.plugins.eveplugin.lines.model;

import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

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
    private final HashMap<Band, DownloadedData> dataMap = new HashMap<Band, DownloadedData>();

    private Interval<Date> interval = new Interval<Date>(null, null);
    private Range selectedRange = new Range();
    private final Range availableRange = new Range();
    private final DrawController drawController;

    private final EVEDrawableElement eveDrawableElement;
    private final YAxisElement yAxisElement;

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
        eveDrawableElement = new EVEDrawableElement();
        yAxisElement = new YAxisElement();

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
        if (data != null) {
            dataMap.put(band, data);
        }
        fireRedrawRequest(true);
    }

    private void removeFromMap(final Band band) {
        if (dataMap.containsKey(band)) {
            dataMap.remove(band);

            fireRedrawRequest(true);
        }
    }

    private void updateBand(final Band band) {
        DownloadedData data = retrieveData(band, interval);
        Range oldAvailableRange = new Range(availableRange);
        for (DownloadedData v : dataMap.values()) {
            if (v != null) {
                availableRange.setMin(v.getMinimumValue());
                availableRange.setMax(v.getMaximumValue());
            }
        }
        if (oldAvailableRange.min != availableRange.min || oldAvailableRange.max != availableRange.max) {
            Log.trace("update band available range changed so we change the plotareaSpace");
            checkSelectedRange(availableRange, selectedRange);
            updatePlotAreaSpace(availableRange, selectedRange);
        } else {
            Log.trace("Same available range");
        }
        dataMap.put(band, data);
    }

    private void updateBands() {
        for (final Band band : dataMap.keySet()) {
            updateBand(band);
        }
    }

    public void setSelectedRange(final Range newSelectedRange) {
        selectedRange = new Range(newSelectedRange);
        drawController.setSelectedRange(newSelectedRange);
        fireRedrawRequest(false);
    }

    public void setSelectedRangeMaximal() {
        fireRedrawRequest(true);
    }

    private void fireRedrawRequest(final boolean maxRange) {
        final Band[] bands = dataMap.keySet().toArray(new Band[0]);
        final LinkedList<DownloadedData> values = new LinkedList<DownloadedData>();
        Range oldAvailableRange = new Range(availableRange);

        for (DownloadedData v : dataMap.values()) {
            if (v != null) {

                availableRange.setMin(v.getMinimumValue());
                availableRange.setMax(v.getMaximumValue());
                values.add(v);
            }
        }

        if (maxRange) {
            selectedRange = new Range();
        }
        checkSelectedRange(availableRange, selectedRange);
        if (oldAvailableRange.min != availableRange.min || oldAvailableRange.max != availableRange.max) {
            Log.error("Available range changed in redraw request. So update plotAreaSpace");
            Log.error("old range : " + oldAvailableRange.toString());
            Log.error("new available range : " + availableRange.toString());
            updatePlotAreaSpace(availableRange, selectedRange);

        }

        for (EVEDrawControllerListener listener : listeners) {
            listener.drawRequest(interval, bands, values.toArray(new EVEValues[0]), availableRange, selectedRange);
        }
        String unitLabel = "";
        if (bands.length > 0) {
            unitLabel = bands[0].getUnitLabel();
        }
        yAxisElement
                .set(selectedRange, availableRange, unitLabel, Math.log10(selectedRange.min), Math.log10(selectedRange.max), Color.PINK);
        eveDrawableElement.set(interval, bands, values.toArray(new EVEValues[0]), yAxisElement);
        if (bands.length > 0) {
            drawController.updateDrawableElement(eveDrawableElement, identifier);
        } else {
            drawController.removeDrawableElement(eveDrawableElement, identifier);
        }
    }

    private void updatePlotAreaSpace(Range availableRange, Range selectedRange) {
        double diffAvailable = Math.log10(availableRange.max) - Math.log10(availableRange.min);
        double diffStart = Math.log10(selectedRange.min) - Math.log10(availableRange.min);
        double diffEnd = Math.log10(selectedRange.max) - Math.log10(availableRange.min);
        double startValue = plotAreaSpace.getScaledMinValue() + diffStart / diffAvailable;
        double endValue = plotAreaSpace.getScaledMinValue() + diffEnd / diffAvailable;
        plotAreaSpace.setScaledSelectedValue(startValue, endValue, false);
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
    public void selectedIntervalChanged(final Interval<Date> newInterval) {
        interval = newInterval;

        updateBands();
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
            dataMap.clear();

            final Band[] activeBands = BandController.getSingletonInstance().getBands(identifier);

            for (final Band band : activeBands) {
                dataMap.put(band, retrieveData(band, interval));
            }

            fireRedrawRequest(true);
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // EVE Cache Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void dataAdded(final Band band) {
        if (dataMap.containsKey(band)) {
            updateBand(band);
            fireRedrawRequest(true);
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
    public void timestampChanged(int idx) {
        final ImmutableDateTime timestamp = LayersModel.getSingletonInstance().getCurrentFrameTimestamp(idx);
        fireRedrawRequestMovieFrameChanged(timestamp.getTime());
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
        double diffScaledAvailable = scaledMaxValue - scaledMinValue;
        double diffAvaliable = Math.log10(availableRange.max) - Math.log10(availableRange.min);
        double diffSelectedStart = scaledSelectedMinValue - scaledMinValue;
        double diffSelectedEnd = scaledSelectedMaxValue - scaledMinValue;
        double selectedStart = Math.pow(10, Math.log10(availableRange.min) + diffSelectedStart / diffScaledAvailable * diffAvaliable);
        double selectedEnd = Math.pow(10, Math.log10(availableRange.min) + diffSelectedEnd / diffScaledAvailable * diffAvaliable);
        if (selectedStart != selectedRange.min || selectedEnd != selectedRange.max) {
            setSelectedRange(new Range(selectedStart, selectedEnd));
        } else {
            fireRedrawRequest(false);
        }
    }
}
