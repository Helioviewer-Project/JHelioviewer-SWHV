package org.helioviewer.plugins.eveplugin.draw;

import java.awt.Color;

import org.helioviewer.plugins.eveplugin.base.Range;

public class YAxisElement {

	private Range selectedRange;
	private Range availableRange;
	private String label;
	private double minValue;
	private double maxValue;
	private Color color;
	
	public YAxisElement(Range selectedRange, Range availableRange, String label, double minValue, double maxValue, Color color) {
		this.selectedRange = selectedRange;
		this.availableRange = availableRange;
		this.label = label;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.color = color;
	}

	public YAxisElement(){
		this.selectedRange = new Range();
		this.availableRange = new Range();
		this.label = "";
		this.minValue = 0.0;
		this.maxValue = 0.0;
		this.color = Color.BLACK;
	}
	
	public Range getSelectedRange() {
		return selectedRange;
	}

	public void setSelectedRange(Range selectedRange) {
		this.selectedRange = selectedRange;
	}

	public Range getAvailableRange() {
		return availableRange;
	}

	public void setAvailableRange(Range availableRange) {
		this.availableRange = availableRange;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
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

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
	public void set(Range availableRange, Range selectedRange, String label, double minValue, double maxValue, Color color){
		this.availableRange = availableRange;
		this.selectedRange = selectedRange;
		this.label = label;
		this.maxValue = maxValue;
		this.minValue = minValue;
		this.color = color;
	}
}
