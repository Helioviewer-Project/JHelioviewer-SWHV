package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class BrightnessPanel implements ChangeListener, FilterDetails {

    private final JSlider slider;
    private final JLabel label;
    private final JPanel buttonPanel;

    public BrightnessPanel() {
        slider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
        label = new JLabel(String.format("%3d%%", slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(slider);

        JButton autoButton = new JButton("Auto");
        autoButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        autoButton.setBorderPainted(false);
        autoButton.setFocusPainted(false);
        autoButton.setContentAreaFilled(false);
        autoButton.setToolTipText("Auto brightness");
        autoButton.addActionListener(e -> {
            double auto = ((ImageLayerOptions) getComponent().getParent()).getAutoBrightness();
            slider.setValue((int) (auto * 100));
        });

        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(label, BorderLayout.LINE_START);
        buttonPanel.add(autoButton, BorderLayout.LINE_END);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setBrightness(slider.getValue() / 100f);
        label.setText(String.format("%3d%%", slider.getValue()));
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Brightness", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

}
