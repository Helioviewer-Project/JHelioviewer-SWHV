package org.helioviewer.plugins.eveplugin.model;

public interface PlotAreaSpaceListener {

	public abstract void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue,
			double scaledMinTime, double scaledMaxTime,
			double scaledSelectedMinValue, double scaledSelectedMaxValue,
			double scaledSelectedMinTime, double scaledSelectedMaxTime);

}
