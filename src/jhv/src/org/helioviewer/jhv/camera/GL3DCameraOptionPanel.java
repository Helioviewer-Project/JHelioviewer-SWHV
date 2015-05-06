package org.helioviewer.jhv.camera;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

public abstract class GL3DCameraOptionPanel extends JPanel {

    private JPanel fovPanel;
    private JSpinner fovSpinner;

    abstract public void deactivate();

    public GL3DCameraOptionPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.createFOV();
    }

    private void createFOV() {
        this.fovPanel = new JPanel();
        this.fovPanel.setLayout(new BoxLayout(fovPanel, BoxLayout.LINE_AXIS));
        this.fovPanel.add(new JLabel("FOV angle"));
        fovSpinner = new JSpinner();
        fovSpinner.setModel(new SpinnerNumberModel(new Double(0.8), new Double(0.0), new Double(180.), new Double(0.01)));
        JFormattedTextField f = ((JSpinner.DefaultEditor) fovSpinner.getEditor()).getTextField();

        f.setFormatterFactory(new AbstractFormatterFactory() {
            @Override
            public AbstractFormatter getFormatter(JFormattedTextField tf) {
                return new DefaultFormatter() {
                    @Override
                    public Object stringToValue(String string) {
                        if (string == null || string.length() == 0) {
                            return new Double(0.);
                        }
                        return Double.parseDouble(string.substring(0, string.length() - 1));
                    }

                    @Override
                    public String valueToString(Object value) {
                        return String.format("%.2f\u00B0", value);
                    }

                    @Override
                    public Class<?> getValueClass() {
                        return Double.class;
                    }
                };
            }
        });

        Displayer.getActiveCamera().setFOVangleDegrees((Double) fovSpinner.getValue());

        this.fovSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Displayer.getActiveCamera().setFOVangleDegrees((Double) fovSpinner.getValue());
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(this.fovSpinner);
        this.fovPanel.add(this.fovSpinner);

        this.fovSpinner.setMaximumSize(new Dimension(6, 22));
        this.fovPanel.add(Box.createHorizontalGlue());
        add(this.fovPanel, BorderLayout.CENTER);
    }

}
