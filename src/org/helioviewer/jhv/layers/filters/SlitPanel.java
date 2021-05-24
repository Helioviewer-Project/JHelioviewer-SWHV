package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.RangeSlider;

public class SlitPanel implements FilterDetails {

    private final RangeSlider slider;
    private final JLabel label;

    public SlitPanel(ImageLayer layer) {
        int left = (int) (layer.getGLImage().getSlitLeft() * 100);
        int right = (int) (layer.getGLImage().getSlitRight() * 100);

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

        label = new JLabel(LevelsPanel.format(slider.getLowValue(), slider.getHighValue()));

        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setSlit(lo / 100., hi / 100.);
            label.setText(LevelsPanel.format(lo, hi));
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Slit", JLabel.RIGHT);
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
