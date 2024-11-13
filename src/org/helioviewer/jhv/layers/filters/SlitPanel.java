package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVRangeSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

public class SlitPanel implements FilterDetails {

    private final JHVRangeSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Slit ", JLabel.RIGHT);

    public SlitPanel(ImageLayer layer) {
        int left = (int) (layer.getGLImage().getSlitLeft() * 100);
        int right = (int) (layer.getGLImage().getSlitRight() * 100);

        slider = new JHVRangeSlider(0, 100, left, right);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    slider.setLowValue(0);
                    slider.setHighValue(100);
                }
            }
        });

        label = new JLabel(LevelsPanel.formatPercent(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setSlit(lo / 100., hi / 100.);
            label.setText(LevelsPanel.formatPercent(lo, hi));
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
