package org.helioviewer.plugins.eveplugin.model;

public interface PlotValueSpaceListener {

    public abstract void selectedIntervalChanged(double selectedMinValue, double selectedMaxValue);

    public abstract void availableIntervalChanged(double selectedMinValue, double selectedMaxValue);

}
