package org.helioviewer.jhv.plugins.eveplugin.draw;

public interface PlotAreaSpaceListener {

    public abstract void plotAreaSpaceChanged(double scaledMinTime, double scaledMaxTime, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced);

    public abstract void availablePlotAreaSpaceChanged(double oldMinTime, double oldMaxTime, double newMinTime, double newMaxTime);
}
