package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.RangeSlider;

public class SectorPanel implements FilterDetails {

    private final RangeSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Sector", JLabel.RIGHT);

    public SectorPanel(ImageLayer layer) {
        int left = (int) (layer.getGLImage().getSector0() + .5);
        int right = (int) (layer.getGLImage().getSector1() + .5);

        slider = new RangeSlider(-180, 180, left, right);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    slider.setLowValue(-180);
                    slider.setHighValue(180);
                }
            }
        });
        slider.setRangeDraggable(true);
        label = new JLabel(LevelsPanel.formatDegree(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);

        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setSector(lo, hi);
            label.setText(LevelsPanel.formatDegree(lo, hi));
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(slider);
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return label;
    }

    public void setVisible(boolean visible) {
        title.setVisible(visible);
        slider.setVisible(visible);
        label.setVisible(visible);
    }

}
