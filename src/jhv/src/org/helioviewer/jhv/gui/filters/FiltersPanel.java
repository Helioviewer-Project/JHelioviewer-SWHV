package org.helioviewer.jhv.gui.filters;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

import org.helioviewer.viewmodel.view.AbstractView;

/**
 * This manager collects all filter control components and creates a panel where
 * all components will occur at the defined area.
 * <p>
 * There are three different areas: Top, Center, Bottom. Within the areas the
 * components will be occur below each other as they were added before.
 *
 * @author Stephan Pagel
 */
public class FiltersPanel extends JPanel {

    private static RunningDifferencePanel runningDifferencePanel;
    private static OpacityPanel opacityPanel;
    private static ChannelMixerPanel channelMixerPanel;
    private static SOHOLUTPanel lutPanel;
    private static GammaCorrectionPanel gammaCorrectionPanel;
    private static ContrastPanel contrastPanel;
    private static SharpenPanel sharpenPanel;

    @Override
    public void setEnabled(boolean enabled) {
        for (Component c : this.getComponents()) {
            c.setEnabled(enabled);
        }
    }

    public FiltersPanel() {
        runningDifferencePanel = new RunningDifferencePanel();
        opacityPanel = new OpacityPanel();
        channelMixerPanel = new ChannelMixerPanel();
        lutPanel = new SOHOLUTPanel();
        gammaCorrectionPanel = new GammaCorrectionPanel();
        contrastPanel = new ContrastPanel();
        sharpenPanel = new SharpenPanel();

        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 1;

        c.gridx = 0;

        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridwidth = 3;
        this.add(runningDifferencePanel.getPanel(), c);
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
        add(details.getSlider(), c);

        c.gridx = 2;
        c.weightx = 0.;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        add(details.getValue(), c);
    }

    public void setActivejp2(final AbstractView jp2view) {
        runningDifferencePanel.setEnabled(true);
        runningDifferencePanel.setJP2View(jp2view);
        opacityPanel.setEnabled(true);
        opacityPanel.setJP2View(jp2view);
        channelMixerPanel.setEnabled(true);
        channelMixerPanel.setJP2View(jp2view);
        lutPanel.setEnabled(true);
        lutPanel.setJP2View(jp2view);
        gammaCorrectionPanel.setEnabled(true);
        gammaCorrectionPanel.setJP2View(jp2view);
        contrastPanel.setEnabled(true);
        contrastPanel.setJP2View(jp2view);
        sharpenPanel.setEnabled(true);
        sharpenPanel.setJP2View(jp2view);
    }

}
