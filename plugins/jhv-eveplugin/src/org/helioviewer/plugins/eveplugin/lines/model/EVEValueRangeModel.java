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
        // plotAreaSpace = PlotAreaSpace.getInstance();
        plotAreaSpace.addPlotAreaSpaceListener(this);
    }

    public static EVEValueRangeModel getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new EVEValueRangeModel();
        }
        return singletonInstance;
    }

    public void addValueRangeModelListener(EVEValueRangeModelListener listener) {
        this.listeners.add(listener);
    }

    public void removeValueRangeListener(EVEValueRangeModelListener listener) {
        this.listeners.remove(listener);
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
        return this.availableInterval.min;
    }

    public void setAvailableIntervalMin(double availableIntervalMin) {
        this.availableInterval.min = availableIntervalMin;
        fireAvailbleIntervalRangeChanged();
    }

    public double getAvailableIntervalMax() {
        return this.availableInterval.max;
    }

    public void setAvailableIntervalMax(double availableIntervalMax) {
        this.availableInterval.max = availableIntervalMax;
        fireAvailbleIntervalRangeChanged();
    }

    public double getSelectedIntervalMin() {
        return this.selectedInterval.min;
    }

    public void setSelectedIntervalMin(double selectedIntervalMin) {
        this.selectedInterval.min = selectedIntervalMin;
        fireSelectedIntervalRangeChanged();
    }

    public double getSelectedIntervalMax() {
        return this.selectedInterval.max;
    }

    public void setSelectedIntervalMax(double selectedIntervalMax) {
        this.selectedInterval.max = selectedIntervalMax;
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
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime) {
        // TODO Auto-generated method stub

    }
}
