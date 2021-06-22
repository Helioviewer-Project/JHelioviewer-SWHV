package org.helioviewer.jhv.layers;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.layers.filters.*;

@SuppressWarnings("serial")
class ImageLayerOptions extends JPanel {

    private final LUTPanel lutPanel;
    private final RunningDifferencePanel runningDifferencePanel;

    ImageLayerOptions(ImageLayer layer) {
        runningDifferencePanel = new RunningDifferencePanel(layer);
        OpacityPanel opacityPanel = new OpacityPanel(layer);
        BlendPanel blendPanel = new BlendPanel(layer);
        ChannelMixerPanel channelMixerPanel = new ChannelMixerPanel(layer);
        lutPanel = new LUTPanel(layer);
        SlitPanel slitPanel = new SlitPanel(layer);
        LevelsPanel levelsPanel = new LevelsPanel(layer);
        SharpenPanel sharpenPanel = new SharpenPanel(layer);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

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
        addToGridBag(c, slitPanel);
        c.gridy++;
        addToGridBag(c, sharpenPanel);
        c.gridy++;
        addToGridBag(c, levelsPanel);
        c.gridy++;
        addToGridBag(c, lutPanel);
        c.gridy++;
        addToGridBag(c, channelMixerPanel);
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

    RunningDifferencePanel getRunningDifferencePanel() {
        return runningDifferencePanel;
    }

}
