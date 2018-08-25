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

public class SplitPanel implements FilterDetails {

    private final RangeSlider slider;
    private final JTextArea label;

    public SplitPanel(ImageLayer layer) {
        int left = (int) (layer.getGLImage().getSplitLeft() * 100);
        int right = (int) (layer.getGLImage().getSplitRight() * 100);

        slider = new RangeSlider(0, 100, left, right);
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

        label = new JTextArea(LevelsPanel.format(slider.getLowValue(), slider.getHighValue()));
        label.setDragEnabled(false);
        label.setHighlighter(null);
        label.setEditable(false);
        label.setOpaque(false);

        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setSplit(lo / 100., hi / 100.);
            label.setText(LevelsPanel.format(lo, hi));
            Display.display();
        });
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Split", JLabel.RIGHT);
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
