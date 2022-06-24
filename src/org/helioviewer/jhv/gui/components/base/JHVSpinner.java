package org.helioviewer.jhv.gui.components.base;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

@SuppressWarnings("serial")
public class JHVSpinner extends JSpinner {

    public JHVSpinner(SpinnerModel model) {
        super(model);
        WheelSupport.installMouseWheelSupport(this);
    }

    public JHVSpinner(double value, double min, double max, double step) {
        this(new SpinnerNumberModel(value, min, max, step));
    }

    public JHVSpinner(int value, int min, int max, int step) {
        this(new SpinnerNumberModel(value, min, max, step));
    }

    @Override
    public Object getValue() {
        try {
            commitEdit();
        } catch (Exception ignore) {
        }
        return super.getValue();
    }

}
