package org.helioviewer.jhv.layers.filters;

import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import javax.swing.JLabel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.GLImage;

public final class SliderFilterPanel {

    private SliderFilterPanel() {
    }

    public static FilterDetails blend(ImageLayer layer) {
        return create("Blend ", 0, 100, (int) (layer.getGLImage().getBlend() * 100),
                SliderFilterPanel::formatPercent, value -> layer.getGLImage().setBlend(value / 100.));
    }

    public static FilterDetails deltaCROTA(ImageLayer layer) {
        return create("δCROTA", GLImage.MIN_DCROTA * 10, GLImage.MAX_DCROTA * 10, (int) (layer.getGLImage().getDeltaCROTA() * 10),
                value -> formatDegree(value / 10.0), value -> layer.getGLImage().setDeltaCROTA(value / 10.0));
    }

    public static FilterDetails deltaCRVAL1(ImageLayer layer) {
        return create("δCRVAL1", GLImage.MIN_DCRVAL, GLImage.MAX_DCRVAL, layer.getGLImage().getDeltaCRVAL1(),
                SliderFilterPanel::formatArcsec, layer.getGLImage()::setDeltaCRVAL1);
    }

    public static FilterDetails deltaCRVAL2(ImageLayer layer) {
        return create("δCRVAL2", GLImage.MIN_DCRVAL, GLImage.MAX_DCRVAL, layer.getGLImage().getDeltaCRVAL2(),
                SliderFilterPanel::formatArcsec, layer.getGLImage()::setDeltaCRVAL2);
    }

    public static FilterDetails opacity(ImageLayer layer) {
        return opacity(layer.getGLImage().getOpacity(), layer.getGLImage()::setOpacity);
    }

    public static FilterDetails opacity(double initialValue, DoubleConsumer setter) {
        return create("Opacity ", 0, 100, (int) (initialValue * 100),
                SliderFilterPanel::formatPercent, value -> setter.accept(value / 100.));
    }

    public static FilterDetails sharpen(ImageLayer layer) {
        return create("Sharpen ", -100, 100, (int) (layer.getGLImage().getSharpen() * 100),
                SliderFilterPanel::formatPercent, value -> layer.getGLImage().setSharpen(value / 100.));
    }

    private static String formatDegree(double value) {
        return "<html><p align='right'>" + String.format("%.1f", value) + "°</p>";
    }

    private static String formatArcsec(int value) {
        return "<html><p align='right'>" + value + "″</p>";
    }

    private static String formatPercent(int value) {
        return "<html><p align='right'>" + value + "%</p>";
    }

    static FilterDetails create(
            String titleText,
            int min, int max, int initial,
            IntFunction<String> formatter,
            IntConsumer onValueChange) {
        JLabel title = new JLabel(titleText, JLabel.RIGHT);
        JHVSlider slider = new JHVSlider(min, max, initial);
        JLabel label = new JLabel(formatter.apply(initial), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int value = slider.getValue();
            onValueChange.accept(value);
            label.setText(formatter.apply(value));
            DisplayController.display();
        });
        return new FilterRow(title, slider, label);
    }

}
