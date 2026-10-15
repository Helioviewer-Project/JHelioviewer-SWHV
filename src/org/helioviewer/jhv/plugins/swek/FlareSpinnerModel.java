package org.helioviewer.jhv.plugins.swek;

import javax.swing.AbstractSpinnerModel;

import org.helioviewer.jhv.astronomy.GOESLevel;

@SuppressWarnings("serial")
class FlareSpinnerModel extends AbstractSpinnerModel {

    private String curval;
    private final double step;
    private final double min;
    private final double max;

    FlareSpinnerModel(double start, double _min, double _max, double _step) {
        min = displayedValue(_min);
        max = displayedValue(_max);
        step = _step;
        curval = GOESLevel.getStringValue(Math.clamp(start, min, max));
    }

    private static double displayedValue(double value) {
        return GOESLevel.getFloatValue(GOESLevel.getStringValue(value));
    }

    @Override
    public Object getValue() {
        return curval;
    }

    @Override
    public void setValue(Object value) {
        String stringValue = value.toString();
        double numericValue = GOESLevel.getFloatValue(stringValue);
        if (numericValue < min || numericValue > max)
            throw new IllegalArgumentException("GOES class outside filter range: " + stringValue);
        curval = stringValue;
        fireStateChanged();
    }

    @Override
    public Object getNextValue() {
        double v = GOESLevel.getFloatValue(curval);
        double p = Math.log10(v);
        v += step * Math.pow(10., (int) (p == (int) p ? p : p - 1));
        return GOESLevel.getStringValue(Math.clamp(v, min, max));
    }

    @Override
    public Object getPreviousValue() {
        double v = GOESLevel.getFloatValue(curval);
        double p = Math.log10(v);
        v -= step * Math.pow(10., (int) (p - 1));
        return GOESLevel.getStringValue(Math.clamp(v, min, max));
    }

}
