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
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;

@SuppressWarnings("serial")
public final class FITSSettings {

    public static final class SettingsDialog extends JDialog implements Interfaces.ShowableDialog, FITSViewState.Listener {

        private final JRadioButton gammaButton = new JRadioButton("γ");
        private final JRadioButton betaButton = new JRadioButton("β");
        private final JRadioButton alphaButton = new JRadioButton("α");
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
            super(MainFrame.get(), "FITS Settings", false);
            FITSViewState.Data initialState = FITSViewState.data();
            gammaSlider = createSlider(FITSViewState.GAMMA, initialState.gammaIndex());
            betaSlider = createSlider(FITSViewState.BETA, initialState.betaIndex());
            alphaSlider = createSlider(FITSViewState.ALPHA, initialState.alphaIndex());
            contrastSlider = createSlider(FITSViewState.Z_CONTRAST, initialState.zContrastIndex());
            setLocationRelativeTo(MainFrame.get());
            setType(Window.Type.UTILITY);
            setResizable(false);
            FITSViewState.addListener(this);

            gammaButton.setToolTipText("<html><body>pixel<sup>1/γ</sup>");
            betaButton.setToolTipText("<html><body>asinh(pixel / 2<sup>β</sup>)");
            alphaButton.setToolTipText("<html><body>log1p(10<sup>α</sup>⋅pixel) / log1p(10<sup>α</sup>)");

            ButtonGroup scalingGroup = new ButtonGroup();
            scalingGroup.add(gammaButton);
            scalingGroup.add(betaButton);
            scalingGroup.add(alphaButton);

            gammaSlider.addChangeListener(e -> {
                int value = gammaSlider.getValue();
                FITSViewState.setGammaIndex(value);
            });
            betaSlider.addChangeListener(e -> {
                int value = betaSlider.getValue();
                FITSViewState.setBetaIndex(value);
            });
            alphaSlider.addChangeListener(e -> {
                int value = alphaSlider.getValue();
                FITSViewState.setAlphaIndex(value);
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
            contrastSlider.addChangeListener(e -> FITSViewState.setZContrastIndex(contrastSlider.getValue()));

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

        private static JHVSlider createSlider(FITSViewState.IndexedParameter parameter, int value) {
            return new JHVSlider(parameter.minIndex(), parameter.maxIndex(), value);
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

    private FITSSettings() {}
}
