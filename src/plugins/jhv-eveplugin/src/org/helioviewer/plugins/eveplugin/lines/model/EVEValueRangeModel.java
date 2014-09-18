package org.helioviewer.plugins.eveplugin.lines.model;

import java.util.List;

import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;

public class EVEValueRangeModel implements PlotAreaSpaceListener {

    private static EVEValueRangeModel singletonInstance;

    private List<EVEValueRangeModelListener> listeners;
    private Range selectedInterval;
    private Range availableInterval;

    private PlotAreaSpace plotAreaSpace;

    private EVEValueRangeModel() {
        plotAreaSpace.addPlotAreaSpaceListener(this);
    }

    public static EVEValueRangeModel getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new EVEValueRangeModel();
        }
        return singletonInstance;
    }

    public void addValueRangeModelListener(EVEValueRangeModelListener listener) {
        listeners.add(listener);
    }

    public void removeValueRangeListener(EVEValueRangeModelListener listener) {
        listeners.remove(listener);
    }

    public Range getSelectedInterval() {
        return selectedInterval;
    }

    public void setSelectedInterval(Range selectedInterval) {
        this.selectedInterval = selectedInterval;
        fireSelectedIntervalRangeChanged();
    }

    public Range getAvailableInterval() {
        return availableInterval;
    }

    public void setAvailableInterval(Range availableInterval) {
        this.availableInterval = availableInterval;
        fireAvailbleIntervalRangeChanged();
    }

    public double getAvailableIntervalMin() {
        return availableInterval.min;
    }

    public void setAvailableIntervalMin(double availableIntervalMin) {
        availableInterval.min = availableIntervalMin;
        fireAvailbleIntervalRangeChanged();
    }

    public double getAvailableIntervalMax() {
        return availableInterval.max;
    }

    public void setAvailableIntervalMax(double availableIntervalMax) {
        availableInterval.max = availableIntervalMax;
        fireAvailbleIntervalRangeChanged();
    }

    public double getSelectedIntervalMin() {
        return selectedInterval.min;
    }

    public void setSelectedIntervalMin(double selectedIntervalMin) {
        selectedInterval.min = selectedIntervalMin;
        fireSelectedIntervalRangeChanged();
    }

    public double getSelectedIntervalMax() {
        return selectedInterval.max;
    }

    public void setSelectedIntervalMax(double selectedIntervalMax) {
        selectedInterval.max = selectedIntervalMax;
        fireSelectedIntervalRangeChanged();
    }

    private void fireSelectedIntervalRangeChanged() {
        for (EVEValueRangeModelListener l : listeners) {
            l.selectedRangeChanged(selectedInterval);
        }
    }

    private void fireAvailbleIntervalRangeChanged() {
        for (EVEValueRangeModelListener l : listeners) {
            l.availableRangeChanged(availableInterval);
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
            double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime,
            boolean forced) {
        // TODO Auto-generated method stub

    }
}
