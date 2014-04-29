package org.helioviewer.plugins.eveplugin.model;

import java.util.ArrayList;
import java.util.List;

public class PlotAreaSpace {
    // private static PlotAreaSpace instance;

    private double scaledMinValue;
    private double scaledMaxValue;
    private double scaledMinTime;
    private double scaledMaxTime;
    private double scaledSelectedMinValue;
    private double scaledSelectedMaxValue;
    private double scaledSelectedMinTime;
    private double scaledSelectedMaxTime;

    private List<PlotAreaSpaceListener> listeners;

    public PlotAreaSpace() {
        listeners = new ArrayList<PlotAreaSpaceListener>();

        this.scaledMinValue = 0.0;
        this.scaledMaxValue = 1.0;
        this.scaledMinTime = 0.0;
        this.scaledMaxTime = 1.0;
        this.scaledSelectedMinValue = 0.0;
        this.scaledSelectedMaxValue = 1.0;
        this.scaledSelectedMinTime = 0.0;
        this.scaledSelectedMaxTime = 1.0;
    }

    /*
     * public static PlotAreaSpace getInstance(){ if(instance == null){ instance
     * = new PlotAreaSpace(); } return instance; }
     */

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
        if (this.scaledMinTime != scaledMinValue) {
            this.scaledMinValue = scaledMinValue;
            firePlotAreaSpaceChanged();
        }
    }

    public double getScaledMaxValue() {
        return scaledMaxValue;
    }

    public void setScaledMaxValue(double scaledMaxValue) {
        if (this.scaledMaxValue != scaledMaxValue) {
            this.scaledMaxValue = scaledMaxValue;
            firePlotAreaSpaceChanged();
        }
    }

    public double getScaledMinTime() {
        return scaledMinTime;
    }

    public void setScaledMinTime(double scaledMinTime) {
        if (this.scaledMinTime != scaledMinTime) {
            this.scaledMinTime = scaledMinTime;
            firePlotAreaSpaceChanged();
        }
    }

    public double getScaledMaxTime() {
        return scaledMaxTime;
    }

    public void setScaledMaxTime(double scaledMaxTime) {
        if (this.scaledMaxTime != scaledMaxTime) {
            this.scaledMaxTime = scaledMaxTime;
            firePlotAreaSpaceChanged();
        }
    }

    public double getScaledSelectedMinValue() {
        return scaledSelectedMinValue;
    }

    public void setScaledSelectedMinValue(double scaledSelectedMinValue) {
        if (this.scaledSelectedMinValue != scaledSelectedMinValue) {
            this.scaledSelectedMinValue = scaledSelectedMinValue;
            firePlotAreaSpaceChanged();
        }
    }

    public double getScaledSelectedMaxValue() {
        return scaledSelectedMaxValue;
    }

    public void setScaledSelectedMaxValue(double scaledSelectedMaxValue) {
        if (this.scaledSelectedMaxValue != scaledSelectedMaxValue) {
            this.scaledSelectedMaxValue = scaledSelectedMaxValue;
            firePlotAreaSpaceChanged();
        }
    }

    public double getScaledSelectedMinTime() {
        return scaledSelectedMinTime;
    }

    public void setScaledSelectedMinTime(double scaledSelectedMinTime) {
        if (this.scaledSelectedMinTime != scaledSelectedMinTime) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            firePlotAreaSpaceChanged();
        }
    }

    public double getScaledSelectedMaxTime() {
        return scaledSelectedMaxTime;
    }

    public void setScaledSelectedMaxTime(double scaledSelectedMaxTime) {
        if (this.scaledSelectedMaxTime != scaledSelectedMaxTime) {
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            firePlotAreaSpaceChanged();
        }
    }

    public void setScaledSelectedTime(double scaledSelectedMinTime, double scaledSelectedMaxTime) {
        if (!(this.scaledSelectedMinTime == scaledSelectedMinTime && this.scaledSelectedMaxTime == scaledSelectedMaxTime)) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            firePlotAreaSpaceChanged();
        }
    }

    public void setScaledSelectedValue(double scaledSelectedMinValue, double scaledSelectedMaxValue) {
        synchronized (this) {
            if (!(this.scaledSelectedMinValue == scaledSelectedMinValue && this.scaledSelectedMaxValue == scaledSelectedMaxValue)) {
                this.scaledSelectedMinValue = scaledSelectedMinValue;
                this.scaledSelectedMaxValue = scaledSelectedMaxValue;
                firePlotAreaSpaceChanged();
            }
        }
    }

    public void setScaledSelectedTimeAndValue(double scaledSelectedMinTime, double scaledSelectedMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue) {
        if (!(this.scaledSelectedMinTime == scaledSelectedMinTime && this.scaledSelectedMaxTime == scaledSelectedMaxTime && this.scaledSelectedMinValue == scaledSelectedMinValue && this.scaledSelectedMaxValue == scaledSelectedMaxValue)) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            this.scaledSelectedMinValue = scaledSelectedMinValue;
            this.scaledSelectedMaxValue = scaledSelectedMaxValue;
            firePlotAreaSpaceChanged();
        }
    }

    public boolean minMaxTimeIntervalContainsTime(double value) {
        return value >= this.scaledMinTime && value <= this.scaledMaxTime;
    }

    public boolean minMaxValueIntervalContainsValue(double value) {
        return value >= this.scaledMinValue && value <= this.scaledMaxValue;
    }

    public String toString() {
        return "Scaled min time  : " + scaledMinTime + "\n" + "Scaled max time  : " + scaledMaxTime + "\n" + "Scaled min value : " + scaledMinValue + "\n" + "Scaled max value : " + scaledMaxValue + "\n" + "Selected scaled min time  : " + scaledSelectedMinTime + "\n" + "Selected scaled max time  : " + scaledSelectedMaxTime + "\n" + "Selected scaled min value : " + scaledSelectedMinValue + "\n" + "Selected scaled max value : " + scaledSelectedMaxValue + "\n";

    }

    private void firePlotAreaSpaceChanged() {
        for (PlotAreaSpaceListener l : listeners) {
            l.plotAreaSpaceChanged(scaledMinValue, scaledMaxValue, scaledMinTime, scaledMaxTime, scaledSelectedMinValue, scaledSelectedMaxValue, scaledSelectedMinTime, scaledSelectedMaxTime);
        }
    }
}
