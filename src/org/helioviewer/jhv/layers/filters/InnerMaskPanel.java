package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.JHVRangeSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.wcs.ImageBounds;

// Double-ended radial mask, normalized to the layer's outer radius: the band between the
// two handles is shown. Low handle at 0 and high handle at 1 mask nothing; the handles
// meeting masks the whole disk and corona. Normalized so resolution is the same fine step
// regardless of the layer's field of view. The R_sun readout uses the layer's outer radius.
public class InnerMaskPanel implements FilterDetails {

    private static final int STEPS = 1000; // 0.001 of the radial extent per tick

    private final JHVRangeSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Mask ", JLabel.RIGHT);
    private final double outerR;

    public InnerMaskPanel(ImageLayer layer) {
        MetaData m = layer.getMetaData();
        outerR = m != null ? ImageBounds.inscribed(m) : 1; // nearest-edge radius; corner/getOuterRadius overshoot
        int low = (int) Math.round(layer.getGLImage().getInnerMask() * STEPS);
        int high = (int) Math.round(layer.getGLImage().getOuterMask() * STEPS);
        slider = new JHVRangeSlider(0, STEPS, low, high);

        label = new JLabel(format(low, high), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setInnerMask(lo / (double) STEPS);
            layer.getGLImage().setOuterMask(hi / (double) STEPS);
            label.setText(format(lo, hi));
            DisplayController.display();
        });
    }

    private String format(int lo, int hi) {
        return "<html><p align='right'>" + String.format("%.2f", lo / (double) STEPS * outerR)
                + "–" + String.format("%.2f", hi / (double) STEPS * outerR) + "R☉</p>";
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
