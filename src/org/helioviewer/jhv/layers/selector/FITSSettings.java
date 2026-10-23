package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.function.DoubleConsumer;

import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.gui.component.SliderValueLabel;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;
import org.helioviewer.jhv.image.ImageProcessingSettings;

@SuppressWarnings("serial")
public final class FITSSettings extends JPanel implements ImageProcessingSettings.FITSListener {

    private final ImageProcessingSettings state;
    private boolean syncing;
    private final JRadioButton gammaButton = new JRadioButton("γ");
    private final JRadioButton betaButton = new JRadioButton("β");
    private final JRadioButton alphaButton = new JRadioButton("α");
    private final JHVSlider gammaSlider;
    private final JLabel gammaLabel;
    private final JHVSlider betaSlider;
    private final JLabel betaLabel;
    private final JHVSlider alphaSlider;
    private final JLabel alphaLabel;
    private final JFormattedTextField minClip = new JFormattedTextField(new TerminatedFormatterFactory("%g", "", -ImageProcessingSettings.CLIP_LIMIT, ImageProcessingSettings.CLIP_LIMIT));
    private final JFormattedTextField maxClip = new JFormattedTextField(new TerminatedFormatterFactory("%g", "", -ImageProcessingSettings.CLIP_LIMIT, ImageProcessingSettings.CLIP_LIMIT));
    private final EnumMap<ImageProcessingSettings.ClippingMode, JRadioButton> clippingButtons = new EnumMap<>(ImageProcessingSettings.ClippingMode.class);

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

    private void bindScalingMode(JRadioButton button, JHVSlider slider, ImageProcessingSettings.ScalingMode mode) {
        bindSelectionControls(button, slider);
        button.addItemListener(e -> {
            if (!syncing && button.isSelected())
                state.setScalingMode(mode);
        });
    }

    public FITSSettings(ImageProcessingSettings _state) {
        super(new GridBagLayout());
        state = _state;
        ImageProcessingSettings.FITSParameters initialState = state.fitsParameters();
        gammaSlider = createSlider(gammaIndex(ImageProcessingSettings.GAMMA_MAX),
                gammaIndex(ImageProcessingSettings.GAMMA_MIN), gammaIndex(initialState.gamma()));
        betaSlider = createSlider(betaIndex(ImageProcessingSettings.BETA_MAX),
                betaIndex(ImageProcessingSettings.BETA_MIN), betaIndex(initialState.beta()));
        alphaSlider = createSlider(alphaIndex(ImageProcessingSettings.ALPHA_MIN),
                alphaIndex(ImageProcessingSettings.ALPHA_MAX), alphaIndex(initialState.alpha()));
        gammaLabel = new SliderValueLabel(String.valueOf(gammaSlider.getValue() / 10.), gammaSlider, step -> String.valueOf(step / 10.));
        betaLabel = new SliderValueLabel(String.valueOf(betaSlider.getValue()), betaSlider, String::valueOf);
        alphaLabel = new SliderValueLabel(String.valueOf(alphaSlider.getValue()), alphaSlider, String::valueOf);
        state.addFITSListener(this);

        gammaButton.setToolTipText("<html><body>pixel<sup>1/γ</sup>");
        betaButton.setToolTipText("<html><body>asinh(pixel / 2<sup>β</sup>)");
        alphaButton.setToolTipText("<html><body>log1p(10<sup>α</sup>⋅pixel) / log1p(10<sup>α</sup>)");

        ButtonGroup scalingGroup = new ButtonGroup();
        scalingGroup.add(gammaButton);
        scalingGroup.add(betaButton);
        scalingGroup.add(alphaButton);

        gammaSlider.addChangeListener(e -> {
            int value = gammaSlider.getValue();
            if (!syncing)
                state.setGamma(10. / value);
        });
        betaSlider.addChangeListener(e -> {
            int value = betaSlider.getValue();
            if (!syncing)
                state.setBeta(1. / (1 << value));
        });
        alphaSlider.addChangeListener(e -> {
            int value = alphaSlider.getValue();
            if (!syncing)
                state.setAlpha(Math.pow(10, value));
        });

        bindScalingMode(gammaButton, gammaSlider, ImageProcessingSettings.ScalingMode.Gamma);
        bindScalingMode(betaButton, betaSlider, ImageProcessingSettings.ScalingMode.Beta);
        bindScalingMode(alphaButton, alphaSlider, ImageProcessingSettings.ScalingMode.Alpha);

        minClip.setColumns(5);
        minClip.addPropertyChangeListener("value", e -> applyClipValue(minClip, state::setClippingMin));
        maxClip.setColumns(5);
        maxClip.addPropertyChangeListener("value", e -> applyClipValue(maxClip, state::setClippingMax));

        JPanel rangePanel = new JPanel(new BorderLayout());
        rangePanel.add(minClip, BorderLayout.LINE_START);
        rangePanel.add(maxClip, BorderLayout.LINE_END);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel("Clipping:", JLabel.RIGHT), c);

