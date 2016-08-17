package org.helioviewer.jhv.layers.filters;

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

public class ContrastPanel implements ActionListener, ChangeListener, FilterDetails {

    private static final float sliderToContrastScale = 25.f;

    private static final Icon enhanceIcon = IconBank.getIcon(JHVIcon.BLANK);
    private static final Border loweredBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
    private static final Border raisedBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED);

    private final JSlider contrastSlider;
    private final JLabel contrastLabel;
    private final JPanel buttonPanel;
    private final JToggleButton autoButton;

    public ContrastPanel() {
        contrastLabel = new JLabel("0");
        contrastSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        contrastSlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(contrastSlider);

        autoButton = new JToggleButton(enhanceIcon);
        autoButton.setToolTipText("Auto contrast");
        autoButton.setBorder(raisedBorder);
        autoButton.addActionListener(this);

        buttonPanel = new JPanel();
        buttonPanel.add(contrastLabel);
        buttonPanel.add(autoButton);
    }

   @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == autoButton) {
            autoButton.setBorder(autoButton.isSelected() ? loweredBorder : raisedBorder);
            ((ImageLayerOptions) getComponent().getParent()).setAutoContrast(autoButton.isSelected());
            Displayer.render(1);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setContrast(contrastSlider.getValue() / sliderToContrastScale);
        contrastLabel.setText(Integer.toString(contrastSlider.getValue()));
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Contrast", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return contrastSlider;
    }

    @Override
    public Component getLabel() {
        //return contrastLabel;
        return buttonPanel;
    }

}
