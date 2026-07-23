package org.helioviewer.jhv.layers.selector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.filters.ChannelMixerPanel;
import org.helioviewer.jhv.layers.filters.DifferencePanel;
import org.helioviewer.jhv.layers.filters.FilterDetails;
import org.helioviewer.jhv.layers.filters.ImageFilterPanel;
import org.helioviewer.jhv.layers.filters.LUTPanel;
import org.helioviewer.jhv.layers.filters.LevelsPanel;
import org.helioviewer.jhv.layers.filters.SliderFilterPanel;

// Rendering controls for the selected image layer: difference, opacity, blend, sharpen,
// levels, colormap (LUT), channels, filter. Shown in the "Layer options" wrapper.
@SuppressWarnings("serial")
final class ImageLayerRenderingPanel extends JPanel {

    private final LUTPanel lutPanel;

    ImageLayerRenderingPanel(ImageLayer layer) {
        DifferencePanel differencePanel = new DifferencePanel(layer);
        FilterDetails opacityPanel = new SliderFilterPanel.Opacity(layer);
        FilterDetails blendPanel = new SliderFilterPanel.Blend(layer);
        FilterDetails channelMixerPanel = new ChannelMixerPanel(layer);
        lutPanel = new LUTPanel(layer);
        FilterDetails levelsPanel = new LevelsPanel(layer);
        FilterDetails sharpenPanel = new SliderFilterPanel.Sharpen(layer);
        FilterDetails imageFilterPanel = new ImageFilterPanel(layer);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;

        c.gridy = 0;
        FilterRowLayout.addFilterRow(this, c, differencePanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, opacityPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, blendPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, sharpenPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, levelsPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, lutPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, channelMixerPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, imageFilterPanel);

        // Usually refreshed through ImageLayer activation; initialize here too in case that activation already happened before panel creation.
        refresh(layer);
    }

    void refresh(Layer layer) {
        lutPanel.setLUT(((ImageLayer) layer).getView().getDefaultLUT());
    }

}
