package org.helioviewer.jhv.layers;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.layers.filters.*;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.view.View;

@SuppressWarnings("serial")
public class ImageLayerOptions extends JPanel {

    private final ImageLayer imageLayer;
    private final LUTPanel lutPanel;

    public ImageLayerOptions(ImageLayer _imageLayer) {
        imageLayer = _imageLayer;

        RunningDifferencePanel runningDifferencePanel = new RunningDifferencePanel(this);
        OpacityPanel opacityPanel = new OpacityPanel(this);
        BlendPanel blendPanel = new BlendPanel(this);
        ChannelMixerPanel channelMixerPanel = new ChannelMixerPanel(this);
        lutPanel = new LUTPanel(this);
        LevelsPanel levelsPanel = new LevelsPanel(this);
        SharpenPanel sharpenPanel = new SharpenPanel(this);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;

        c.gridy = 0;
        addToGridBag(c, runningDifferencePanel);
        c.gridy++;
        addToGridBag(c, opacityPanel);
        c.gridy++;
        addToGridBag(c, blendPanel);
        c.gridy++;
        addToGridBag(c, sharpenPanel);
        c.gridy++;
        addToGridBag(c, levelsPanel);
        c.gridy++;
        addToGridBag(c, lutPanel);
        c.gridy++;
        addToGridBag(c, channelMixerPanel);

        ComponentUtils.smallVariant(this);
        levelsPanel.syncFont(); // JideButton does not respect variant small
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

    void setLUT(LUT lut) {
        lutPanel.setLUT(lut);
    }

    public GLImage getGLImage() {
        return imageLayer.getGLImage();
    }

    public View getView() {
        return imageLayer.getView();
    }

    public double getAutoBrightness() {
        return imageLayer.getAutoBrightness();
    }

}
