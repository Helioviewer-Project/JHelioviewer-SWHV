package org.helioviewer.jhv.plugins.eveplugin.lines.model;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Timer;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.ValueSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis.YAxisLocation;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandColors;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVECacheController;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVECacheControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.jhv.plugins.eveplugin.lines.gui.EVEDrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

public class EVEDrawController implements TimingListener, EVECacheControllerListener, ValueSpaceListener {

    private final Map<YAxis, Map<Band, EVEValues>> dataMapPerUnitLabel = new HashMap<YAxis, Map<Band, EVEValues>>();
    private final DrawController drawController;
    private final Set<BandType> bandTypes;
    private final Map<YAxis, EVEDrawableElement> eveDrawableElementMap;
    private final Map<Band, YAxis> yAxisElementMap;
    private final Map<YAxis, List<Band>> bandsPerYAxis;

    private static EVEDrawController instance;
    private final Timer selectedIntervalChangedTimer;
    private boolean selectedIntervalChanged;
    private boolean keepFullValueRange;
    private final LineDataSelectorModel selectorModel;

    private EVEDrawController() {
        drawController = DrawController.getSingletonInstance();
        drawController.addTimingListener(this);

        EVECacheController.getSingletonInstance().addControllerListener(this);
        selectorModel = LineDataSelectorModel.getSingletonInstance();
        eveDrawableElementMap = new HashMap<YAxis, EVEDrawableElement>();
        bandTypes = new HashSet<BandType>();
        yAxisElementMap = new HashMap<Band, YAxis>();
        bandsPerYAxis = new HashMap<YAxis, List<Band>>();
        selectedIntervalChanged = false;
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
        Interval interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        YAxis yAxisElement = drawController.getYAxisElementForUnit(band.getUnitLabel());
        if (yAxisElement == null && drawController.hasAxisAvailable()) {
            yAxisElement = new YAxis();
            yAxisElement.addValueSpaceListener(this);
        }
        if (yAxisElement != null) {
            // drawController.addValueSpace(yAxisElement);
            yAxisElementMap.put(band, yAxisElement);
            addToBandsPerYAxis(yAxisElement, band);
            EVEValues data = retrieveData(band, interval, plotArea);

            if (!dataMapPerUnitLabel.containsKey(yAxisElement)) {
                dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
            }
            dataMapPerUnitLabel.get(yAxisElement).put(band, data);
        } else {
            Log.debug("band could not be added. No Yaxis Available ");
        }
        fireRedrawRequest(true);
    }

    private void addToBandsPerYAxis(YAxis yAxisElement, Band band) {
        List<Band> bands = new ArrayList<Band>();
        if (bandsPerYAxis.containsKey(yAxisElement)) {
            bands = bandsPerYAxis.get(yAxisElement);
        }
        bands.add(band);
        bandsPerYAxis.put(yAxisElement, bands);
    }

    private void removeFromMap(final Band band) {
        YAxis yAxisElement = yAxisElementMap.get(band);
        if (dataMapPerUnitLabel.containsKey(yAxisElement)) {
            if (dataMapPerUnitLabel.get(yAxisElement).containsKey(band)) {
                dataMapPerUnitLabel.get(yAxisElement).remove(band);
                List<Band> bands = bandsPerYAxis.get(yAxisElement);
                bands.remove(band);
                if (bands.isEmpty()) {
                    EVEDrawableElement removed = eveDrawableElementMap.remove(yAxisElement);
                    yAxisElementMap.remove(band);
                    bandsPerYAxis.remove(yAxisElement);
                    drawController.removeDrawableElement(removed);
                }
                resetAvailableRange();
                fireRedrawRequest(true);
            }
        }
    }

    private void updateBand(final Band band) {
        Interval interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        EVEValues data = retrieveData(band, interval, plotArea);
        YAxis yAxisElement = yAxisElementMap.get(band);
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

    private void updateBands() {
        for (Map<Band, EVEValues> value : dataMapPerUnitLabel.values()) {
            for (final Band band : value.keySet()) {
                updateBand(band);
            }
        }
    }

    public void setSelectedRangeMaximal() {
        fireRedrawRequest(true);
    }

    private void fireRedrawRequest(final boolean maxRange) {
        for (Map.Entry<YAxis, Map<Band, EVEValues>> entry : dataMapPerUnitLabel.entrySet()) {
            YAxis yAxisElement = entry.getKey();
            Map<Band, EVEValues> bandMap = entry.getValue();

            final Band[] bands = bandMap.keySet().toArray(new Band[0]);

            String unitLabel = "";
            boolean isLog = false;
            if (bands.length > 0) {
                unitLabel = bands[0].getUnitLabel();
                isLog = bands[0].getBandType().isLogScale();
            }

            if (!eveDrawableElementMap.containsKey(yAxisElement)) {
                eveDrawableElementMap.put(yAxisElement, new EVEDrawableElement());
            }

            Range newAvailableRange = new Range();
            for (EVEValues v : bandMap.values()) {
                newAvailableRange.setMin(v.getMinimumValue());
                newAvailableRange.setMax(v.getMaximumValue());
            }
            if (newAvailableRange.max == newAvailableRange.min) {
                newAvailableRange.setMin(newAvailableRange.min - newAvailableRange.min / 10);
                newAvailableRange.setMax(newAvailableRange.max + newAvailableRange.max / 10);
            }
            yAxisElement.setAvailableRange(new Range(newAvailableRange));
            if (maxRange) {
                yAxisElement.setSelectedRange(new Range(newAvailableRange));
            }
            yAxisElement.set(unitLabel, isLog);

            EVEDrawableElement eveDrawableElement = eveDrawableElementMap.get(yAxisElement);
            eveDrawableElement.set(bands, yAxisElement);

            if (bands.length > 0) {
                drawController.updateDrawableElement(eveDrawableElement, true);
            } else {
                drawController.removeDrawableElement(eveDrawableElement);
            }
        }
    }

    private final EVEValues retrieveData(final Band band, final Interval interval, Rectangle plotArea) {
        return EVECacheController.getSingletonInstance().downloadData(band, interval, plotArea);
    }

    // Zoom Controller Listener

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        this.keepFullValueRange = keepFullValueRange;
        selectedIntervalChanged = true;

    }

