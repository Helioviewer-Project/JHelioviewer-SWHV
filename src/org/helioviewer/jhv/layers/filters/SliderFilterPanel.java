package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import javax.swing.JLabel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.GLImage;

public class SliderFilterPanel {

    public static class Blend extends AbstractSliderFilterPanel {
        public Blend(ImageLayer layer) {
            super("Blend ",
                    0, 100, (int) (layer.getGLImage().getBlend() * 100),
                    LevelsPanel::formatPercent,
                    value -> layer.getGLImage().setBlend(value / 100.));
        }
    }

    public static class DeltaCROTA extends AbstractSliderFilterPanel {
        public DeltaCROTA(ImageLayer layer) {
            super("δCROTA",
                    GLImage.MIN_DCROTA * 10, GLImage.MAX_DCROTA * 10, (int) (layer.getGLImage().getDeltaCROTA() * 10),
                    value -> formatDegree(value / 10.0),
                    value -> layer.getGLImage().setDeltaCROTA(value / 10.0));
        }
    }

    public static class DeltaCRVAL1 extends AbstractSliderFilterPanel {
        public DeltaCRVAL1(ImageLayer layer) {
            super("δCRVAL1",
                    GLImage.MIN_DCRVAL, GLImage.MAX_DCRVAL, layer.getGLImage().getDeltaCRVAL1(),
                    SliderFilterPanel::formatArcsec,
                    layer.getGLImage()::setDeltaCRVAL1);
        }
    }

    public static class DeltaCRVAL2 extends AbstractSliderFilterPanel {
        public DeltaCRVAL2(ImageLayer layer) {
            super("δCRVAL2",
                    GLImage.MIN_DCRVAL, GLImage.MAX_DCRVAL, layer.getGLImage().getDeltaCRVAL2(),
                    SliderFilterPanel::formatArcsec,
                    layer.getGLImage()::setDeltaCRVAL2);
        }
    }

    public static class Opacity extends AbstractSliderFilterPanel {
        public Opacity(ImageLayer layer) {
            super("Opacity ",
                    0, 100, (int) (layer.getGLImage().getOpacity() * 100),
                    LevelsPanel::formatPercent,
                    value -> layer.getGLImage().setOpacity(value / 100.));
        }
    }

    public static class Sharpen extends AbstractSliderFilterPanel {
        public Sharpen(ImageLayer layer) {
            super("Sharpen ",
                    -100, 100, (int) (layer.getGLImage().getSharpen() * 100),
                    LevelsPanel::formatPercent,
                    value -> layer.getGLImage().setSharpen(value / 100.));
        }
    }

    private static String formatDegree(double value) {
        return "<html><p align='right'>" + String.format("%.1f", value) + "°</p>";
    }

    private static String formatArcsec(int value) {
        return "<html><p align='right'>" + value + "″</p>";
    }

    private static abstract class AbstractSliderFilterPanel implements FilterDetails {

        private final JLabel title;
        private final JHVSlider slider;
        private final JLabel label;

        protected AbstractSliderFilterPanel(
                String titleText,
                int min, int max, int initial,
                IntFunction<String> formatter,
                IntConsumer onValueChange) {
            title = new JLabel(titleText, JLabel.RIGHT);
            slider = new JHVSlider(min, max, initial);
            label = new JLabel(formatter.apply(initial), JLabel.RIGHT);

            slider.addChangeListener(e -> {
                int value = slider.getValue();
                onValueChange.accept(value);
                label.setText(formatter.apply(value));
                DisplayController.display();
            });
        }

        @Override
        public Component getFirst() {
            return title;
        }

        @Override
        public Component getSecond() {
            return slider;
        }

        @Override
        public Component getThird() {
            return label;
        }

        public void setVisible(boolean visible) {
            title.setVisible(visible);
            slider.setVisible(visible);
            label.setVisible(visible);
        }
    }

}
