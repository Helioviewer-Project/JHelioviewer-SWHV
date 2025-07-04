package org.helioviewer.jhv.view.uri;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public class FITSSettings {

    enum ConversionMode {
        Gamma, Beta, Alpha
    }

    private static void refresh() {
        URIView.clearURICache();
        MovieDisplay.render(1);
    }

    static ConversionMode conversionMode = ConversionMode.Gamma;
    static double GAMMA = 1. / 2.2;
    static double BETA = 1. / (1 << 6);
    static double ALPHA = Math.pow(10, 3);
    static boolean ZScale = false;

    public static final class SettingsDialog extends JDialog implements Interfaces.ShowableDialog {

        public SettingsDialog() {
            super(JHVFrame.getFrame(), "FITS Settings", false);
            setLocationRelativeTo(JHVFrame.getFrame());
            setType(Window.Type.UTILITY);
            setResizable(false);

            JRadioButton gammaButton = new JRadioButton("\u03B3");
            gammaButton.setToolTipText("<html><body>pixel<sup>1/\u03B3</sup>");
            gammaButton.setSelected(true);
            JRadioButton betaButton = new JRadioButton("\u03B2");
            betaButton.setToolTipText("<html><body>asinh(pixel / 2<sup>\u03B2</sup>)");
            betaButton.setSelected(false);
            JRadioButton alphaButton = new JRadioButton("\u03B1");
            alphaButton.setToolTipText("<html><body>log1p(10<sup>\u03B1</sup>\u22C5pixel) / log1p(10<sup>\u03B1</sup>)");
            alphaButton.setSelected(false);

            ButtonGroup functionGroup = new ButtonGroup();
            functionGroup.add(gammaButton);
            functionGroup.add(betaButton);
            functionGroup.add(alphaButton);

            int gammaDefault = (int) (10. / GAMMA);
            int betaDefault = (int) (Math.log(1 / BETA) / Math.log(2));
            int alphaDefault = (int) Math.log10(ALPHA);
            JHVSlider gammaSlider = new JHVSlider(10, 40, gammaDefault);
            JLabel gammaLabel = new JLabel(String.valueOf(gammaDefault / 10.), JLabel.RIGHT);
            JHVSlider betaSlider = new JHVSlider(1, 12, betaDefault);
            JLabel betaLabel = new JLabel(String.valueOf(betaDefault), JLabel.RIGHT);
            JHVSlider alphaSlider = new JHVSlider(1, 5, alphaDefault);
            JLabel alphaLabel = new JLabel(String.valueOf(alphaDefault), JLabel.RIGHT);

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
            alphaSlider.addChangeListener(e -> {
                int value = alphaSlider.getValue();
                ALPHA = Math.pow(10, value);
                alphaLabel.setText(String.valueOf(value));
                if (alphaButton.isSelected())
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
            alphaButton.addItemListener(e -> {
                if (alphaButton.isSelected()) {
                    conversionMode = ConversionMode.Alpha;
                    refresh();
                }
            });

            JCheckBox scaleCheck = new JCheckBox("ZScale clipping", false);
            scaleCheck.addActionListener(e -> {
                ZScale = scaleCheck.isSelected();
                refresh();
            });

            JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
            scalePanel.add(scaleCheck);

            JPanel gammaPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
            gammaPanel.add(gammaButton);
            gammaPanel.add(gammaSlider);
            gammaPanel.add(gammaLabel);
            JPanel betaPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
            betaPanel.add(betaButton);
            betaPanel.add(betaSlider);
            betaPanel.add(betaLabel);
            JPanel alphaPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
            alphaPanel.add(alphaButton);
            alphaPanel.add(alphaSlider);
            alphaPanel.add(alphaLabel);

            JPanel radios = new JPanel(new BorderLayout());
            radios.add(gammaPanel, BorderLayout.NORTH);
            radios.add(betaPanel, BorderLayout.CENTER);
            radios.add(alphaPanel, BorderLayout.SOUTH);

            JPanel content = new JPanel(new BorderLayout());
            content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            content.add(scalePanel, BorderLayout.NORTH);
            content.add(radios, BorderLayout.CENTER);
            add(content);
        }

        @Override
        public void showDialog() {
            pack();
            setVisible(true);
        }

    }

}
