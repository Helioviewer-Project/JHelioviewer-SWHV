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

    private final JSlider brightnessSlider;
    private final JLabel brightnessLabel;
    private final JPanel buttonPanel;
    private final int STEP = 10;

    public BrightnessPanel() {
        brightnessLabel = new JLabel("1.0");
        brightnessSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
        brightnessSlider.addChangeListener(this);
        brightnessSlider.setMinorTickSpacing(STEP);
        brightnessSlider.setSnapToTicks(true);
        WheelSupport.installMouseWheelSupport(brightnessSlider);

        JButton autoButton = new JButton("Auto");
        autoButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        autoButton.setBorderPainted(false);
        autoButton.setFocusPainted(false);
        autoButton.setContentAreaFilled(false);
        autoButton.setToolTipText("Auto brightness");
        autoButton.addActionListener(e -> {
            double auto = ((ImageLayerOptions) getComponent().getParent()).getAutoBrightness();
            brightnessSlider.setValue((int) (auto * 100));
        });

        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(brightnessLabel, BorderLayout.LINE_START);
        buttonPanel.add(autoButton, BorderLayout.LINE_END);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        float brightness = 0.1f * (brightnessSlider.getValue() / STEP);
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setBrightness(brightness);
        brightnessLabel.setText(String.format("%.1f", brightness));
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Brightness");
    }

    @Override
    public Component getComponent() {
        return brightnessSlider;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

}
