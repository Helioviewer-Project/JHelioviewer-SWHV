package org.helioviewer.jhv.view.uri;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.function.DoubleConsumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;

@SuppressWarnings("serial")
public final class FITSSettings {

    private FITSSettings() {
    }

    public static final class SettingsDialog extends JDialog implements Interfaces.ShowableDialog, FITSViewState.Listener {

        private final JRadioButton gammaButton = new JRadioButton("\u03B3");
        private final JRadioButton betaButton = new JRadioButton("\u03B2");
        private final JRadioButton alphaButton = new JRadioButton("\u03B1");
        private final JHVSlider gammaSlider;
        private final JLabel gammaLabel = new JLabel("", JLabel.RIGHT);
        private final JHVSlider betaSlider;
        private final JLabel betaLabel = new JLabel("", JLabel.RIGHT);
        private final JHVSlider alphaSlider;
        private final JLabel alphaLabel = new JLabel("", JLabel.RIGHT);
        private final JFormattedTextField minClip = new JFormattedTextField(new TerminatedFormatterFactory("%g", "", -FITSViewState.CLIP_LIMIT, FITSViewState.CLIP_LIMIT));
        private final JFormattedTextField maxClip = new JFormattedTextField(new TerminatedFormatterFactory("%g", "", -FITSViewState.CLIP_LIMIT, FITSViewState.CLIP_LIMIT));
        private final JHVSlider contrastSlider;
        private final EnumMap<FITSViewState.ClippingMode, JRadioButton> clippingButtons = new EnumMap<>(FITSViewState.ClippingMode.class);

        private static JPanel createScalingPanel(JRadioButton button, JHVSlider slider, JLabel label) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
            panel.add(button);
            panel.add(slider);
            panel.add(label);
            return panel;
        }

