package org.helioviewer.plugins.eveplugin.model;

import java.util.ArrayList;
import java.util.List;

public class PlotValueSpace implements PlotAreaSpaceListener {
    private double minValue;
    private double maxValue;
    private double selectedMinValue;
    private double selectedMaxValue;

    private List<PlotValueSpaceListener> listeners;

    private PlotAreaSpace plotAreaSpace;

    public PlotValueSpace() {
        this.minValue = -1.0;
        this.maxValue = -1.0;
        this.selectedMinValue = -1.0;
        this.selectedMaxValue = -1.0;
        this.listeners = new ArrayList<PlotValueSpaceListener>();
        // this.plotAreaSpace = PlotAreaSpace.getInstance();
        plotAreaSpace.addPlotAreaSpaceListener(this);
    }

    public void addPlotValueSpaceListener(PlotValueSpaceListener listener) {
        listeners.add(listener);
    }

    public void removePlotValueSpaceListener(PlotValueSpaceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime) {
        if (this.minValue != -1.0 && this.maxValue != -1.0 && this.selectedMinValue != -1.0 && this.selectedMaxValue != -1.0) {
            // TODO calculate the new selected min and max value
            double diffValue = maxValue - minValue;
            double scaleDiff = scaledMaxValue - scaledMinValue;
            double selectedMin = (scaledSelectedMinValue - scaledMinValue) / scaleDiff;
            double selectedMax = (scaledSelectedMaxValue - scaledMinValue) / scaleDiff;
            this.selectedMinValue = minValue + Math.round(diffValue * selectedMin);
            this.selectedMaxValue = minValue + Math.round(diffValue * selectedMax);
            fireSelectedIntervalChanged();
        }
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getSelectedMinValue() {
        return selectedMinValue;
    }

    public void setSelectedMinValue(double selectedMinValue) {
        this.selectedMinValue = selectedMinValue;
    }

    public double getSelectedMaxValue() {
        return selectedMaxValue;
    }

    public void setSelectedMaxValue(double selectedMaxValue) {
        this.selectedMaxValue = selectedMaxValue;
    }

    public void setMinAndMaxValue(double minValue, double maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.selectedMinValue = minValue;
        this.selectedMaxValue = maxValue;
        fireAvailableIntervalChanged();
    }

    public void setSelectedMinAndSelectedMaxValue(double selectedMinValue, double selectedMaxValue) {
        this.selectedMinValue = selectedMinValue;
        this.selectedMaxValue = selectedMaxValue;
        fireSelectedIntervalChanged();
    }

    private void fireSelectedIntervalChanged() {
        for (PlotValueSpaceListener l : listeners) {
            l.selectedIntervalChanged(this.selectedMinValue, this.selectedMaxValue);
        }

    }

    private void fireAvailableIntervalChanged() {
        for (PlotValueSpaceListener l : listeners) {
            l.availableIntervalChanged(this.selectedMinValue, this.selectedMaxValue);
        }
    }

}
