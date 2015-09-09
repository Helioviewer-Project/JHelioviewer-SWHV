package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.util.ArrayList;
import java.util.List;

public class PlotAreaSpace {

    private double scaledMinValue;
    private double scaledMaxValue;
    private double scaledMinTime;
    private double scaledMaxTime;
    private double scaledSelectedMinValue;
    private double scaledSelectedMaxValue;
    private double scaledSelectedMinTime;
    private double scaledSelectedMaxTime;

    private final List<PlotAreaSpaceListener> listeners;

    private static PlotAreaSpace singletonInstance;

    private PlotAreaSpace() {
        listeners = new ArrayList<PlotAreaSpaceListener>();

        scaledMinValue = 0.0;
        scaledMaxValue = 1.0;
        scaledMinTime = 0.0;
        scaledMaxTime = 1.0;
        scaledSelectedMinValue = 0.0;
        scaledSelectedMaxValue = 1.0;
        scaledSelectedMinTime = 0.0;
        scaledSelectedMaxTime = 1.0;
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

    public double getScaledMinValue() {
        return scaledMinValue;
    }

    public void setScaledMinValue(double scaledMinValue) {
        if (scaledMinTime != scaledMinValue) {
            this.scaledMinValue = scaledMinValue;
            firePlotAreaSpaceChanged(false);
        }
    }

    public double getScaledMaxValue() {
        return scaledMaxValue;
    }

    public void setScaledMaxValue(double scaledMaxValue) {
        if (this.scaledMaxValue != scaledMaxValue) {
            this.scaledMaxValue = scaledMaxValue;
            firePlotAreaSpaceChanged(false);
        }
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

    public double getScaledSelectedMinValue() {
        return scaledSelectedMinValue;
    }

    public void setScaledSelectedMinValue(double scaledSelectedMinValue) {
        if (this.scaledSelectedMinValue != scaledSelectedMinValue) {
            this.scaledSelectedMinValue = scaledSelectedMinValue;
            firePlotAreaSpaceChanged(false);
        }
    }

    public double getScaledSelectedMaxValue() {
        return scaledSelectedMaxValue;
    }

    public void setScaledSelectedMaxValue(double scaledSelectedMaxValue) {
        if (this.scaledSelectedMaxValue != scaledSelectedMaxValue) {
            this.scaledSelectedMaxValue = scaledSelectedMaxValue;
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
        if (forced || !(this.scaledSelectedMinTime == scaledSelectedMinTime && this.scaledSelectedMaxTime == scaledSelectedMaxTime)) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            if (this.scaledSelectedMinTime < scaledMinTime || this.scaledSelectedMaxTime > scaledMaxTime) {
                double oldScaledMinTime = scaledMinTime;
                double oldScaledMaxTime = scaledMaxTime;
                scaledMinTime = Math.min(this.scaledSelectedMinTime, scaledMinTime);
                scaledMaxTime = Math.max(this.scaledSelectedMaxTime, scaledMaxTime);
                fireAvailableAreaSpaceChanged(scaledMinValue, scaledMaxValue, oldScaledMinTime, oldScaledMaxTime, scaledMinValue, scaledMaxValue, scaledMinTime, scaledMaxTime);
            }
            firePlotAreaSpaceChanged(forced);
        }
    }

    public void setScaledSelectedValue(double scaledSelectedMinValue, double scaledSelectedMaxValue, boolean forced) {
        if (forced || !(this.scaledSelectedMinValue == scaledSelectedMinValue && this.scaledSelectedMaxValue == scaledSelectedMaxValue)) {
            this.scaledSelectedMinValue = scaledSelectedMinValue;
            this.scaledSelectedMaxValue = scaledSelectedMaxValue;
            firePlotAreaSpaceChanged(forced);
        }
    }

    public void setScaledSelectedTimeAndValue(double scaledSelectedMinTime, double scaledSelectedMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue) {
        if (!(this.scaledSelectedMinTime == scaledSelectedMinTime && this.scaledSelectedMaxTime == scaledSelectedMaxTime && this.scaledSelectedMinValue == scaledSelectedMinValue && this.scaledSelectedMaxValue == scaledSelectedMaxValue)) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            this.scaledSelectedMinValue = scaledSelectedMinValue;
            this.scaledSelectedMaxValue = scaledSelectedMaxValue;
            if (this.scaledSelectedMinTime < scaledMinTime || this.scaledSelectedMaxTime > scaledMaxTime || this.scaledSelectedMinValue < scaledMinValue || this.scaledSelectedMaxValue > scaledMaxValue) {
                // Log.debug("setScaledSelectedTimeAndValue part 1 executed : this.scaledSelectedMinTime < scaledMinTime "
                // + (this.scaledSelectedMinTime < scaledMinTime) +
                // " this.scaledSelectedMaxTime > scaledMaxTime " +
                // (this.scaledSelectedMaxTime > scaledMaxTime) +
                // " this.scaledSelectedMinValue < scaledMinValue " +
                // (this.scaledSelectedMinValue < scaledMinValue) +
                // " this.scaledSelectedMaxValue > scaledMaxValue " +
                // (this.scaledSelectedMaxValue > scaledMaxValue));
                double oldScaledMinTime = scaledMinTime;
                double oldScaledMaxTime = scaledMaxTime;
                double oldScaledMinValue = scaledMinValue;
                double oldScaledMaxValue = scaledMaxValue;
                scaledMinTime = Math.min(this.scaledSelectedMinTime, scaledMinTime);
                scaledMaxTime = Math.max(this.scaledSelectedMaxTime, scaledMaxTime);
                scaledMinValue = Math.min(this.scaledSelectedMinValue, scaledMinValue);
                scaledMaxValue = Math.max(this.scaledSelectedMaxValue, scaledMaxValue);
                fireAvailableAreaSpaceChanged(oldScaledMinValue, oldScaledMaxValue, oldScaledMinTime, oldScaledMaxTime, scaledMinValue, scaledMaxValue, scaledMinTime, scaledMaxTime);
            } else {
                // Log.debug("setScaledSelectedTimeAndValue part 1 not executed : this.scaledSelectedMinTime < scaledMinTime "
                // + (this.scaledSelectedMinTime < scaledMinTime) +
                // " this.scaledSelectedMaxTime > scaledMaxTime " +
                // (this.scaledSelectedMaxTime > scaledMaxTime) +
                // " this.scaledSelectedMinValue < scaledMinValue " +
                // (this.scaledSelectedMinValue < scaledMinValue) +
                // " this.scaledSelectedMaxValue > scaledMaxValue " +
                // (this.scaledSelectedMaxValue > scaledMaxValue));
            }
            firePlotAreaSpaceChanged(false);
        } else {
            // Log.debug("setScaledSelectedTimeAndValue not executed: this.scaledSelectedMinTime == scaledSelectedMinTime "
            // + (this.scaledSelectedMinTime == scaledSelectedMinTime) +
            // " this.scaledSelectedMaxTime == scaledSelectedMaxTime " +
            // (this.scaledSelectedMaxTime == scaledSelectedMaxTime) +
            // " this.scaledSelectedMinValue == scaledSelectedMinValue " +
            // (this.scaledSelectedMinValue == scaledSelectedMinValue) +
            // " this.scaledSelectedMaxValue == scaledSelectedMaxValue" +
            // (this.scaledSelectedMaxValue == scaledSelectedMaxValue));
        }
    }

    private void fireAvailableAreaSpaceChanged(double oldScaledMinValue, double oldScaledMaxValue, double oldScaledMinTime, double oldScaledMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        for (PlotAreaSpaceListener l : listeners) {
            l.availablePlotAreaSpaceChanged(oldScaledMinValue, oldScaledMaxValue, oldScaledMinTime, oldScaledMaxTime, newMinValue, newMaxValue, newMinTime, newMaxTime);
        }

    }

    public boolean minMaxTimeIntervalContainsTime(double value) {
        return value >= scaledMinTime && value <= scaledMaxTime;
    }

    public boolean minMaxValueIntervalContainsValue(double value) {
        return value >= scaledMinValue && value <= scaledMaxValue;
    }

    public void resetSelectedValueAndTimeInterval() {
        setScaledSelectedTimeAndValue(scaledMinTime, scaledMaxTime, scaledMinValue, scaledMaxValue);
    }

    @Override
    public String toString() {
        return "Scaled min time  : " + scaledMinTime + "\n" + "Scaled max time  : " + scaledMaxTime + "\n" + "Scaled min value : " + scaledMinValue + "\n" + "Scaled max value : " + scaledMaxValue + "\n" + "Selected scaled min time  : " + scaledSelectedMinTime + "\n" + "Selected scaled max time  : " + scaledSelectedMaxTime + "\n" + "Selected scaled min value : " + scaledSelectedMinValue + "\n" + "Selected scaled max value : " + scaledSelectedMaxValue + "\n";

    }

    private void firePlotAreaSpaceChanged(boolean forced) {
        // Log.info("Listeners size : " + listeners.size());
        for (PlotAreaSpaceListener l : listeners) {
            // Log.info("listener : " + l);
            l.plotAreaSpaceChanged(scaledMinValue, scaledMaxValue, scaledMinTime, scaledMaxTime, scaledSelectedMinValue, scaledSelectedMaxValue, scaledSelectedMinTime, scaledSelectedMaxTime, forced);
        }
    }

}
