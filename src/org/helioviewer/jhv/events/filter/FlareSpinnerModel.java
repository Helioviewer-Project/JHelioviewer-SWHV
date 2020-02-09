package org.helioviewer.jhv.events.filter;

import javax.swing.AbstractSpinnerModel;

import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
class FlareSpinnerModel extends AbstractSpinnerModel {

    private String curval;
    private final double step;
    private final double min;
    private final double max;

    FlareSpinnerModel(double start, double _min, double _max, double _step) {
        min = _min;
        max = _max;
        step = _step;
        curval = GOESLevel.getStringValue(MathUtils.clip(start, min, max));
    }

    @Override
    public Object getValue() {
        return curval;
    }

    @Override
    public void setValue(Object value) {
        curval = value.toString();
        fireStateChanged();
    }

    @Override
    public Object getNextValue() {
        double v = GOESLevel.getFloatValue(curval);
        double p = Math.log10(v);
        v += step * Math.pow(10., (int) (p == (int) p ? p : p - 1));
        return GOESLevel.getStringValue(MathUtils.clip(v, min, max));
    }

    @Override
    public Object getPreviousValue() {
        double v = GOESLevel.getFloatValue(curval);
        double p = Math.log10(v);
        v -= step * Math.pow(10., (int) (p - 1));
        return GOESLevel.getStringValue(MathUtils.clip(v, min, max));
    }

}
