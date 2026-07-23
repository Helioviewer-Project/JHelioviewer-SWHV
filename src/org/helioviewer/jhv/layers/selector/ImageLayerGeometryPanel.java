package org.helioviewer.jhv.layers.selector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.filters.InnerMaskPanel;
import org.helioviewer.jhv.layers.filters.SliderFilterPanel;
import org.helioviewer.jhv.layers.filters.SlitPanel;

// Geometry/crop controls for the selected image layer: slit, inner mask, delta CROTA/CRVAL.
// Shown in the "Geometry / crop" wrapper. All rows are always visible (no toggle).
@SuppressWarnings("serial")
final class ImageLayerGeometryPanel extends JPanel {

    ImageLayerGeometryPanel(ImageLayer layer) {
        SlitPanel slitPanel = new SlitPanel(layer);
        InnerMaskPanel innerMaskPanel = new InnerMaskPanel(layer);
        SliderFilterPanel.DeltaCROTA deltaCROTAPanel = new SliderFilterPanel.DeltaCROTA(layer);
        SliderFilterPanel.DeltaCRVAL1 deltaCRVAL1Panel = new SliderFilterPanel.DeltaCRVAL1(layer);
        SliderFilterPanel.DeltaCRVAL2 deltaCRVAL2Panel = new SliderFilterPanel.DeltaCRVAL2(layer);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;

        c.gridy = 0;
        FilterRowLayout.addFilterRow(this, c, slitPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, innerMaskPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, deltaCROTAPanel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, deltaCRVAL1Panel);
        c.gridy++;
        FilterRowLayout.addFilterRow(this, c, deltaCRVAL2Panel);
    }

}
