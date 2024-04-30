package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVRangeSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

public class SectorPanel implements FilterDetails {

    private final JHVRangeSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Sector", JLabel.RIGHT);

    public SectorPanel(ImageLayer layer) {
        int left = 0; // (int) (layer.getGLImage().getSector0() + .5);
        int right = 0; // (int) (layer.getGLImage().getSector1() + .5);

        slider = new JHVRangeSlider(-180, 180, left, right);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    slider.setLowValue(-180);
                    slider.setHighValue(180);
                }
            }
        });

        label = new JLabel(LevelsPanel.formatDegree(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            // layer.getGLImage().setSector(lo, hi);
            label.setText(LevelsPanel.formatDegree(lo, hi));
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