        private static void bindSelectionControls(JRadioButton button, Component... components) {
            for (Component component : components) {
                component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!button.isSelected()) {
                            button.setSelected(true);
                        }
                    }
                });
            }
        }

        private static void bindScalingMode(JRadioButton button, JHVSlider slider, FITSViewState.ScalingMode mode) {
            bindSelectionControls(button, slider);
            button.addItemListener(e -> {
                if (button.isSelected())
                    FITSViewState.setScalingMode(mode);
            });
        }

        public SettingsDialog() {
            super(JHVFrame.getFrame(), "FITS Settings", false);
            FITSViewState.Data initialState = FITSViewState.data();
            gammaSlider = new JHVSlider(FITSViewState.GAMMA_SLIDER_MIN, FITSViewState.GAMMA_SLIDER_MAX, initialState.gammaIndex());
            betaSlider = new JHVSlider(FITSViewState.BETA_SLIDER_MIN, FITSViewState.BETA_SLIDER_MAX, initialState.betaIndex());
            alphaSlider = new JHVSlider(FITSViewState.ALPHA_SLIDER_MIN, FITSViewState.ALPHA_SLIDER_MAX, initialState.alphaIndex());
            contrastSlider = new JHVSlider(FITSViewState.Z_CONTRAST_SLIDER_MIN, FITSViewState.Z_CONTRAST_SLIDER_MAX, initialState.zContrastIndex());
            setLocationRelativeTo(JHVFrame.getFrame());
            setType(Window.Type.UTILITY);
            setResizable(false);
            FITSViewState.addListener(this);

            gammaButton.setToolTipText("<html><body>pixel<sup>1/\u03B3</sup>");
            betaButton.setToolTipText("<html><body>asinh(pixel / 2<sup>\u03B2</sup>)");
            alphaButton.setToolTipText("<html><body>log1p(10<sup>\u03B1</sup>\u22C5pixel) / log1p(10<sup>\u03B1</sup>)");

            ButtonGroup scalingGroup = new ButtonGroup();
            scalingGroup.add(gammaButton);
            scalingGroup.add(betaButton);
            scalingGroup.add(alphaButton);

            gammaSlider.addChangeListener(e -> {
                int value = gammaSlider.getValue();
                FITSViewState.setGammaIndex(value, gammaSlider.getValueIsAdjusting());
            });
            betaSlider.addChangeListener(e -> {
                int value = betaSlider.getValue();
                FITSViewState.setBetaIndex(value, betaSlider.getValueIsAdjusting());
            });
            alphaSlider.addChangeListener(e -> {
                int value = alphaSlider.getValue();
                FITSViewState.setAlphaIndex(value, alphaSlider.getValueIsAdjusting());
            });

            bindScalingMode(gammaButton, gammaSlider, FITSViewState.ScalingMode.Gamma);
            bindScalingMode(betaButton, betaSlider, FITSViewState.ScalingMode.Beta);
            bindScalingMode(alphaButton, alphaSlider, FITSViewState.ScalingMode.Alpha);

            JPanel gammaPanel = createScalingPanel(gammaButton, gammaSlider, gammaLabel);
            JPanel betaPanel = createScalingPanel(betaButton, betaSlider, betaLabel);
            JPanel alphaPanel = createScalingPanel(alphaButton, alphaSlider, alphaLabel);

            minClip.setColumns(10);
            minClip.addPropertyChangeListener("value", e -> applyClipValue(minClip, FITSViewState::setClippingMin));
            maxClip.setColumns(10);
            maxClip.addPropertyChangeListener("value", e -> applyClipValue(maxClip, FITSViewState::setClippingMax));

            JPanel rangePanel = new JPanel(new BorderLayout());
            rangePanel.add(minClip, BorderLayout.LINE_START);
            rangePanel.add(maxClip, BorderLayout.LINE_END);
            contrastSlider.addChangeListener(e -> FITSViewState.setZContrastIndex(contrastSlider.getValue(), contrastSlider.getValueIsAdjusting()));

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
            for (FITSViewState.ClippingMode clipping : FITSViewState.ClippingMode.values()) {
                JRadioButton radio = new JRadioButton(clipping.toString(), clipping == initialState.clippingMode());
                clippingButtons.put(clipping, radio);
                boolean rangeMode = clipping == FITSViewState.ClippingMode.Range;
                boolean zscaleMode = clipping == FITSViewState.ClippingMode.ZScale;
                if (zscaleMode) {
                    bindSelectionControls(radio, contrastSlider);
                }
                if (rangeMode) {
                    bindSelectionControls(radio, minClip, maxClip);
                }
                radio.addItemListener(e -> {
                    if (radio.isSelected())
                        FITSViewState.setClippingMode(clipping);
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
            fitsViewStateChanged();
            pack(); // hack: fixes first visible layout on Windows at 150% scale
        }

        @Override
        public void fitsViewStateChanged() {
            FITSViewState.Data data = FITSViewState.data();

            gammaButton.setSelected(data.scalingMode() == FITSViewState.ScalingMode.Gamma);
            betaButton.setSelected(data.scalingMode() == FITSViewState.ScalingMode.Beta);
            alphaButton.setSelected(data.scalingMode() == FITSViewState.ScalingMode.Alpha);
            clippingButtons.forEach((mode, button) -> button.setSelected(mode == data.clippingMode()));

            syncControl(gammaSlider, gammaLabel, data.gammaIndex(), String.valueOf(data.gammaDisplayValue()));
            syncControl(betaSlider, betaLabel, data.betaIndex());
            syncControl(alphaSlider, alphaLabel, data.alphaIndex());

            if (differentDoubleValue(minClip.getValue(), data.clippingMin()))
                minClip.setValue(data.clippingMin());
            if (differentDoubleValue(maxClip.getValue(), data.clippingMax()))
                maxClip.setValue(data.clippingMax());

            syncControl(contrastSlider, null, data.zContrastIndex());

            boolean rangeMode = data.clippingMode() == FITSViewState.ClippingMode.Range;
            minClip.setEditable(rangeMode);
            maxClip.setEditable(rangeMode);
        }

        @Override
        public void showDialog() {
            fitsViewStateChanged();
            pack();
            setVisible(true);
        }

        @Override
        public void dispose() {
            FITSViewState.removeListener(this);
            super.dispose();
        }

        private static boolean differentDoubleValue(Object value, double expected) {
            return !(value instanceof Number number) || number.doubleValue() != expected;
        }

        private static void applyClipValue(JFormattedTextField field, DoubleConsumer setter) {
            Object value = field.getValue();
            if (value instanceof Number number)
                setter.accept(number.doubleValue());
        }

        private static void syncControl(JSlider slider, JLabel label, int value) {
            syncControl(slider, label, value, String.valueOf(value));
        }

        private static void syncControl(JSlider slider, JLabel label, int value, String text) {
            if (slider.getValue() != value)
                slider.setValue(value);
            if (label != null)
                label.setText(text);
        }

    }

}