    // Band Controller Listener

    public void bandAdded(final BandType bandType) {
        if (!bandTypes.contains(bandType)) {
            bandTypes.add(bandType);
            Band band = new Band(bandType);
            band.setDataColor(BandColors.getNextColor());
            DownloadController.getSingletonInstance().updateBand(band, drawController.getAvailableInterval(), drawController.getSelectedInterval());
            addToMap(band);
            selectorModel.addLineData(band);
        }
    }

    public void bandUpdated(final Band band) {
        if (band.isVisible()) {
            addToMap(band);
        } else {
            removeFromMap(band);
        }
    }

    public void bandRemoved(final Band band) {
        bandTypes.remove(band.getBandType());
        DownloadController.getSingletonInstance().stopDownloads(band);
        removeFromMap(band);
        fixAxis();
        selectorModel.removeLineData(band);
    }

    // EVE Cache Controller Listener

    private void fixAxis() {
        boolean hadLeftAxis = false;
        List<Band> rightAxisBands = null;
        for (Map.Entry<YAxis, List<Band>> yEntry : bandsPerYAxis.entrySet()) {
            if (drawController.getYAxisLocation(yEntry.getKey()) == YAxisLocation.LEFT) {
                hadLeftAxis = true;
            } else {
                rightAxisBands = yEntry.getValue();
            }
        }
        if (!hadLeftAxis && rightAxisBands != null) {
            for (Band b : rightAxisBands) {
                if (canChangeAxis(b)) {
                    changeAxis(b);
                }
            }
        }
    }

    @Override
    public void dataAdded(final Band band) {
        selectedIntervalChanged = true;
    }

    public EVEValues getValues(Band band, Interval interval, Rectangle graphArea) {
        return dataMapPerUnitLabel.get(yAxisElementMap.get(band)).get(band);
    }

    public void bandColorChanged(Band band) {
        fireRedrawRequest(false);
    }

    public boolean hasDataInSelectedInterval(Band band) {
        return EVECacheController.getSingletonInstance().hasDataInSelectedInterval(band, drawController.getSelectedInterval());
    }

    public void changeAxis(Band band) {
        YAxis currentYAxisElement = yAxisElementMap.get(band);
        if (((bandsPerYAxis.size() == 1 && bandsPerYAxis.get(currentYAxisElement).size() > 1) || bandsPerYAxis.size() == 2) && drawController.canChangeAxis(band.getUnitLabel())) {
            YAxis otherYAxisElement = getOtherAxisElement(currentYAxisElement);
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
                updateBand(band);
                fireRedrawRequest(true);
            }
        }
    }

    private void resetAvailableRange() {
        for (YAxis yAxisElement : dataMapPerUnitLabel.keySet()) {
            yAxisElement.reset();
        }
    }

    private YAxis getOtherAxisElement(YAxis currentYAxisElement) {
        if (drawController.canChangeAxis(currentYAxisElement.getOriginalLabel())) {
            Set<YAxis> allYAxisElements = bandsPerYAxis.keySet();
            if (allYAxisElements.size() == 2) {
                for (YAxis el : allYAxisElements) {
                    if (!el.equals(currentYAxisElement)) {
                        return el;
                    }
                }
            }
            YAxis other = new YAxis();
            other.addValueSpaceListener(this);
            return other;
        }
        return null;
    }

    public boolean canChangeAxis(Band band) {
        return drawController.canChangeAxis(band.getUnitLabel()) && yAxisElementMap.size() > 1 && (drawController.getYAxisLocation(yAxisElementMap.get(band)) == YAxis.YAxisLocation.RIGHT || (drawController.getYAxisLocation(yAxisElementMap.get(band)) == YAxis.YAxisLocation.LEFT && bandsPerYAxis.get(yAxisElementMap.get(band)).size() > 1));
    }

    public int getAxisLocation(Band band) {
        return drawController.getYAxisLocation(yAxisElementMap.get(band)) == YAxis.YAxisLocation.LEFT ? 0 : 1;
    }

    @Override
    public void valueSpaceChanged(Range availableRange, Range selectedRange) {
        fireRedrawRequest(false);
    }

    private class SelectedIntervalTimerTask implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedIntervalChanged) {
                selectedIntervalChanged = false;
                updateBands();
                fireRedrawRequest(keepFullValueRange);
            }
        }
    }

    public Set<Band> getAllBands() {
        return yAxisElementMap.keySet();
    }

    public boolean containsBandType(BandType value) {
        return bandTypes.contains(value);
    }

}
