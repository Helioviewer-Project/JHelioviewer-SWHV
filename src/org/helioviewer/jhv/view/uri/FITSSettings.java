package org.helioviewer.jhv.view.uri;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public class FITSSettings {

    enum ConversionMode {
        Gamma, Beta
    }

    private static void refresh() {
        URIView.clearURICache();
        MovieDisplay.render(1);
    }

    static ConversionMode conversionMode = ConversionMode.Gamma;
    static double GAMMA = 1. / 2.2;
    static double BETA = 1. / (1 << 5);

    public static final class SettingsDialog extends JDialog implements ShowableDialog {

        public SettingsDialog() {
            super(JHVFrame.getFrame(), "FITS Settings", false);
            setType(Window.Type.UTILITY);
            setResizable(false);

            JRadioButton gammaButton = new JRadioButton("Gamma");
            gammaButton.setSelected(true);
            JRadioButton betaButton = new JRadioButton("Beta");
            betaButton.setSelected(false);

            ButtonGroup functionGroup = new ButtonGroup();
            functionGroup.add(gammaButton);
            functionGroup.add(betaButton);

            int gammaDefault = (int) (10. / GAMMA);
            int betaDefault = (int) (Math.log(1 / BETA) / Math.log(2));
            JHVSlider gammaSlider = new JHVSlider(10, 40, gammaDefault);
            JLabel gammaLabel = new JLabel(String.valueOf(gammaDefault / 10.), JLabel.RIGHT);
            JHVSlider betaSlider = new JHVSlider(1, 10, betaDefault);
            JLabel betaLabel = new JLabel(String.valueOf(betaDefault), JLabel.RIGHT);

            gammaSlider.addChangeListener(e -> {
                int value = gammaSlider.getValue();
                GAMMA = 10. / value;
                gammaLabel.setText(String.valueOf(value / 10.));
                if (gammaButton.isSelected())
                    refresh();
            });
            betaSlider.addChangeListener(e -> {
                int value = betaSlider.getValue();
                BETA = 1. / (1 << value);
                betaLabel.setText(String.valueOf(value));
                if (betaButton.isSelected())
                    refresh();
            });

            gammaButton.addItemListener(e -> {
                if (gammaButton.isSelected()) {
                    conversionMode = ConversionMode.Gamma;
                    refresh();
                }
            });
            betaButton.addItemListener(e -> {
                if (betaButton.isSelected()) {
                    conversionMode = ConversionMode.Beta;
                    refresh();
                }
            });

            JPanel content = new JPanel();
            content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            content.setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridy = 0;
            c.gridx = 0;
            content.add(gammaButton, c);
            c.gridx = 1;
            content.add(gammaSlider, c);
            c.gridx = 2;
            content.add(gammaLabel, c);
            c.gridy = 1;
            c.gridx = 0;
            content.add(betaButton, c);
            c.gridx = 1;
            content.add(betaSlider, c);
            c.gridx = 2;
            content.add(betaLabel, c);

            add(content);
        }

        @Override
        public void showDialog() {
            pack();
            setLocationRelativeTo(JHVFrame.getFrame());
            setVisible(true);
        }

    }

}
