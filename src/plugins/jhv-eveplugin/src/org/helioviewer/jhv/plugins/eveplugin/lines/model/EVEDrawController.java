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
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.RangeListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
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

public class EVEDrawController implements TimingListener, EVECacheControllerListener, RangeListener {

    private final Map<YAxis, Map<Band, EVEValues>> dataMapPerUnitLabel = new HashMap<YAxis, Map<Band, EVEValues>>();
    private final DrawController drawController;
    private final Set<BandType> bandTypes;
    private final Map<YAxis, EVEDrawableElement> eveDrawableElementMap;
    private final Map<Band, YAxis> yAxisMap;
    private final Map<YAxis, List<Band>> bandsPerYAxis;

    private static EVEDrawController instance;
    private final Timer selectedIntervalChangedTimer;
    private boolean selectedIntervalChanged;
    private boolean keepFullValueRange;
    private final LineDataSelectorModel selectorModel;

    private EVEDrawController() {
        drawController = EVEPlugin.dc;
        drawController.addTimingListener(this);

        EVECacheController.getSingletonInstance().addControllerListener(this);
        selectorModel = LineDataSelectorModel.getSingletonInstance();
        eveDrawableElementMap = new HashMap<YAxis, EVEDrawableElement>();
        bandTypes = new HashSet<BandType>();
        yAxisMap = new HashMap<Band, YAxis>();
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
        YAxis yAxis = drawController.getYAxisForUnit(band.getUnitLabel());
        if (yAxis == null && drawController.hasAxisAvailable()) {
            yAxis = new YAxis();
            EVEPlugin.dc.addRangeListener(this);
        }
        if (yAxis != null) {
            yAxisMap.put(band, yAxis);
            addToBandsPerYAxis(yAxis, band);
            EVEValues data = retrieveData(band, interval, plotArea);

            if (!dataMapPerUnitLabel.containsKey(yAxis)) {
                dataMapPerUnitLabel.put(yAxis, new HashMap<Band, EVEValues>());
            }
            dataMapPerUnitLabel.get(yAxis).put(band, data);
        } else {
            Log.debug("band could not be added. No Yaxis Available ");
        }
        fireRedrawRequest(true);
    }

    private void addToBandsPerYAxis(YAxis yAxis, Band band) {
        List<Band> bands = new ArrayList<Band>();
        if (bandsPerYAxis.containsKey(yAxis)) {
            bands = bandsPerYAxis.get(yAxis);
        }
        bands.add(band);
        bandsPerYAxis.put(yAxis, bands);
    }

