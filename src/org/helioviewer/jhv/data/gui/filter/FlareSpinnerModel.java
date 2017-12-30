package org.helioviewer.jhv.data.gui.filter;

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
        v += step * Math.pow(10., -8 + (int) Math.log10(v / 1e-8));
        return GOESLevel.getStringValue(MathUtils.clip(v, min, max));
    }

    @Override
    public Object getPreviousValue() {
        double v = GOESLevel.getFloatValue(curval);
        double p = Math.log10(v / 1e-8);
        v -= step * Math.pow(10., -8 + (int) (p - (int) p == 0 ? p - 1 : p));
        return GOESLevel.getStringValue(MathUtils.clip(v, min, max));
    }

}
