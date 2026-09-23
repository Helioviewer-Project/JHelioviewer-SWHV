package org.helioviewer.jhv.layers.filters;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

import javax.swing.JLabel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.layers.ImageLayer;

public final class SliderFilterPanel {

    private SliderFilterPanel() {
    }

    public static FilterDetails blend(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        return create("Blend ", 0, 1, settings.getBlend(), 100, SliderFilterPanel::formatPercent, settings::setBlend);
    }

    public static FilterDetails deltaCROTA(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        return create("δCROTA", ImageDisplaySettings.MIN_DCROTA, ImageDisplaySettings.MAX_DCROTA, settings.getDeltaCROTA(), 10,
                SliderFilterPanel::formatDegree, settings::setDeltaCROTA);
    }

    public static FilterDetails deltaCRVAL1(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        return create("δCRVAL1", ImageDisplaySettings.MIN_DCRVAL, ImageDisplaySettings.MAX_DCRVAL, settings.getDeltaCRVAL1(), 1,
                SliderFilterPanel::formatArcsec, settings::setDeltaCRVAL1);
    }

    public static FilterDetails deltaCRVAL2(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        return create("δCRVAL2", ImageDisplaySettings.MIN_DCRVAL, ImageDisplaySettings.MAX_DCRVAL, settings.getDeltaCRVAL2(), 1,
                SliderFilterPanel::formatArcsec, settings::setDeltaCRVAL2);
    }

    public static FilterDetails opacity(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        return create("Opacity ", 0, 1, settings.getOpacity(), 100, SliderFilterPanel::formatPercent, settings::setOpacity);
    }

    public static FilterDetails sharpen(ImageLayer layer) {
        ImageDisplaySettings settings = layer.getDisplaySettings();
        return create("Sharpen ", -1, 1, settings.getSharpen(), 100, SliderFilterPanel::formatPercent, settings::setSharpen);
    }

    private static String formatDegree(double value) {
        return "<html><p align='right'>" + String.format("%.1f", value) + "°</p>";
    }

    private static String formatArcsec(double value) {
        return "<html><p align='right'>" + String.format("%.0f", value) + "″</p>";
    }

    private static String formatPercent(double value) {
        return "<html><p align='right'>" + String.format("%.0f", value * 100) + "%</p>";
    }

    // value changes in steps of 1/scale
    static FilterDetails create(
            String titleText,
            double min, double max, double initial, int scale,
            DoubleFunction<String> formatter,
            DoubleConsumer onValueChange) {
        JLabel title = new JLabel(titleText, JLabel.RIGHT);
        JHVSlider slider = new JHVSlider(min, max, initial, scale);
        JLabel label = new JLabel(formatter.apply(slider.getDoubleValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            double value = slider.getDoubleValue();
            onValueChange.accept(value);
            label.setText(formatter.apply(value));
            DisplayController.display();
        });
        return new FilterRow(title, slider, label);
    }

}
