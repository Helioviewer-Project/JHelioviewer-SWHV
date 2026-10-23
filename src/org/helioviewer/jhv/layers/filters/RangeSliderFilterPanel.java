package org.helioviewer.jhv.layers.filters;

import javax.swing.JLabel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.JHVRangeSlider;
import org.helioviewer.jhv.gui.component.SliderValueLabel;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.layers.ImageLayer;

public final class RangeSliderFilterPanel {

    private RangeSliderFilterPanel() {
    }

    public static FilterDetails levels(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        double offset = settings.getBrightOffset();
        double scale = settings.getBrightScale();
        return create("Levels ", -1, 2, offset, offset + scale, 100,
                RangeSliderFilterPanel::formatPercent,
                (low, high) -> settings.setBrightness(low, high - low));
    }

    public static FilterDetails mask(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        double maximum = ImageDisplaySettings.MAX_MASK; // top of the range means unbounded
        double outer = Double.isFinite(settings.getOuterMask()) ? settings.getOuterMask() : maximum;
        return create("Mask ", 0, maximum, settings.getInnerMask(), outer, 100,
                (low, high) -> formatMask(low, high, maximum),
                (low, high) -> settings.setMask(low, high == maximum ? Double.POSITIVE_INFINITY : high));
    }

    public static FilterDetails slit(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        return create("Slit ", 0, 1, settings.getSlitLeft(), settings.getSlitRight(), 100,
                RangeSliderFilterPanel::formatPercent,
                settings::setSlit);
    }

    // values change in steps of 1/scale
    private static FilterDetails create(
            String titleText,
            double min, double max, double initialLow, double initialHigh, int scale,
            RangeFormatter formatter,
            RangeConsumer onValueChange) {
        JLabel title = new JLabel(titleText, JLabel.RIGHT);
        JHVRangeSlider slider = new JHVRangeSlider(min, max, initialLow, initialHigh, scale);
        // Both readout lines depend on one endpoint independently, so equal endpoints
        // cover every displayed value without enumerating pairs.
        JLabel label = new SliderValueLabel(formatter.format(slider.getLowDouble(), slider.getHighDouble()), slider,
                step -> formatter.format(step / (double) scale, step / (double) scale));
        slider.addChangeListener(e -> {
            double low = slider.getLowDouble();
            double high = slider.getHighDouble();
            onValueChange.accept(low, high);
            label.setText(formatter.format(low, high));
            DisplayController.display();
        });
        return new FilterRow(title, slider, label);
    }

    private static String formatPercent(double low, double high) {
        return String.format("%.0f%%\n%.0f%%", low * 100, high * 100);
    }

    private static String formatMask(double low, double high, double maximum) {
        String outer = high == maximum ? "∞" : String.format("%.2f", high);
        return String.format("%.2fR☉\n%sR☉", low, outer);
    }

    @FunctionalInterface
    private interface RangeConsumer {
        void accept(double low, double high);
    }

    @FunctionalInterface
    private interface RangeFormatter {
        String format(double low, double high);
    }

}