    private void removeFromMap(final Band band) {
        YAxis yAxis = yAxisMap.get(band);
        if (dataMapPerUnitLabel.containsKey(yAxis)) {
            if (dataMapPerUnitLabel.get(yAxis).containsKey(band)) {
                dataMapPerUnitLabel.get(yAxis).remove(band);
                List<Band> bands = bandsPerYAxis.get(yAxis);
                bands.remove(band);
                if (bands.isEmpty()) {
                    EVEDrawableElement removed = eveDrawableElementMap.remove(yAxis);
                    yAxisMap.remove(band);
                    bandsPerYAxis.remove(yAxis);
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
        YAxis yAxis = yAxisMap.get(band);
        if (!dataMapPerUnitLabel.containsKey(yAxis)) {
            dataMapPerUnitLabel.put(yAxis, new HashMap<Band, EVEValues>());
        }
        dataMapPerUnitLabel.get(yAxis).put(band, data);
    }

    private void updateBands() {
        for (Map<Band, EVEValues> value : dataMapPerUnitLabel.values()) {
            for (final Band band : value.keySet()) {
                updateBand(band);
            }
        }
    }

    private void fireRedrawRequest(final boolean maxRange) {
        for (Map.Entry<YAxis, Map<Band, EVEValues>> entry : dataMapPerUnitLabel.entrySet()) {
            YAxis yAxis = entry.getKey();
            Map<Band, EVEValues> bandMap = entry.getValue();

            final Band[] bands = bandMap.keySet().toArray(new Band[0]);

            String unitLabel = "";
            boolean isLog = false;
            if (bands.length > 0) {
                unitLabel = bands[0].getUnitLabel();
                isLog = bands[0].getBandType().isLogScale();
            }

            if (!eveDrawableElementMap.containsKey(yAxis)) {
                eveDrawableElementMap.put(yAxis, new EVEDrawableElement());
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
            if (maxRange) {
                yAxis.setSelectedRange(new Range(newAvailableRange));
            }
            yAxis.set(unitLabel, isLog);

            EVEDrawableElement eveDrawableElement = eveDrawableElementMap.get(yAxis);
            eveDrawableElement.set(bands, yAxis);

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

    public EVEValues getValues(Band band, Rectangle graphArea) {
        return dataMapPerUnitLabel.get(yAxisMap.get(band)).get(band);
    }

    public void bandColorChanged(Band band) {
        fireRedrawRequest(false);
    }

    public boolean hasDataInSelectedInterval(Band band) {
        return EVECacheController.getSingletonInstance().hasDataInSelectedInterval(band, drawController.getSelectedInterval());
    }

    public void changeAxis(Band band) {
        YAxis currentYAxis = yAxisMap.get(band);
        if (((bandsPerYAxis.size() == 1 && bandsPerYAxis.get(currentYAxis).size() > 1) || bandsPerYAxis.size() == 2) && drawController.canChangeAxis(band.getUnitLabel())) {
            YAxis otherYAxis = getOtherAxisElement(currentYAxis);
            if (otherYAxis != null) {
                yAxisMap.put(band, otherYAxis);
                List<Band> bandsPerList = new ArrayList<Band>();
                if (bandsPerYAxis.containsKey(otherYAxis)) {
                    bandsPerList = bandsPerYAxis.get(otherYAxis);
                }
                bandsPerList.add(band);
                bandsPerYAxis.put(otherYAxis, bandsPerList);
                bandsPerYAxis.get(currentYAxis).remove(band);
                Map<Band, EVEValues> valuesPerBand = new HashMap<Band, EVEValues>();
                if (!dataMapPerUnitLabel.containsKey(otherYAxis)) {
                    dataMapPerUnitLabel.put(otherYAxis, valuesPerBand);
                }
                dataMapPerUnitLabel.get(otherYAxis).put(band, dataMapPerUnitLabel.get(currentYAxis).get(band));
                dataMapPerUnitLabel.get(currentYAxis).remove(band);
                if (!eveDrawableElementMap.containsKey(otherYAxis)) {
                    eveDrawableElementMap.put(otherYAxis, new EVEDrawableElement());
                }
                resetAvailableRange();
                updateBand(band);
                fireRedrawRequest(true);
            }
        }
    }

    private void resetAvailableRange() {
        for (YAxis yAxis : dataMapPerUnitLabel.keySet()) {
            yAxis.reset();
        }
    }

    private YAxis getOtherAxisElement(YAxis currentYAxis) {
        if (drawController.canChangeAxis(currentYAxis.getOriginalLabel())) {
            Set<YAxis> allYAxes = bandsPerYAxis.keySet();
            if (allYAxes.size() == 2) {
                for (YAxis el : allYAxes) {
                    if (!el.equals(currentYAxis)) {
                        return el;
                    }
                }
            }
            YAxis other = new YAxis();
            EVEPlugin.dc.addRangeListener(this);
            return other;
        }
        return null;
    }

    public boolean canChangeAxis(Band band) {
        return drawController.canChangeAxis(band.getUnitLabel()) && yAxisMap.size() > 1 && (drawController.getYAxisLocation(yAxisMap.get(band)) == YAxis.YAxisLocation.RIGHT || (drawController.getYAxisLocation(yAxisMap.get(band)) == YAxis.YAxisLocation.LEFT && bandsPerYAxis.get(yAxisMap.get(band)).size() > 1));
    }

    public int getAxisLocation(Band band) {
        return drawController.getYAxisLocation(yAxisMap.get(band)) == YAxis.YAxisLocation.LEFT ? 0 : 1;
    }

    @Override
    public void rangeChanged() {
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
        return yAxisMap.keySet();
    }

    public boolean containsBandType(BandType value) {
        return bandTypes.contains(value);
    }

}
