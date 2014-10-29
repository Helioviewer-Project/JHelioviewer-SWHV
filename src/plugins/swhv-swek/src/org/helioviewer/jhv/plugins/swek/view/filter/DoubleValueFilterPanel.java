package org.helioviewer.jhv.plugins.swek.view.filter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.download.SWEKOperand;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;

public class DoubleValueFilterPanel extends AbstractFilterPanel {

    /** The UID */
    private static final long serialVersionUID = 1849764035063972566L;

    /** The value spinner */
    private JSpinner spinner;

    public DoubleValueFilterPanel(SWEKEventType eventType, SWEKParameter parameter) {
        super(eventType, parameter);
    }

    @Override
    public void filter(boolean active) {
        if (active) {
            SWEKParam param = new SWEKParam(parameter.getParameterName(), "" + spinner.getValue(), SWEKOperand.EQUALS);
            ArrayList<SWEKParam> params = new ArrayList<SWEKParam>();
            params.add(param);
            filterManager.addFilter(eventType, parameter, params);
        } else {
            filterManager.removedFilter(eventType, parameter);
        }
    }

    @Override
    public JComponent initFilterComponents() {
        SpinnerModel spinnerModel = new SpinnerNumberModel(middleValue, min, max, stepSize);

        spinner = new JSpinner(spinnerModel);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.00000000"));
        spinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                filterManager.removedFilter(eventType, parameter);
                filterToggleButton.setSelected(false);
            }
        });
        WheelSupport.installMouseWheelSupport(spinner);
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        p.add(new JLabel("Value:"), c);
        c.gridx = 1;
        p.add(spinner, c);
        return p;
    }
}
