package org.helioviewer.plugins.eveplugin.draw;

public interface PlotAreaSpaceListener {

    public abstract void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
            double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime,
            boolean forced);

    public abstract void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime,
            double newMinValue, double newMaxValue, double newMinTime, double newMaxTime);
}
