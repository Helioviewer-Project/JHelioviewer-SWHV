package org.helioviewer.jhv.plugins.eveplugin.lines.model;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
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
public class EVEDrawController implements BandControllerListener, TimingListener, EVECacheControllerListener, PlotAreaSpaceListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final Map<YAxisElement, Map<Band, EVEValues>> dataMapPerUnitLabel = new HashMap<YAxisElement, Map<Band, EVEValues>>();
    private final Map<YAxisElement, Range> selectedRangeMap = new HashMap<YAxisElement, Range>();
    private final Map<YAxisElement, Range> availableRangeMap = new HashMap<YAxisElement, Range>();
    private final Map<YAxisElement, Range> scaledSelectedRangeMap = new HashMap<YAxisElement, Range>();
    private final Map<YAxisElement, Range> scaledAvailableRangeMap = new HashMap<YAxisElement, Range>();
    private final DrawController drawController;

    private final Map<YAxisElement, EVEDrawableElement> eveDrawableElementMap;
    private final Map<Band, YAxisElement> yAxisElementMap;
    private final Map<YAxisElement, List<Band>> bandsPerYAxis;
    private final PlotAreaSpace plotAreaSpace;
    private static EVEDrawController instance;

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
        }
        if (yAxisElement != null) {
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
                    availableRangeMap.remove(yAxisElement);
                    selectedRangeMap.remove(yAxisElement);
                    scaledAvailableRangeMap.remove(yAxisElement);
                    scaledSelectedRangeMap.remove(yAxisElement);
                    yAxisElementMap.remove(band);
                    bandsPerYAxis.remove(yAxisElement);
                    drawController.removeDrawableElement(removed);
                }
                resetAvailableRange();
                fireRedrawRequest(true);
            }
        }
    }

    private void updateBand(final Band band, boolean keepFullValueRange) {
        Log.debug("<<<<< Update band start >>>>>  ");
        Interval<Date> interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        EVEValues data = retrieveData(band, interval, plotArea);
        boolean isLog = band.getBandType().isLogScale();
        YAxisElement yAxisElement = yAxisElementMap.get(band);
        if (!availableRangeMap.containsKey(yAxisElement)) {
            availableRangeMap.put(yAxisElement, new Range());
            selectedRangeMap.put(yAxisElement, new Range());
            scaledSelectedRangeMap.put(yAxisElement, new Range(0.0, 1.0));
            scaledAvailableRangeMap.put(yAxisElement, new Range(0.0, 1.0));
            dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
        }
        Range oldAvailableRange = new Range(availableRangeMap.get(yAxisElement));
        for (EVEValues v : dataMapPerUnitLabel.get(yAxisElement).values()) {
            if (v != null) {
                availableRangeMap.get(yAxisElement).setMin(v.getMinimumValue());
                availableRangeMap.get(yAxisElement).setMax(v.getMaximumValue());
            }
        }
        availableRangeMap.get(yAxisElement).setMin(data.getMinimumValue());
        availableRangeMap.get(yAxisElement).setMax(data.getMaximumValue());
        double avMin = availableRangeMap.get(yAxisElement).min;
        double avMax = availableRangeMap.get(yAxisElement).max;
        if (avMin == avMax) {
            if (avMin == 0) {
                availableRangeMap.get(yAxisElement).setMin(-1.0);
                availableRangeMap.get(yAxisElement).setMax(1.0);
            } else {
                availableRangeMap.get(yAxisElement).setMin(avMin - avMin / 10);
                availableRangeMap.get(yAxisElement).setMax(avMax + avMax / 10);
            }
        }
        if (oldAvailableRange.min != availableRangeMap.get(yAxisElement).min || oldAvailableRange.max != availableRangeMap.get(yAxisElement).max) {
            // Log.trace("update band available range changed so we change the plotareaSpace");
            checkSelectedRange(availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement), scaledAvailableRangeMap.get(yAxisElement), scaledSelectedRangeMap.get(yAxisElement));
            updateScaledValues(yAxisElement, availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement), keepFullValueRange, isLog);
        } else {
            // Log.trace("Same available range");
        }
        dataMapPerUnitLabel.get(yAxisElement).put(band, data);
        Log.debug("<<<<< Update band end >>>>>  ");
        Log.debug("");
    }

    private void updateBands(boolean keepFullValueRange) {
        for (YAxisElement yAxisElement : dataMapPerUnitLabel.keySet()) {
            for (final Band band : dataMapPerUnitLabel.get(yAxisElement).keySet()) {
                updateBand(band, keepFullValueRange);
            }
        }
    }

    public void setSelectedRange(final Range newSelectedRange, YAxisElement yAxisElement) {
        Log.debug("set selected range");
        Log.debug("old selected range : [" + Math.log10(selectedRangeMap.get(yAxisElement).min) + " , " + Math.log10(selectedRangeMap.get(yAxisElement).max) + "]");
        Log.debug("new selected range : [" + Math.log10(newSelectedRange.min) + " , " + Math.log10(newSelectedRange.max) + "]");
        // Thread.dumpStack();
        selectedRangeMap.put(yAxisElement, new Range(newSelectedRange));
        drawController.setSelectedRange(newSelectedRange);
        fireRedrawRequest(false);
    }

    public void setSelectedRangeMaximal() {
        fireRedrawRequest(true);
    }

    private void fireRedrawRequest(final boolean maxRange) {
        Log.debug("<<<<< fire redraw request start >>>>>");
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

            if (!availableRangeMap.containsKey(yAxisElement)) {
                availableRangeMap.put(yAxisElement, new Range());
                selectedRangeMap.put(yAxisElement, new Range());
                scaledAvailableRangeMap.put(yAxisElement, new Range(0.0, 1.0));
                scaledSelectedRangeMap.put(yAxisElement, new Range(0.0, 1.0));
                eveDrawableElementMap.put(yAxisElement, new EVEDrawableElement());
            }

            Range oldAvailableRange = new Range(availableRangeMap.get(yAxisElement));

            for (EVEValues v : dataMapPerUnitLabel.get(yAxisElement).values()) {
                if (v != null) {
                    availableRangeMap.get(yAxisElement).setMin(v.getMinimumValue());
                    availableRangeMap.get(yAxisElement).setMax(v.getMaximumValue());
                    values.add(v);
                }
            }

            if (maxRange) {
                selectedRangeMap.put(yAxisElement, new Range());
                Range availableRange = scaledAvailableRangeMap.get(yAxisElement);
                Log.debug("Set scaled selected range to " + availableRange);
                // Thread.dumpStack();
                scaledSelectedRangeMap.put(yAxisElement, new Range(availableRange.min, availableRange.max));
            }
            checkSelectedRange(availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement), scaledAvailableRangeMap.get(yAxisElement), scaledSelectedRangeMap.get(yAxisElement));
            if (oldAvailableRange.min != availableRangeMap.get(yAxisElement).min || oldAvailableRange.max != availableRangeMap.get(yAxisElement).max) {
                Log.error("Available range changed in redraw request. So update plotAreaSpace");
                Log.error("old range : " + oldAvailableRange.toString());
                Log.error("new available range : " + availableRangeMap.get(yAxisElement).toString());
                updateScaledValues(yAxisElement, availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement), false, isLog);

            }

            yAxisElement.set(selectedRangeMap.get(yAxisElement), availableRangeMap.get(yAxisElement), unitLabel, selectedRangeMap.get(yAxisElement).min, selectedRangeMap.get(yAxisElement).max, Color.PINK, isLog);
            eveDrawableElementMap.get(yAxisElement).set(interval, bands, yAxisElement);
            if (bands.length > 0) {
                drawController.updateDrawableElement(eveDrawableElementMap.get(yAxisElement));
            } else {
                drawController.removeDrawableElement(eveDrawableElementMap.get(yAxisElement));
            }

        }
        Log.debug("<<<<< fire redraw request end >>>>>");
        Log.debug("");
    }

    private void updateScaledValues(YAxisElement yAxisElement, Range availableRange, Range selectedRange, boolean keepFullValueSpace, boolean isLog) {
        Range scaledAvailable = scaledAvailableRangeMap.get(yAxisElement);
        Range scaledSelected = scaledSelectedRangeMap.get(yAxisElement);
        Range oldScaledAvailable = new Range(scaledAvailable);
        Range oldScaledSelected = new Range(scaledSelected);
        if (!keepFullValueSpace) {
            double diffSelected = 0;
            if (isLog) {
                diffSelected = Math.log10(selectedRange.max) - Math.log10(selectedRange.min);
            } else {
                diffSelected = selectedRange.max - selectedRange.min;
            }
            double diffScaledSelected = scaledSelected.max - scaledSelected.min;
            double ratio = diffScaledSelected / diffSelected;
            double diffSelMaxAvaiMax = 0;
            if (isLog) {
                diffSelMaxAvaiMax = Math.log10(availableRange.max) - Math.log10(selectedRange.max);
            } else {
                diffSelMaxAvaiMax = availableRange.max - selectedRange.max;
            }
            double diffSelMinAvaiMin = 0;
            if (isLog) {
                diffSelMinAvaiMin = Math.log10(selectedRange.min) - Math.log10(availableRange.min);
            } else {
                diffSelMinAvaiMin = selectedRange.min - availableRange.min;
            }
            scaledAvailable.min = scaledSelected.min - diffSelMinAvaiMin * ratio;
            scaledAvailable.max = scaledSelected.max + diffSelMaxAvaiMax * ratio;

            Log.debug("//////////////////////////////////");
            Log.debug("old scaled available: " + oldScaledAvailable);
            Log.debug("old scaled selected: " + oldScaledSelected);
            Log.debug("Available range:(log10) [" + Math.log10(availableRangeMap.get(yAxisElement).min) + " , " + Math.log10(availableRangeMap.get(yAxisElement).max) + "]; (normal) [" + availableRangeMap.get(yAxisElement).min + ", " + availableRangeMap.get(yAxisElement).max + "]");
            Log.debug("Selected range: (log10) [" + Math.log10(selectedRangeMap.get(yAxisElement).min) + " , " + Math.log10(selectedRangeMap.get(yAxisElement).max) + "]; (normal) [" + selectedRangeMap.get(yAxisElement).min + ", " + selectedRangeMap.get(yAxisElement).max + "]");
            Log.debug("New Available range:(log10) [" + Math.log10(availableRange.min) + " , " + Math.log10(availableRange.max) + "]; (normal) [" + availableRange.min + ", " + availableRange.max + "]");
            Log.debug("New Selected range: (log10) [" + Math.log10(selectedRange.min) + " , " + Math.log10(selectedRange.max) + "]; (normal) [" + selectedRange.min + ", " + selectedRange.max + "]");
            Log.debug("diffSelected = Math.log10(selectedRange.max) - Math.log10(selectedRange.min)   =>   " + Math.log10(selectedRange.max) + " - " + Math.log10(selectedRange.min) + " = " + diffSelected);
            Log.debug("diffScaledSelected = scaledSelected.max - scaledSelected.min   =>   " + scaledSelected.max + " - " + scaledSelected.min + " = " + diffScaledSelected);
            Log.debug("ratio = diffScaledSelected / diffSelected   =>   " + diffScaledSelected + " / " + diffSelected + " = " + ratio);
            Log.debug("-------- MIN ----------");
            Log.debug("diffSelMinAvaiMin = log10(selectedRange.min) - log10(availableRange.min)   =>   " + Math.log10(availableRange.min) + " - " + Math.log10(selectedRange.min) + " = " + diffSelMinAvaiMin);
            Log.debug("scaledAvailable.min = scaledSelected.min - diffSelMinAvaiMin * ratio   =>   " + scaledSelected.min + " - " + diffSelMinAvaiMin + " * " + ratio + " = " + scaledAvailable.min);
            Log.debug("-------- MAX ----------");
            Log.debug("diffSelMaxAvaiMax = availableRange.max - selectedRange.max   =>   " + Math.log10(availableRange.max) + " - " + Math.log10(selectedRange.max) + " = " + diffSelMaxAvaiMax);
            Log.debug("scaledAvailable.max = scaledSelected.max + diffSelMaxAvaiMax * ratio   =>   " + scaledSelected.max + "+" + diffSelMaxAvaiMax + " * " + ratio + " = " + scaledAvailable.max);
            Log.debug("//////////////////////////////////");

        } else {
            // plotAreaSpace.setScaledSelectedValue(plotAreaSpace.getScaledMinValue(),
            // plotAreaSpace.getScaledMaxValue(), true);
            scaledSelected.min = scaledAvailable.min;
            scaledSelected.max = scaledAvailable.max;
        }
    }

    private void checkSelectedRange(final Range availableRange, final Range selectedRange, final Range scaledAvailable, final Range scaledSelected) {
        if (selectedRange.min > availableRange.max || selectedRange.max < availableRange.min) {
            Log.debug("Check selected range 1 before. available range: [" + Math.log10(availableRange.min) + ", " + Math.log10(availableRange.max) + "] , selected range: [" + Math.log10(selectedRange.min) + ", " + Math.log10(selectedRange.max) + "] scaled selected range: " + scaledSelected + " scaled available range: " + scaledAvailable);
            // Thread.dumpStack();
            selectedRange.min = availableRange.min;
            selectedRange.max = availableRange.max;
            scaledSelected.min = scaledAvailable.min;
            scaledSelected.max = scaledAvailable.max;
            Log.debug("Check selected range 1 after. available range: [" + Math.log10(availableRange.min) + ", " + Math.log10(availableRange.max) + "] , selected range: [" + Math.log10(selectedRange.min) + ", " + Math.log10(selectedRange.max) + "] scaled selected range: " + scaledSelected + " scaled available range: " + scaledAvailable);
            return;
        }

        if (selectedRange.min < availableRange.min) {
            Log.debug("Check selected range 2 before. available range: [" + Math.log10(availableRange.min) + ", " + Math.log10(availableRange.max) + "] , selected range: [" + Math.log10(selectedRange.min) + ", " + Math.log10(selectedRange.max) + "] scaled selected range: " + scaledSelected + " scaled available range: " + scaledAvailable);
            // Thread.dumpStack();
            selectedRange.min = availableRange.min;
            scaledSelected.min = scaledAvailable.min;
            Log.debug("Check selected range 2 after. available range: [" + Math.log10(availableRange.min) + ", " + Math.log10(availableRange.max) + "] , selected range: [" + Math.log10(selectedRange.min) + ", " + Math.log10(selectedRange.max) + "] scaled selected range: " + scaledSelected + " scaled available range: " + scaledAvailable);
        }

        if (selectedRange.max > availableRange.max) {
            Log.debug("Check selected range 3 before. available range: [" + Math.log10(availableRange.min) + ", " + Math.log10(availableRange.max) + "] , selected range: [" + Math.log10(selectedRange.min) + ", " + Math.log10(selectedRange.max) + "] scaled selected range: " + scaledSelected + " scaled available range: " + scaledAvailable);
            // Thread.dumpStack();
            selectedRange.max = availableRange.max;
            scaledSelected.max = scaledAvailable.max;
            Log.debug("Check selected range 3 after. available range: [" + Math.log10(availableRange.min) + ", " + Math.log10(availableRange.max) + "] , selected range: [" + Math.log10(selectedRange.min) + ", " + Math.log10(selectedRange.max) + "] scaled selected range: " + scaledSelected + " scaled available range: " + scaledAvailable);
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
    public void selectedIntervalChanged() {

        updateBands(drawController.keepfullValueRange());
        fireRedrawRequest(false);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void bandAdded(final Band band) {
        Log.debug("<<<<< Band added start >>>>>");
        addToMap(band);
        Log.debug("<<<<< Band added end >>>>>");
        Log.debug("");
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
        if (yAxisElementMap.containsKey(band)) {
            if (dataMapPerUnitLabel.get(yAxisElementMap.get(band)).containsKey(band)) {
                updateBand(band, false);
                fireRedrawRequest(true);
            }
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        Log.debug("<<<<< Plot area changed start >>>>>  ");
        for (YAxisElement yAxisElement : bandsPerYAxis.keySet()) {
            double diffPAAvai = scaledMaxValue - scaledMinValue;
            double diffLoScAvai = scaledAvailableRangeMap.get(yAxisElement).max - scaledAvailableRangeMap.get(yAxisElement).min;
            // Local scaled selected start
            double diffPASelSAvaiS = scaledSelectedMinValue - scaledMinValue;
            double scLoSelS = diffPASelSAvaiS / diffPAAvai * diffLoScAvai + scaledAvailableRangeMap.get(yAxisElement).min;
            // Local scaled selected end
            double diffPASelEAvaiS = scaledSelectedMaxValue - scaledMinValue;
            double scLoSelE = diffPASelEAvaiS / diffPAAvai * diffLoScAvai + scaledAvailableRangeMap.get(yAxisElement).min;

            double diffLoAvai = Math.log10(availableRangeMap.get(yAxisElement).max) - Math.log10(availableRangeMap.get(yAxisElement).min);
            // Local selected start
            double diffLoScSelSScAS = scLoSelS - scaledSelectedRangeMap.get(yAxisElement).min;
            double localSelectedStart = Math.pow(10, diffLoScSelSScAS / diffLoScAvai * diffLoAvai + Math.log10(availableRangeMap.get(yAxisElement).min));
            // Local selected end
            double diffLoScSelEScAs = scLoSelE - scaledSelectedRangeMap.get(yAxisElement).min;
            double localSelectedEnd = Math.pow(10, diffLoScSelEScAs / diffLoScAvai * diffLoAvai + Math.log10(availableRangeMap.get(yAxisElement).min));
            if (localSelectedStart != selectedRangeMap.get(yAxisElement).min || localSelectedEnd != selectedRangeMap.get(yAxisElement).max) {
                Log.debug("----------------------------------------");
                Log.debug("Available range:(log10) [" + Math.log10(availableRangeMap.get(yAxisElement).min) + " , " + Math.log10(availableRangeMap.get(yAxisElement).max) + "]; (normal) [" + availableRangeMap.get(yAxisElement).min + ", " + availableRangeMap.get(yAxisElement).max + "]");
                Log.debug("Selected range: (log10) [" + Math.log10(selectedRangeMap.get(yAxisElement).min) + " , " + Math.log10(selectedRangeMap.get(yAxisElement).max) + "]; (normal) [" + selectedRangeMap.get(yAxisElement).min + ", " + selectedRangeMap.get(yAxisElement).max + "]");
                Log.debug("diffPAAvai = scaledMaxValue - scaledMinValue   =>   " + scaledMaxValue + " - " + scaledMinValue + " = " + diffPAAvai);
                Log.debug("diffLoScAvai = scaledAvailableRangeMap.get(yAxisElement).max - scaledAvailableRangeMap.get(yAxisElement).min   =>   " + scaledAvailableRangeMap.get(yAxisElement).max + " - " + scaledAvailableRangeMap.get(yAxisElement).min + " = " + diffLoScAvai);
                Log.debug("--------- LOCAL SCALED SELECTED START ---------");
                Log.debug("diffPASelSAvaiS = scaledSelectedMinValue - scaledMinValue   =>   " + scaledSelectedMinValue + " - " + scaledMinValue + " = " + diffPASelSAvaiS);
                Log.debug("scLoSelS = diffPASelSAvaiS / diffPAAvai * diffLoScAvai + scaledAvailableRangeMap.get(yAxisElement).min   =>   " + diffPASelSAvaiS + " / " + diffPAAvai + " * " + diffLoScAvai + " + " + scaledAvailableRangeMap.get(yAxisElement).min + " = " + scLoSelS);
                Log.debug("--------- LOCAL SCALED SELECTED END ---------");
                Log.debug("diffPASelEAvaiS = scaledSelectedMaxValue - scaledMinValue   =>   " + scaledSelectedMaxValue + " - " + scaledMinValue + " = " + diffPASelEAvaiS);
                Log.debug("scLoSelE = diffPASelEAvaiS / diffPAAvai * diffLoScAvai + scaledAvailableRangeMap.get(yAxisElement).min   =>   " + diffPASelEAvaiS + " / " + diffPAAvai + " * " + diffLoScAvai + " + " + scaledAvailableRangeMap.get(yAxisElement).min + " = " + scLoSelE);
                Log.debug("diffLoAvai = log10(availableRangeMap.get(yAxisElement).max) - log10(availableRangeMap.get(yAxisElement).min)   =>   " + Math.log10(availableRangeMap.get(yAxisElement).max) + " - " + Math.log10(availableRangeMap.get(yAxisElement).min) + " = " + diffLoAvai);
                Log.debug("--------- LOCAL SELECTED START ---------");
                Log.debug("diffLoScSelSScAS = scLoSelS - scaledSelectedRangeMap.get(yAxisElement).min   =>   " + scLoSelS + " - " + scaledSelectedRangeMap.get(yAxisElement).min + " = " + diffLoScSelSScAS);
                Log.debug("localSelectedStart = (diffLoScSelSScAS / diffLoScAvai * diffLoAvai + Math.log10(availableRangeMap.get(yAxisElement).min))^10   =>   (" + diffLoScSelSScAS + " / " + diffLoScAvai + " * " + diffLoAvai + " + " + Math.log10(availableRangeMap.get(yAxisElement).min) + ")^10 = " + localSelectedStart);
                Log.debug("--------- LOCAL SELECTED END ---------");
                Log.debug("diffLoScSelEScAs = scLoSelE - scaledSelectedRangeMap.get(yAxisElement).min   =>   " + scLoSelE + " - " + scaledSelectedRangeMap.get(yAxisElement).min + " = " + diffLoScSelEScAs);
                Log.debug("localSelectedEnd = (diffLoScSelEScAs / diffLoScAvai * diffLoAvai + log10(availableRangeMap.get(yAxisElement).min))^10   =>   ( " + diffLoScSelEScAs + " / " + diffLoScAvai + " * " + diffLoAvai + " + " + Math.log10(availableRangeMap.get(yAxisElement).min) + ")^10 = " + localSelectedEnd);
                Log.debug("new selected range: [ " + Math.log10(localSelectedStart) + ", " + Math.log10(localSelectedEnd) + " ]");
                Log.debug("----------------------------------------");
                setSelectedRange(new Range(localSelectedStart, localSelectedEnd), yAxisElement);
            } else {
                fireRedrawRequest(false);
            }
            /*
             * double diffScaledAvailablePA = scaledMaxValue - scaledMinValue;
             * double diffAvailable =
             * Math.log10(availableRangeMap.get(yAxisElement).max) -
             * Math.log10(availableRangeMap.get(yAxisElement).min); double
             * diffScaledAvai = scaledAvailableRangeMap.get(yAxisElement).max -
             * scaledAvailableRangeMap.get(yAxisElement).min; double
             * diffSelectedStartPA = scaledSelectedMinValue - scaledMinValue;
             * double diffSelectedEndPA = scaledSelectedMaxValue -
             * scaledMinValue; double diffScaledSelectedStart =
             * diffSelectedStartPA * diffScaledAvai / diffScaledAvailablePA;
             * double diffScaledSelectedEnd = diffSelectedEndPA * diffScaledAvai
             * / diffScaledAvailablePA; double selectedStart = Math.pow(10,
             * Math.log10(availableRangeMap.get(yAxisElement).min) +
             * diffScaledSelectedStart * diffAvailable); double selectedEnd =
             * Math.pow(10, Math.log10(availableRangeMap.get(yAxisElement).min)
             * + diffScaledSelectedEnd * diffAvailable); if (selectedStart !=
             * selectedRangeMap.get(yAxisElement).min || selectedEnd !=
             * selectedRangeMap.get(yAxisElement).max) {
             * Log.debug("---------------------------------------");
             * Log.debug("Available range: [" +
             * Math.log10(availableRangeMap.get(yAxisElement).min) + " , " +
             * Math.log10(availableRangeMap.get(yAxisElement).max) + "]");
             * Log.debug("Selected range: [" +
             * Math.log10(selectedRangeMap.get(yAxisElement).min) + " , " +
             * Math.log10(selectedRangeMap.get(yAxisElement).max) + "]");
             * Log.debug
             * ("diffScaledAvailablePA = scaledMaxValue - scaledMinValue   =>   "
             * + scaledMaxValue + " - " + scaledMinValue + " = " +
             * diffScaledAvailablePA); Log.debug(
             * "diffScaledAvai = scaledAvailableRangeMap.max - scaledAvailableRangeMap.min   =>   "
             * + scaledAvailableRangeMap.get(yAxisElement).max + " - " +
             * scaledAvailableRangeMap.get(yAxisElement).min + " = " +
             * diffScaledAvai); Log.debug(
             * "diffAvailable = log10(availableRangeMap_max) - log10(availableRangeMap_min)   =>   "
             * + Math.log10(availableRangeMap.get(yAxisElement).max) + " - " +
             * Math.log10(availableRangeMap.get(yAxisElement).min) + " = " +
             * diffAvailable); Log.debug("---- START ----"); Log.debug(
             * "diffSelectedStartPA = scaledSelectedMinValue - scaledMinValue   =>   "
             * + scaledSelectedMinValue + " - " + scaledMinValue + " = " +
             * diffSelectedStartPA); Log.debug(
             * "diffScaledSelectedStart = diffSelectedStartPA * diffScaledAvai / diffScaledAvailablePA   =>   "
             * + diffSelectedStartPA + " * " + diffScaledAvai + " / " +
             * diffScaledAvailablePA + " = " + diffScaledSelectedStart);
             * Log.debug(
             * "selecteStart = (log10( available_min ) + diffScaledSelectedStart * diffAvailable ) ^ 10   =>    ("
             * + Math.log10(availableRangeMap.get(yAxisElement).min) + " + " +
             * diffScaledSelectedStart + " * " + diffAvailable + ") ^ 10 = " +
             * selectedStart); Log.debug("log10(selectedStart)   =>   " +
             * Math.log10(selectedStart)); Log.debug("---- END ----");
             * Log.debug(
             * "diffSelectedEndPA = scaledSelectedMaxValue - scaledMinValue   =>   "
             * + scaledSelectedMaxValue + " - " + scaledMinValue + " = " +
             * diffSelectedEndPA); Log.debug(
             * "diffScaledSelectedEnd = diffSelectedEndPA * diffScaledAvai / diffScaledAvailablePA   =>   "
             * + diffSelectedEndPA + " * " + diffScaledAvai + "/" +
             * diffScaledAvailablePA + " = " + diffScaledSelectedEnd);
             * Log.debug(
             * "selectedEnd = (log10(available_min) + diffScaledSelectedEnd * diffAvailable) ^ 10   =>   ("
             * + Math.log10(availableRangeMap.get(yAxisElement).min) + " + " +
             * diffScaledSelectedEnd + " * " + diffAvailable + ") ^ 10 = " +
             * selectedEnd); Log.debug("log10(selectedEnd)   =>   " +
             * Math.log10(selectedEnd));
             * Log.debug("---------------------------------------");
             * setSelectedRange(new Range(selectedStart, selectedEnd),
             * yAxisElement); } else { fireRedrawRequest(false); }
             */
        }
        Log.debug("<<<<< Plot area changed end >>>>>  ");
        Log.debug("");
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub
    }

    public EVEValues getValues(Band band, Interval<Date> interval, Rectangle graphArea) {
        return EVECacheController.getSingletonInstance().downloadData(band, interval, graphArea);
    }

    public void bandColorChanged(Band band) {
        fireRedrawRequest(false);
    }

    public boolean hasDataInSelectedInterval(Band band) {
        return EVECacheController.getSingletonInstance().hasDataInSelectedInterval(band, DrawController.getSingletonInstance().getSelectedInterval());
    }

    public void changeAxis(Band band) {
        Log.debug("<<<<< Change axis start >>>>>");
        YAxisElement currentYAxisElement = yAxisElementMap.get(band);
        if (((bandsPerYAxis.size() == 1 && bandsPerYAxis.get(currentYAxisElement).size() > 1) || bandsPerYAxis.size() == 2) && drawController.canChangeAxis(band.getUnitLabel())) {
            YAxisElement otherYAxisElement = getOtherAxisElement(currentYAxisElement);
            if (otherYAxisElement != null) {
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
        Log.debug("<<<<< Change axis end >>>>>");
        Log.debug("");
    }

    private void resetAvailableRange() {
        for (YAxisElement yAxisElement : availableRangeMap.keySet()) {
            availableRangeMap.put(yAxisElement, new Range());
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
            return new YAxisElement();
        }
        return null;
    }

    public boolean canChangeAxis(Band band) {
        return DrawController.getSingletonInstance().canChangeAxis(band.getUnitLabel());
    }

    public int getAxisLocation(Band band) {
        return drawController.getYAxisLocation(yAxisElementMap.get(band)) == YAxisElement.YAxisLocation.LEFT ? 0 : 1;
    }

}