        ButtonGroup clippingGroup = new ButtonGroup();
        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        for (ImageProcessingSettings.ClippingMode clipping : ImageProcessingSettings.ClippingMode.values()) {
            JRadioButton radio = new JRadioButton(clipping.toString(), clipping == initialState.clippingMode());
            clippingButtons.put(clipping, radio);
            boolean rangeMode = clipping == ImageProcessingSettings.ClippingMode.Range;
            if (rangeMode) {
                bindSelectionControls(radio, minClip, maxClip);
            }
            radio.addItemListener(e -> {
                if (!syncing && radio.isSelected())
                    state.setClippingMode(clipping);
            });
            clippingGroup.add(radio);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
            panel.add(radio);
            if (rangeMode) {
                panel.add(rangePanel);
            }
            add(panel, c);
            c.gridy++;
        }

        c.gridwidth = 1;
        addScalingRow(c, gammaButton, gammaSlider, gammaLabel);
        addScalingRow(c, betaButton, betaSlider, betaLabel);
        addScalingRow(c, alphaButton, alphaSlider, alphaLabel);

        fitsParametersChanged();
    }

    private void addScalingRow(GridBagConstraints c, JRadioButton button, JHVSlider slider, JLabel label) {
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        button.setHorizontalAlignment(JRadioButton.RIGHT);
        add(button, c);
        c.gridx = 1;
        c.weightx = 1;
        add(slider, c);
        c.gridx = 2;
        c.weightx = 0;
        add(label, c);
        c.gridy++;
    }

    @Override
    public void fitsParametersChanged() {
        syncing = true;
        try {
            ImageProcessingSettings.FITSParameters data = state.fitsParameters();

            gammaButton.setSelected(data.scalingMode() == ImageProcessingSettings.ScalingMode.Gamma);
            betaButton.setSelected(data.scalingMode() == ImageProcessingSettings.ScalingMode.Beta);
            alphaButton.setSelected(data.scalingMode() == ImageProcessingSettings.ScalingMode.Alpha);
            clippingButtons.forEach((mode, button) -> button.setSelected(mode == data.clippingMode()));

            gammaSlider.setValue(gammaIndex(data.gamma()));
            betaSlider.setValue(betaIndex(data.beta()));
            alphaSlider.setValue(alphaIndex(data.alpha()));
            gammaLabel.setText(String.valueOf(gammaSlider.getValue() / 10.));
            betaLabel.setText(String.valueOf(betaSlider.getValue()));
            alphaLabel.setText(String.valueOf(alphaSlider.getValue()));

            if (differentDoubleValue(minClip.getValue(), data.clippingMin()))
                minClip.setValue(data.clippingMin());
            if (differentDoubleValue(maxClip.getValue(), data.clippingMax()))
                maxClip.setValue(data.clippingMax());

            boolean rangeMode = data.clippingMode() == ImageProcessingSettings.ClippingMode.Range;
            minClip.setEditable(rangeMode);
            maxClip.setEditable(rangeMode);
        } finally {
            syncing = false;
        }
    }

    private static boolean differentDoubleValue(Object value, double expected) {
        return !(value instanceof Number number) || number.doubleValue() != expected;
    }

    private void applyClipValue(JFormattedTextField field, DoubleConsumer setter) {
        Object value = field.getValue();
        if (!syncing && value instanceof Number number)
            setter.accept(number.doubleValue());
    }

    private static int gammaIndex(double value) {
        return (int) Math.round(10. / value);
    }

    private static int betaIndex(double value) {
        return (int) Math.round(Math.log(1 / value) / Math.log(2));
    }

    private static int alphaIndex(double value) {
        return (int) Math.round(Math.log10(value));
    }

    private static JHVSlider createSlider(int min, int max, int value) {
        JHVSlider slider = new JHVSlider(min, max, Math.clamp(value, min, max));
        slider.setPreferredSize(new Dimension(100, slider.getPreferredSize().height));
        return slider;
    }

}
