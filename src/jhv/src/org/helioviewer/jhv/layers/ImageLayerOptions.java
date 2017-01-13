package org.helioviewer.jhv.layers;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.layers.filters.*;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class ImageLayerOptions extends SmallPanel {

    private final ImageLayer imageLayer;
    private final OpacityPanel opacityPanel;
    private final LUTPanel lutPanel;

    public ImageLayerOptions(ImageLayer _imageLayer) {
        imageLayer = _imageLayer;

        RunningDifferencePanel runningDifferencePanel = new RunningDifferencePanel();
        opacityPanel = new OpacityPanel();
        ChannelMixerPanel channelMixerPanel = new ChannelMixerPanel();
        lutPanel = new LUTPanel();
        ContrastPanel contrastPanel = new ContrastPanel();
        BrightnessPanel brightnessPanel = new BrightnessPanel();
        SharpenPanel sharpenPanel = new SharpenPanel();

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;

        c.gridwidth = 3;
        c.gridy = 0;
        add(runningDifferencePanel.getComponent(), c);
        c.gridwidth = 1;
        c.gridy++;
        addToGridBag(c, opacityPanel);
        c.gridy++;
        addToGridBag(c, sharpenPanel);
        c.gridy++;
        addToGridBag(c, contrastPanel);
        c.gridy++;
        addToGridBag(c, brightnessPanel);
        c.gridy++;
        addToGridBag(c, lutPanel);
        c.gridy++;
        addToGridBag(c, channelMixerPanel);

        setSmall();
    }

    private void addToGridBag(GridBagConstraints c, FilterDetails details) {
        c.gridwidth = 1;

        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        add(details.getTitle(), c);

        c.gridx = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(details.getComponent(), c);

        c.gridx = 2;
        c.weightx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        add(details.getLabel(), c);
    }

    void setOpacity(float opacity) {
        opacityPanel.setValue(opacity);
    }

    void setLUT(LUT lut) {
        lutPanel.setLUT(lut);
    }

    public GLImage getGLImage() {
        return imageLayer.getGLImage();
    }

    public View getView() {
        return imageLayer.getView();
    }

    public void setAutoBrightness(boolean b) {
        imageLayer.setAutoBrightness(b);
    }

}
