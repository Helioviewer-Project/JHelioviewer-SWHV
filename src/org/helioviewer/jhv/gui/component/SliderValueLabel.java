package org.helioviewer.jhv.gui.component;

import java.awt.Dimension;
import java.util.function.IntFunction;

import javax.swing.JLabel;
import javax.swing.JSlider;

// Reserve the widest readout over the slider's complete discrete range.
// A newline displays two right-aligned values for range sliders.
@SuppressWarnings("serial")
public final class SliderValueLabel extends JLabel {

    public SliderValueLabel(String text, JSlider slider, IntFunction<String> formatter) {
        super("", JLabel.RIGHT);
        JLabel measure = new JLabel();
        measure.setFont(getFont());
        int width = 0;
        for (int step = slider.getMinimum(); step <= slider.getMaximum(); step++) {
            for (String line : formatter.apply(step).split("\n")) {
                measure.setText(line);
                width = Math.max(width, measure.getPreferredSize().width);
            }
        }
        setText(text);
        Dimension size = new Dimension(width, getPreferredSize().height);
        setPreferredSize(size);
        setMinimumSize(size);
    }

    @Override
    public void setText(String text) {
        super.setText(text != null && text.contains("\n")
                ? "<html><p align='right'>" + text.replace("\n", "</p><p align='right'>") + "</p>"
                : text);
    }

}
