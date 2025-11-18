package org.helioviewer.jhv.view.uri;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public class FITSSettings {

    enum ClippingMode {
        Auto, ZScale, Range
    }

    enum ScalingMode {
        Gamma, Beta, Alpha
    }

    private static void refresh() {
        URIView.clearURICache();
        MovieDisplay.render(1);
    }

    static int zContrast = 4;
    static double clippingMin = -500;
    static double clippingMax = 500;
    static ClippingMode clippingMode = ClippingMode.Auto;

    static ScalingMode scalingMode = ScalingMode.Gamma;
    static double GAMMA = 1. / 2.2;
    static double BETA = 1. / (1 << 6);
    static double ALPHA = Math.pow(10, 3);

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

            ButtonGroup scalingGroup = new ButtonGroup();
            scalingGroup.add(gammaButton);
            scalingGroup.add(betaButton);
            scalingGroup.add(alphaButton);

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
                    scalingMode = ScalingMode.Gamma;
                    refresh();
                }
            });
            betaButton.addItemListener(e -> {
                if (betaButton.isSelected()) {
                    scalingMode = ScalingMode.Beta;
                    refresh();
                }
            });
            alphaButton.addItemListener(e -> {
                if (alphaButton.isSelected()) {
                    scalingMode = ScalingMode.Alpha;
                    refresh();
                }
            });

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

            JHVSlider contrastSlider = new JHVSlider(1, 100, zContrast / 4);
            contrastSlider.addChangeListener(e -> {
                zContrast = 4 * contrastSlider.getValue();
                refresh();
            });

            JFormattedTextField minClip = new JFormattedTextField(new TerminatedFormatterFactory("%g", "", -1e12, 1e12));
            minClip.setValue(clippingMin);
            minClip.setColumns(10);
            minClip.addPropertyChangeListener("value", e -> {
                clippingMin = (Double) minClip.getValue();
                refresh();
            });
            JFormattedTextField maxClip = new JFormattedTextField(new TerminatedFormatterFactory("%g", "", -1e12, 1e12));
            maxClip.setValue(clippingMax);
            maxClip.setColumns(10);
            maxClip.addPropertyChangeListener("value", e -> {
                clippingMax = (Double) maxClip.getValue();
                refresh();
            });

            JPanel rangePanel = new JPanel(new BorderLayout());
            rangePanel.add(minClip, BorderLayout.LINE_START);
            rangePanel.add(maxClip, BorderLayout.LINE_END);
            ComponentUtils.setEnabled(rangePanel, false);

            //
            JPanel content = new JPanel(new GridBagLayout());
            content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;
            content.add(new JLabel("Clipping:", JLabel.RIGHT), c);

            ButtonGroup clippingGroup = new ButtonGroup();
            c.gridx = 1;
            for (ClippingMode clipping : ClippingMode.values()) {
                JRadioButton radio = new JRadioButton(clipping.toString(), clipping == clippingMode);
                boolean rangeMode = clipping == ClippingMode.Range;
                boolean zscaleMode = clipping == ClippingMode.ZScale;
                radio.addItemListener(e -> {
                    if (radio.isSelected()) {
                        clippingMode = clipping;
                        ComponentUtils.setEnabled(rangePanel, rangeMode);
                        ComponentUtils.setEnabled(contrastSlider, zscaleMode);
                        refresh();
                    }
                });
                clippingGroup.add(radio);

                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
                panel.add(radio, c);
                if (rangeMode) {
                    panel.add(rangePanel);
                }
                if (zscaleMode) {
                    panel.add(contrastSlider);
                }
                content.add(panel, c);
                c.gridy++;
            }

            c.gridx = 0;
            content.add(new JLabel("Scaling:", JLabel.RIGHT), c);

            c.gridx = 1;
            content.add(gammaPanel, c);
            c.gridy++;
            content.add(betaPanel, c);
            c.gridy++;
            content.add(alphaPanel, c);
            c.gridy++;

            add(content);
        }

        @Override
        public void showDialog() {
            pack();
            setVisible(true);
        }

    }

}
