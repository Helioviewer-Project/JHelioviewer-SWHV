package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlotAreaSpace {

    private double scaledMinTime;
    private double scaledMaxTime;
    private double scaledSelectedMinTime;
    private double scaledSelectedMaxTime;
    private double minSelectedTimeDiff;

    private final List<PlotAreaSpaceListener> listeners;

    private final Set<ValueSpace> valueSpaces;

    private static PlotAreaSpace singletonInstance;

    private PlotAreaSpace() {
        listeners = new ArrayList<PlotAreaSpaceListener>();

        scaledMinTime = 0.0;
        scaledMaxTime = 1.0;
        scaledSelectedMinTime = 0.0;
        scaledSelectedMaxTime = 1.0;
        minSelectedTimeDiff = 0;
        valueSpaces = new HashSet<ValueSpace>();
    }

    public static PlotAreaSpace getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new PlotAreaSpace();
        }
        return singletonInstance;
    }

    public void addPlotAreaSpaceListener(PlotAreaSpaceListener listener) {
        listeners.add(listener);
    }

    public void removePlotAreaSpaceListener(PlotAreaSpaceListener listener) {
        listeners.remove(listener);
    }

    public double getScaledMinTime() {
        return scaledMinTime;
    }

    public void setScaledMinTime(double scaledMinTime) {
        if (this.scaledMinTime != scaledMinTime) {
            this.scaledMinTime = scaledMinTime;
            firePlotAreaSpaceChanged(false);
        }
    }

    public double getScaledMaxTime() {
        return scaledMaxTime;
    }

    public void setScaledMaxTime(double scaledMaxTime) {
        if (this.scaledMaxTime != scaledMaxTime) {
            this.scaledMaxTime = scaledMaxTime;
            firePlotAreaSpaceChanged(false);
        }
    }

    public double getScaledSelectedMinTime() {
        return scaledSelectedMinTime;
    }

    public void setScaledSelectedMinTime(double scaledSelectedMinTime) {
        if (this.scaledSelectedMinTime != scaledSelectedMinTime) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            firePlotAreaSpaceChanged(false);
        }
    }

    public double getScaledSelectedMaxTime() {
        return scaledSelectedMaxTime;
    }

    public void setScaledSelectedMaxTime(double scaledSelectedMaxTime) {
        if (this.scaledSelectedMaxTime != scaledSelectedMaxTime) {
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            firePlotAreaSpaceChanged(false);
        }
    }

    public void setScaledSelectedTime(double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        if ((forced || !(this.scaledSelectedMinTime == scaledSelectedMinTime && this.scaledSelectedMaxTime == scaledSelectedMaxTime)) && (scaledSelectedMaxTime - scaledSelectedMinTime) > minSelectedTimeDiff) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            if (this.scaledSelectedMinTime < scaledMinTime || this.scaledSelectedMaxTime > scaledMaxTime) {
                double oldScaledMinTime = scaledMinTime;
                double oldScaledMaxTime = scaledMaxTime;
                scaledMinTime = Math.min(this.scaledSelectedMinTime, scaledMinTime);
                scaledMaxTime = Math.max(this.scaledSelectedMaxTime, scaledMaxTime);
                fireAvailableAreaSpaceChanged(oldScaledMinTime, oldScaledMaxTime, scaledMinTime, scaledMaxTime);
            }
            firePlotAreaSpaceChanged(forced);
        }
    }

    private void fireAvailableAreaSpaceChanged(double oldScaledMinTime, double oldScaledMaxTime, double newMinTime, double newMaxTime) {
        for (PlotAreaSpaceListener l : listeners) {
            l.availablePlotAreaSpaceChanged(oldScaledMinTime, oldScaledMaxTime, newMinTime, newMaxTime);
        }

    }

    public Set<ValueSpace> getValueSpaces() {
        return valueSpaces;
    }

    public boolean minMaxTimeIntervalContainsTime(double value) {
        return value >= scaledMinTime && value <= scaledMaxTime;
    }

    public void resetSelectedValueAndTimeInterval() {
        scaledSelectedMinTime = scaledMinTime;
        scaledSelectedMaxTime = scaledMaxTime;
        for (ValueSpace vs : valueSpaces) {
            vs.resetScaledSelectedRange();
        }
    }

    @Override
    public String toString() {
        return "Scaled min time  : " + scaledMinTime + "\n" + "Scaled max time  : " + scaledMaxTime + "\n" + "\n" + "Selected scaled min time  : " + scaledSelectedMinTime + "\n" + "Selected scaled max time  : " + scaledSelectedMaxTime + "\n";

    }

    private void firePlotAreaSpaceChanged(boolean forced) {
        for (PlotAreaSpaceListener l : listeners) {
            l.plotAreaSpaceChanged(scaledMinTime, scaledMaxTime, scaledSelectedMinTime, scaledSelectedMaxTime, forced);
        }
    }

    public void addValueSpace(ValueSpace valueSpace) {
        valueSpaces.add(valueSpace);
    }

    public void removeValueSpace(ValueSpace valueSpace) {
        valueSpaces.remove(valueSpace);
    }

    public double getMinSelectedTimeDiff() {
        return minSelectedTimeDiff;
    }

    public void setMinSelectedTimeDiff(double minSelectedTimeDiff) {
        this.minSelectedTimeDiff = minSelectedTimeDiff;
    }
}
