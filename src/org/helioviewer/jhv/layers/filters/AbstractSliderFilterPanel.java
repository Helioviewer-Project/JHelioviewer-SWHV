package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.MovieDisplay;

abstract class AbstractSliderFilterPanel implements FilterDetails {

    private final JLabel title;
    private final JHVSlider slider;
    private final JLabel label;

    protected AbstractSliderFilterPanel(
            String titleText,
            int min,
            int max,
            int initial,
            IntFunction<String> formatter,
            IntConsumer onValueChange) {
        title = new JLabel(titleText, JLabel.RIGHT);
        slider = new JHVSlider(min, max, initial);
        label = new JLabel(formatter.apply(initial), JLabel.RIGHT);

        slider.addChangeListener(e -> {
            int value = slider.getValue();
            onValueChange.accept(value);
            label.setText(formatter.apply(value));
            MovieDisplay.display();
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
