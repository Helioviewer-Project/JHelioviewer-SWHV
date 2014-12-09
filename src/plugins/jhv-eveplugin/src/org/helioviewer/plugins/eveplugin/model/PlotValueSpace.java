package org.helioviewer.plugins.eveplugin.model;

import java.util.ArrayList;
import java.util.List;

public class PlotValueSpace implements PlotAreaSpaceListener {
    private double minValue;
    private double maxValue;
    private double selectedMinValue;
    private double selectedMaxValue;

    private final List<PlotValueSpaceListener> listeners;

    private PlotAreaSpace plotAreaSpace;

    public PlotValueSpace() {
        minValue = -1.0;
        maxValue = -1.0;
        selectedMinValue = -1.0;
        selectedMaxValue = -1.0;
        listeners = new ArrayList<PlotValueSpaceListener>();
        plotAreaSpace.addPlotAreaSpaceListener(this);
    }

    public void addPlotValueSpaceListener(PlotValueSpaceListener listener) {
        listeners.add(listener);
    }

    public void removePlotValueSpaceListener(PlotValueSpaceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
            double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime,
            boolean forced) {
        if (minValue != -1.0 && maxValue != -1.0 && selectedMinValue != -1.0 && selectedMaxValue != -1.0) {
            double diffValue = maxValue - minValue;
            double scaleDiff = scaledMaxValue - scaledMinValue;
            double selectedMin = (scaledSelectedMinValue - scaledMinValue) / scaleDiff;
            double selectedMax = (scaledSelectedMaxValue - scaledMinValue) / scaleDiff;
            selectedMinValue = minValue + Math.round(diffValue * selectedMin);
            selectedMaxValue = minValue + Math.round(diffValue * selectedMax);
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
        selectedMinValue = minValue;
        selectedMaxValue = maxValue;
        fireAvailableIntervalChanged();
    }

    public void setSelectedMinAndSelectedMaxValue(double selectedMinValue, double selectedMaxValue) {
        this.selectedMinValue = selectedMinValue;
        this.selectedMaxValue = selectedMaxValue;
        fireSelectedIntervalChanged();
    }

    private void fireSelectedIntervalChanged() {
        for (PlotValueSpaceListener l : listeners) {
            l.selectedIntervalChanged(selectedMinValue, selectedMaxValue);
        }

    }

    private void fireAvailableIntervalChanged() {
        for (PlotValueSpaceListener l : listeners) {
            l.availableIntervalChanged(selectedMinValue, selectedMaxValue);
        }
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime,
            double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub

    }

}
