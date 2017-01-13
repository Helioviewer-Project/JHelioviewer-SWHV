package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class BrightnessPanel implements ActionListener, ChangeListener, FilterDetails {

    private static final Icon enhanceIcon = IconBank.getIcon(JHVIcon.BLANK);
    private static final Border loweredBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
    private static final Border raisedBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED);

    private final JSlider brightnessSlider;
    private final JLabel brightnessLabel;
    private final JPanel buttonPanel;
    private final JToggleButton autoButton;

    public BrightnessPanel() {
        brightnessLabel = new JLabel("1.0");
        brightnessSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
        brightnessSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(brightnessSlider);

        autoButton = new JToggleButton(enhanceIcon);
        autoButton.setToolTipText("Auto brightness");
        autoButton.setBorder(raisedBorder);
        autoButton.addActionListener(this);

        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(brightnessLabel, BorderLayout.LINE_START);
        buttonPanel.add(autoButton, BorderLayout.LINE_END);
    }

   @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == autoButton) {
            autoButton.setBorder(autoButton.isSelected() ? loweredBorder : raisedBorder);
            ((ImageLayerOptions) getComponent().getParent()).getGLImage().setAutoBrightness(autoButton.isSelected());
            Displayer.render(1);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        float brightness = 0.1f * (brightnessSlider.getValue() / 10);
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
        //return brightnessLabel;
        return buttonPanel;
    }

}
