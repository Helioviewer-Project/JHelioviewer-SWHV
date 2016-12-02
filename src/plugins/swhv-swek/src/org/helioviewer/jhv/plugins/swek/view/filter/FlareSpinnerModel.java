package org.helioviewer.jhv.plugins.swek.view.filter;

import javax.swing.AbstractSpinnerModel;

@SuppressWarnings("serial")
class FlareSpinnerModel extends AbstractSpinnerModel {

    private String curval;
    private static final double incr = 0.1;

    public FlareSpinnerModel(String _start, String _end, String startval, double _stepsize) {
        curval = startval;
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

    private static String compose_next(double pv, String start, String alternative) {
        if (pv < 9.9) {
            pv += incr;
            return start + String.format("%.1f", pv);
        } else {
            return alternative;
        }
    }

    @Override
    public Object getNextValue() {
        if (curval.length() >= 2) {
            char v = curval.charAt(0);
            double pv = Double.parseDouble(curval.substring(1));
            switch (v) {
                case 'A':
                    return compose_next(pv, "A", "B1.0");
                case 'B':
                    return compose_next(pv, "B", "C1.0");
                case 'C':
                    return compose_next(pv, "C", "M1.0");
                case 'M':
                    return compose_next(pv, "M", "X1.0");
                case 'X':
                    if (pv <= 19.9) {
                        pv += 0.1;
                        return String.format("X%.1f", pv);
                    }
            }
        }
        return null;
    }

    private static String compose_prev(double pv, String start, String alternative) {
        if (pv >= 1.1) {
            pv -= incr;
            return start + String.format("%.1f", pv);
        } else {
            return alternative;
        }
    }

    @Override
    public Object getPreviousValue() {
        if (curval.length() >= 2) {
            char v = curval.charAt(0);
            double pv = Double.parseDouble(curval.substring(1));
            switch (v) {
                case 'X':
                    return compose_prev(pv, "X", "M9.9");
                case 'M':
                    return compose_prev(pv, "M", "C9.9");
                case 'C':
                    return compose_prev(pv, "C", "B9.9");
                case 'B':
                    return compose_prev(pv, "B", "A9.9");
                case 'A':
                    return compose_prev(pv, "A", null);
            }
        }
        return null;
    }

}
