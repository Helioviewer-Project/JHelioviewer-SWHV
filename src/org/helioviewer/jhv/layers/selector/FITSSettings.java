package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.function.DoubleConsumer;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.gui.component.SliderValueLabel;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.layers.Frames;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.source.FITSSource;

@SuppressWarnings("serial")
public final class FITSSettings extends JPanel implements ImageProcessingSettings.FITSListener {

    private final ImageLayer layer;
    private final ImageProcessingSettings state;
    private boolean syncing;
    private @Nullable ImageProcessingSettings.FITSSelection selection;
    private final JLabel hduLabel = new JLabel("HDU", JLabel.RIGHT);
    private final JComboBox<FITSSource.Image> hduChoice = new JComboBox<>();
    private final JSpinner planeChoice = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private List<FITSSource.Image> images = List.of();
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

    public FITSSettings(ImageLayer _layer) {
        super(new GridBagLayout());
        layer = _layer;
        state = layer.getProcessingSettings();
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

        JPanel rangePanel = new JPanel(new GridLayout(1, 2, 6, 0));
        rangePanel.add(minClip);
        rangePanel.add(maxClip);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        add(hduLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        add(hduChoice, c);
        c.gridx = 2;
        c.weightx = 0;
        add(planeChoice, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        hduLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        hduLabel.setLabelFor(hduChoice);
        hduChoice.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof FITSSource.Image image) {
                    String name = image.name();
                    label.setText(image.hdu() + " · " + (name == null || name.isBlank() ? "Image" : name.trim()));
                }
                return label;
            }
        });
        hduChoice.setPrototypeDisplayValue(new FITSSource.Image(0, "Image HDU", 1));
        hduChoice.setToolTipText("Image HDU");
        planeChoice.putClientProperty("JComponent.minimumWidth", 0);
        ((JSpinner.DefaultEditor) planeChoice.getEditor()).getTextField().setColumns(2);
        hduChoice.addActionListener(e -> {
            if (!syncing && hduChoice.getSelectedItem() instanceof FITSSource.Image image
                    && selection != null && image.hdu() != selection.hdu()) {
                layer.selectFITS(new ImageProcessingSettings.FITSSelection(image.hdu(), 0));
            }
        });
        planeChoice.addChangeListener(e -> {
            if (!syncing && hduChoice.getSelectedItem() instanceof FITSSource.Image image) {
                layer.selectFITS(new ImageProcessingSettings.FITSSelection(image.hdu(), ((Number) planeChoice.getValue()).intValue() - 1));
            }
        });
        JLabel clippingLabel = new JLabel("Clipping", JLabel.RIGHT);
        clippingLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        add(clippingLabel, c);

        ButtonGroup clippingGroup = new ButtonGroup();
        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        for (ImageProcessingSettings.ClippingMode clipping : ImageProcessingSettings.ClippingMode.values()) {
            String label = switch (clipping) {
                case Percentile001 -> "Percentile 0.001%";
                case Percentile05 -> "Percentile 0.5%";
                case Range -> "Range";
            };
            JRadioButton radio = new JRadioButton(label, clipping == initialState.clippingMode());
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

            if (rangeMode) {
                JPanel panel = new JPanel(new BorderLayout(6, 0));
                panel.add(radio, BorderLayout.LINE_START);
                panel.add(rangePanel, BorderLayout.CENTER);
                add(panel, c);
            } else
                add(radio, c);
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
            FITSSource source = null;
            Frames frames = layer.frames();
            for (int i = 0; i < frames.size(); i++) {
                if (frames.valueAt(i).source() instanceof FITSSource fits) {
                    source = fits;
                    break;
                }
            }
            selection = source == null ? null : source.selection();
            List<FITSSource.Image> available = source == null ? List.of() : source.images();
            if (!images.equals(available)) {
                images = available;
                hduChoice.removeAllItems();
                for (FITSSource.Image image : images)
                    hduChoice.addItem(image);
            }
            int planes = 1;
            if (selection != null) {
                for (FITSSource.Image image : images) {
                    if (image.hdu() == selection.hdu()) {
                        hduChoice.setSelectedItem(image);
                        planes = image.planes();
                        break;
                    }
                }
                SpinnerNumberModel planeModel = (SpinnerNumberModel) planeChoice.getModel();
                planeModel.setMaximum(planes);
                planeModel.setValue(selection.plane() + 1);
            }
            hduLabel.setVisible(images.size() > 1);
            hduChoice.setVisible(images.size() > 1);
            String planeName = source == null ? null : source.planeName();
            String planeTooltip = "Plane " + planeChoice.getValue() + " of " + planes
                    + (planeName == null || planeName.isBlank() ? "" : ": " + planeName.trim());
            planeChoice.setToolTipText(planeTooltip);
            ((JSpinner.DefaultEditor) planeChoice.getEditor()).getTextField().setToolTipText(planeTooltip);
            planeChoice.setVisible(planes > 1);
            hduChoice.setEnabled(!layer.isDownloading());
            planeChoice.setEnabled(!layer.isDownloading());
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
