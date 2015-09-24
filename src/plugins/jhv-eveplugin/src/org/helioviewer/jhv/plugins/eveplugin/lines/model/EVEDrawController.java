package org.helioviewer.jhv.plugins.eveplugin.lines.model;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Timer;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.ValueSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVECacheController;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVECacheControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.jhv.plugins.eveplugin.lines.gui.EVEDrawableElement;

/**
 * @author Stephan Pagel
 * */
public class EVEDrawController implements BandControllerListener, TimingListener, EVECacheControllerListener, PlotAreaSpaceListener, ValueSpaceListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final Map<YAxisElement, Map<Band, EVEValues>> dataMapPerUnitLabel = new HashMap<YAxisElement, Map<Band, EVEValues>>();
    private final DrawController drawController;

    private final Map<YAxisElement, EVEDrawableElement> eveDrawableElementMap;
    private final Map<Band, YAxisElement> yAxisElementMap;
    private final Map<YAxisElement, List<Band>> bandsPerYAxis;
    private final PlotAreaSpace plotAreaSpace;
    private static EVEDrawController instance;
    private final Timer addDataTimer;
    private final Timer selectedIntervalChangedTimer;
    private boolean dataAdded;
    private final Set<Band> addedDataForBand;
    private boolean selectedIntervalChanged;
    private boolean keepFullValueRange;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    private EVEDrawController() {

        BandController.getSingletonInstance().addBandControllerListener(this);
        DrawController.getSingletonInstance().addTimingListener(this);
        EVECacheController.getSingletonInstance().addControllerListener(this);

        drawController = DrawController.getSingletonInstance();
        eveDrawableElementMap = new HashMap<YAxisElement, EVEDrawableElement>();
        yAxisElementMap = new HashMap<Band, YAxisElement>();
        bandsPerYAxis = new HashMap<YAxisElement, List<Band>>();
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        plotAreaSpace.addPlotAreaSpaceListener(this);
        dataAdded = false;
        selectedIntervalChanged = false;
        addedDataForBand = new HashSet<Band>();
        addDataTimer = new Timer(200, new DataAddedTimerTask());
        addDataTimer.start();
        selectedIntervalChangedTimer = new Timer(300, new SelectedIntervalTimerTask());
        selectedIntervalChangedTimer.start();
    }

    public static EVEDrawController getSingletonInstance() {
        if (instance == null) {
            instance = new EVEDrawController();
        }
        return instance;
    }

    private void addToMap(final Band band) {
        Interval<Date> interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        YAxisElement yAxisElement = drawController.getYAxisElementForUnit(band.getUnitLabel());
        if (yAxisElement == null && drawController.hasAxisAvailable()) {
            yAxisElement = new YAxisElement();
            yAxisElement.addValueSpaceListener(this);
        }
        if (yAxisElement != null) {
            plotAreaSpace.addValueSpace(yAxisElement);
            yAxisElementMap.put(band, yAxisElement);
            addToBandsPerYAxis(yAxisElement, band);
            EVEValues data = retrieveData(band, interval, plotArea);
            if (!dataMapPerUnitLabel.containsKey(yAxisElement)) {
                dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
            }
            if (data != null) {
                dataMapPerUnitLabel.get(yAxisElement).put(band, data);
            }
        } else {
            Log.debug("band could not be added. No Yaxis Available ");
        }
        fireRedrawRequest(true);
    }

    private void addToBandsPerYAxis(YAxisElement yAxisElement, Band band) {
        List<Band> bands = new ArrayList<Band>();
        if (bandsPerYAxis.containsKey(yAxisElement)) {
            bands = bandsPerYAxis.get(yAxisElement);
        }
        bands.add(band);
        bandsPerYAxis.put(yAxisElement, bands);
    }

    private void removeFromMap(final Band band) {
        YAxisElement yAxisElement = yAxisElementMap.get(band);
        if (dataMapPerUnitLabel.containsKey(yAxisElement)) {
            if (dataMapPerUnitLabel.get(yAxisElement).containsKey(band)) {
                dataMapPerUnitLabel.get(yAxisElement).remove(band);
                List<Band> bands = bandsPerYAxis.get(yAxisElement);
                bands.remove(band);
                if (bands.isEmpty()) {
                    EVEDrawableElement removed = eveDrawableElementMap.remove(yAxisElement);
                    yAxisElementMap.remove(band);
                    bandsPerYAxis.remove(yAxisElement);
                    plotAreaSpace.removeValueSpace(yAxisElement);
                    drawController.removeDrawableElement(removed);
                }
                resetAvailableRange();
                fireRedrawRequest(true);
            }
        }
    }

    private void updateBand(final Band band, boolean keepFullValueRange) {
        Interval<Date> interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        EVEValues data = retrieveData(band, interval, plotArea);
        YAxisElement yAxisElement = yAxisElementMap.get(band);
        if (!dataMapPerUnitLabel.containsKey(yAxisElement)) {
            dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
        }
        Range newAvailableRange = new Range(yAxisElement.getAvailableRange());
        for (EVEValues v : dataMapPerUnitLabel.get(yAxisElement).values()) {
            if (v != null) {
                newAvailableRange.setMin(v.getMinimumValue());
                newAvailableRange.setMax(v.getMaximumValue());
            }
        }
        newAvailableRange.setMin(data.getMinimumValue());
        newAvailableRange.setMax(data.getMaximumValue());
        double avMin = newAvailableRange.min;
        double avMax = newAvailableRange.max;
        if (avMin == avMax) {
            if (avMin == 0) {
                yAxisElement.getAvailableRange().setMin(-1.0);
                yAxisElement.getAvailableRange().setMax(1.0);
            } else {
                yAxisElement.getAvailableRange().setMin(avMin - avMin / 10);
                yAxisElement.getAvailableRange().setMax(avMax + avMax / 10);
            }
        }
        yAxisElement.setAvailableRange(newAvailableRange);
        dataMapPerUnitLabel.get(yAxisElement).put(band, data);
    }

    private void updateBands(boolean keepFullValueRange) {
        for (YAxisElement yAxisElement : dataMapPerUnitLabel.keySet()) {
            for (final Band band : dataMapPerUnitLabel.get(yAxisElement).keySet()) {
                updateBand(band, keepFullValueRange);
            }
        }
    }

    public void setSelectedRangeMaximal() {
        fireRedrawRequest(true);
    }

    private void fireRedrawRequest(final boolean maxRange) {
        Interval<Date> interval = drawController.getSelectedInterval();
        for (YAxisElement yAxisElement : dataMapPerUnitLabel.keySet()) {
            final Band[] bands = dataMapPerUnitLabel.get(yAxisElement).keySet().toArray(new Band[0]);
            final LinkedList<EVEValues> values = new LinkedList<EVEValues>();

            String unitLabel = "";
            boolean isLog = false;
            if (bands.length > 0) {
                unitLabel = bands[0].getUnitLabel();
                isLog = bands[0].getBandType().isLogScale();
            }

            if (!eveDrawableElementMap.containsKey(yAxisElement)) {
                eveDrawableElementMap.put(yAxisElement, new EVEDrawableElement());
            }

            Range newAvailableRange = new Range(yAxisElement.getAvailableRange());

            for (EVEValues v : dataMapPerUnitLabel.get(yAxisElement).values()) {
                if (v != null) {
                    newAvailableRange.setMin(v.getMinimumValue());
                    newAvailableRange.setMax(v.getMaximumValue());
                    values.add(v);
                }
            }
            yAxisElement.setAvailableRange(new Range(newAvailableRange));
            if (maxRange) {
                yAxisElement.setSelectedRange(new Range(newAvailableRange));
            }
            yAxisElement.set(unitLabel, isLog);
            eveDrawableElementMap.get(yAxisElement).set(interval, bands, yAxisElement);
            if (bands.length > 0) {
                drawController.updateDrawableElement(eveDrawableElementMap.get(yAxisElement));
            } else {
                drawController.removeDrawableElement(eveDrawableElementMap.get(yAxisElement));
            }

        }
    }

    private final EVEValues retrieveData(final Band band, final Interval<Date> interval, Rectangle plotArea) {
        return EVECacheController.getSingletonInstance().downloadData(band, interval, plotArea);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Zoom Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        this.keepFullValueRange = keepFullValueRange;
        selectedIntervalChanged = true;

    }

    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void bandAdded(final Band band) {
        addToMap(band);
    }

    @Override
    public void bandRemoved(final Band band) {
        removeFromMap(band);
    }

    @Override
    public void bandUpdated(final Band band) {
        if (band.isVisible()) {
            addToMap(band);
        } else {
            removeFromMap(band);
        }
    }

    @Override
    public void bandGroupChanged() {
        Interval<Date> interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        dataMapPerUnitLabel.clear();

        final Band[] activeBands = BandController.getSingletonInstance().getBands();

        for (final Band band : activeBands) {
            YAxisElement yAxisElement = yAxisElementMap.get(band);
            if (!dataMapPerUnitLabel.containsKey(band.getUnitLabel())) {
                dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
            }
            dataMapPerUnitLabel.get(yAxisElement).put(band, retrieveData(band, interval, plotArea));
        }

        fireRedrawRequest(true);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // EVE Cache Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void dataAdded(final Band band) {
        addedDataForBand.add(band);
        dataAdded = true;
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinTime, double scaledMaxTime, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        fireRedrawRequest(false);
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinTime, double oldMaxTime, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub
    }

    public EVEValues getValues(Band band, Interval<Date> interval, Rectangle graphArea) {
        return dataMapPerUnitLabel.get(yAxisElementMap.get(band)).get(band);
    }

    public void bandColorChanged(Band band) {
        fireRedrawRequest(false);
    }

    public boolean hasDataInSelectedInterval(Band band) {
        return EVECacheController.getSingletonInstance().hasDataInSelectedInterval(band, DrawController.getSingletonInstance().getSelectedInterval());
    }

    public void changeAxis(Band band) {
        YAxisElement currentYAxisElement = yAxisElementMap.get(band);
        if (((bandsPerYAxis.size() == 1 && bandsPerYAxis.get(currentYAxisElement).size() > 1) || bandsPerYAxis.size() == 2) && drawController.canChangeAxis(band.getUnitLabel())) {
            YAxisElement otherYAxisElement = getOtherAxisElement(currentYAxisElement);
            if (otherYAxisElement != null) {
                plotAreaSpace.addValueSpace(otherYAxisElement);
                yAxisElementMap.put(band, otherYAxisElement);
                List<Band> bandsPerList = new ArrayList<Band>();
                if (bandsPerYAxis.containsKey(otherYAxisElement)) {
                    bandsPerList = bandsPerYAxis.get(otherYAxisElement);
                }
                bandsPerList.add(band);
                bandsPerYAxis.put(otherYAxisElement, bandsPerList);
                bandsPerYAxis.get(currentYAxisElement).remove(band);
                Map<Band, EVEValues> valuesPerBand = new HashMap<Band, EVEValues>();
                if (!dataMapPerUnitLabel.containsKey(otherYAxisElement)) {
                    dataMapPerUnitLabel.put(otherYAxisElement, valuesPerBand);
                }
                dataMapPerUnitLabel.get(otherYAxisElement).put(band, dataMapPerUnitLabel.get(currentYAxisElement).get(band));
                dataMapPerUnitLabel.get(currentYAxisElement).remove(band);
                if (!eveDrawableElementMap.containsKey(otherYAxisElement)) {
                    eveDrawableElementMap.put(otherYAxisElement, new EVEDrawableElement());
                }
                resetAvailableRange();
                updateBand(band, true);
                fireRedrawRequest(true);
            }
        }
    }

    private void resetAvailableRange() {
        for (YAxisElement yAxisElement : dataMapPerUnitLabel.keySet()) {
            yAxisElement.reset();
        }
    }

    private YAxisElement getOtherAxisElement(YAxisElement currentYAxisElement) {
        if (drawController.canChangeAxis(currentYAxisElement.getOriginalLabel())) {
            Set<YAxisElement> allYAxisElements = bandsPerYAxis.keySet();
            if (allYAxisElements.size() == 2) {
                for (YAxisElement el : allYAxisElements) {
                    if (!el.equals(currentYAxisElement)) {
                        return el;
                    }
                }
            }
            YAxisElement other = new YAxisElement();
            other.addValueSpaceListener(this);
            return other;
        }
        return null;
    }

    public boolean canChangeAxis(Band band) {
        return DrawController.getSingletonInstance().canChangeAxis(band.getUnitLabel());
    }

    public int getAxisLocation(Band band) {
        return drawController.getYAxisLocation(yAxisElementMap.get(band)) == YAxisElement.YAxisLocation.LEFT ? 0 : 1;
    }

    @Override
    public void valueSpaceChanged(Range availableRange, Range selectedRange) {
        fireRedrawRequest(false);
    }

    private class DataAddedTimerTask implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (dataAdded) {
                dataAdded = false;
                boolean update = false;
                for (Band b : addedDataForBand) {
                    if (yAxisElementMap.containsKey(b)) {
                        if (dataMapPerUnitLabel.get(yAxisElementMap.get(b)).containsKey(b)) {
                            updateBand(b, keepFullValueRange);
                            update = true;
                        }
                    }
                }
                if (update) {
                    fireRedrawRequest(keepFullValueRange);
                }
                addedDataForBand.clear();
            }
        }
    }

    private class SelectedIntervalTimerTask implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (selectedIntervalChanged) {
                selectedIntervalChanged = false;

                updateBands(keepFullValueRange);
                fireRedrawRequest(keepFullValueRange);

            }
        }
    }

}
