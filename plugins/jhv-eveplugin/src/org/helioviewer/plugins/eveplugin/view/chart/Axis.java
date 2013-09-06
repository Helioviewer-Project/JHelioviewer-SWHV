package org.helioviewer.plugins.eveplugin.view.chart;

import java.awt.Graphics;
import org.helioviewer.plugins.eveplugin.base.Range;



public class Axis {
	private final double [] original_ticks;
	private final Range timeRange;
	private final int beginInt;
	private final int endInt;
	private final boolean is_linear;
	private final boolean is_logarithmic;
	private final boolean is_unspecified;

	public Axis( Range timeRange, boolean is_linear, int beginInt, int endInt ){
		this.timeRange = timeRange;
		this.endInt = endInt;
		this.beginInt = beginInt;
		this.is_linear = is_linear;
		this.is_logarithmic = false;
		this.is_unspecified = false;
		original_ticks = new double[10];
		//double delta = (timeRange.max-timeRange.min)/original_ticks.length;
		for(int j=beginInt; j<=endInt; j++){
			original_ticks[j] = (timeRange.min) +(timeRange.min-timeRange.max)/(beginInt-endInt)*(j-beginInt);
		}
	}

	public void display(Graphics g){
		
	}
}
