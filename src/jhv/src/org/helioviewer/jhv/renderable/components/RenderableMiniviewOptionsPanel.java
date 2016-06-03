package org.helioviewer.jhv.renderable.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
public class RenderableMiniviewOptionsPanel extends SmallPanel {

    private static final int DEFAULT = 10;
    int scale = DEFAULT;

    private final RenderableMiniview miniview;
    private JSpinner xSpinner;

    public RenderableMiniviewOptionsPanel(RenderableMiniview miniview) {
        this.miniview = miniview;

        createXSpinner();

        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.EAST;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        add(new JLabel("Size", JLabel.RIGHT), c0);

        c0.anchor = GridBagConstraints.WEST;
        c0.gridx = 1;
        add(xSpinner, c0);

        setSmall();
    }

    private void createXSpinner() {
        int min = 5, max = 15;

        xSpinner = new JSpinner();
        xSpinner.setModel(new SpinnerNumberModel(Double.valueOf(DEFAULT), Double.valueOf(min), Double.valueOf(max), Double.valueOf(1)));
        JFormattedTextField f = ((JSpinner.DefaultEditor) xSpinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.0f", "%", min, max));

        xSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                scale = ((Double) xSpinner.getValue()).intValue();
                miniview.reshapeViewport();
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(xSpinner);
    }

}
