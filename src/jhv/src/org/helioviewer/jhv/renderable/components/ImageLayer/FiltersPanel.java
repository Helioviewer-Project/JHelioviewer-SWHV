package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.layers.RenderableImageLayer;

@SuppressWarnings("serial")
public class FiltersPanel extends SmallPanel {

    private final OpacityPanel opacityPanel;

    final RenderableImageLayer imageLayer;

    public FiltersPanel(RenderableImageLayer imageLayer, float opacity, LUT lut) {
        this.imageLayer = imageLayer;

        RunningDifferencePanel runningDifferencePanel = new RunningDifferencePanel();
        opacityPanel = new OpacityPanel(opacity);
        ChannelMixerPanel channelMixerPanel = new ChannelMixerPanel();
        LUTPanel lutPanel = new LUTPanel(lut);
        GammaCorrectionPanel gammaCorrectionPanel = new GammaCorrectionPanel();
        ContrastPanel contrastPanel = new ContrastPanel();
        SharpenPanel sharpenPanel = new SharpenPanel();

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;

        c.gridx = 0;

        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridwidth = 3;
        this.add(runningDifferencePanel.getComponent(), c);
        c.gridwidth = 1;
        c.gridy++;
        this.addToGridBag(c, opacityPanel);
        c.gridy++;
        this.addToGridBag(c, sharpenPanel);
        c.gridy++;
        this.addToGridBag(c, gammaCorrectionPanel);
        c.gridy++;
        this.addToGridBag(c, contrastPanel);
        c.gridy++;
        this.addToGridBag(c, lutPanel);
        c.gridy++;
        this.addToGridBag(c, channelMixerPanel);
        c.gridy++;

        setSmall();
    }

    private void addToGridBag(GridBagConstraints c, FilterDetails details) {
        c.gridwidth = 1;

        c.gridx = 0;
        c.weightx = 0.;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        add(details.getTitle(), c);

        c.gridx = 1;
        c.weightx = 1.;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(details.getComponent(), c);

        c.gridx = 2;
        c.weightx = 0.;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        add(details.getLabel(), c);
    }

    public void setOpacity(float opacity) {
        opacityPanel.setValue(opacity);
    }

}
