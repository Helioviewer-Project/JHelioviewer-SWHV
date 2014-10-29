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

public class DoubleMinFilterPanel extends AbstractFilterPanel {

    /** Minimum value spinner */
    private JSpinner minimumValueSpinner;

    public DoubleMinFilterPanel(SWEKEventType eventType, SWEKParameter parameter) {
        super(eventType, parameter);
        // TODO Auto-generated constructor stub
    }

    /**
     * The UID.
     */
    private static final long serialVersionUID = 979250707589258006L;

    @Override
    public void filter(boolean active) {
        if (active) {
            SWEKParam paramMin = new SWEKParam(parameter.getParameterName(), "" + minimumValueSpinner.getValue(),
                    SWEKOperand.BIGGER_OR_EQUAL);
            ArrayList<SWEKParam> params = new ArrayList<SWEKParam>();
            params.add(paramMin);
            filterManager.addFilter(eventType, parameter, params);

        } else {
            filterManager.removedFilter(eventType, parameter);
        }

    }

    @Override
    public JComponent initFilterComponents() {
        SpinnerModel minimumSpinnerModel = new SpinnerNumberModel(middleValue, min, max, stepSize);

        minimumValueSpinner = new JSpinner(minimumSpinnerModel);
        minimumValueSpinner.setEditor(new JSpinner.NumberEditor(minimumValueSpinner, getSpinnerFormat(min, max)));
        minimumValueSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                filterManager.removedFilter(eventType, parameter);
                filterToggleButton.setSelected(false);
            }
        });

        WheelSupport.installMouseWheelSupport(minimumValueSpinner);
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        p.add(new JLabel("Minimum Value:"), c);
        c.gridx = 1;
        p.add(minimumValueSpinner, c);
        return p;
    }

}
