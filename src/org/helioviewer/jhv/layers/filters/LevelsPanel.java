package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayer;

import com.jidesoft.swing.RangeSlider;

public class LevelsPanel implements FilterDetails {

    private final RangeSlider slider;
    private final JTextArea label;

    static String align3(int value) {
        if (value < -99)
            return value + "%";
        if (value < -9)
            return "\u2007" + value + '%';
        if (value < 0)
            return "\u2007\u2007" + value + '%';
        if (value < 10)
            return "\u2007\u2007\u2007" + value + '%';
        if (value < 100)
            return "\u2007\u2007" + value + '%';
        return "\u2007" + value + '%';
    }

    static String format(int low, int high) {
        return align3(low) + '\n' + align3(high);
    }

    public LevelsPanel(ImageLayer layer) {
        double offset = layer.getGLImage().getBrightOffset();
        double scale = layer.getGLImage().getBrightScale();
        int high = (int) (100 * (offset + scale));

        slider = new RangeSlider(-101, 201, (int) (offset * 100), high);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    slider.setLowValue(0);
                    slider.setHighValue(100);
                }
            }
        });
        slider.setRangeDraggable(true);

        label = new JTextArea(format(slider.getLowValue(), slider.getHighValue()));
        label.setDragEnabled(false);
        label.setHighlighter(null);
        label.setEditable(false);
        label.setOpaque(false);

        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setBrightness(lo / 100., (hi - lo) / 100.);
            label.setText(format(lo, hi));
            Display.display();
        });
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Levels", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return label;
    }

}
