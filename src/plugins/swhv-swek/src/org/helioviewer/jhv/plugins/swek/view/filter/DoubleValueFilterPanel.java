package org.helioviewer.jhv.plugins.swek.view.filter;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.download.SWEKOperand;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;

public class DoubleValueFilterPanel extends AbstractFilterPanel {

    /** The UID */
    private static final long serialVersionUID = 1849764035063972566L;

    /** The value spinner */
    private final JSpinner spinner;

    public DoubleValueFilterPanel(SWEKEventType eventType, SWEKParameter parameter) {
        super(eventType, parameter);
        SpinnerModel spinnerModel = new SpinnerNumberModel(min, max, middleValue, stepSize);
        spinner = new JSpinner(spinnerModel);
    }

    @Override
    public void filter() {
        SWEKParam param = new SWEKParam(parameter.getParameterName(), "" + spinner.getValue(), SWEKOperand.EQUALS);
        ArrayList<SWEKParam> params = new ArrayList<SWEKParam>();
        params.add(param);
        filterManager.addFilter(eventType, parameter, params);
    }

    @Override
    public JComponent initFilterComponents() {
        return spinner;
    }
}
