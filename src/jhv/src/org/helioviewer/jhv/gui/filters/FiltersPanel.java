package org.helioviewer.jhv.gui.filters;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

import org.helioviewer.viewmodel.view.AbstractView;

@SuppressWarnings("serial")
public class FiltersPanel extends JPanel {

    private final RunningDifferencePanel runningDifferencePanel;
    private final OpacityPanel opacityPanel;
    private final ChannelMixerPanel channelMixerPanel;
    private final SOHOLUTPanel lutPanel;
    private final GammaCorrectionPanel gammaCorrectionPanel;
    private final ContrastPanel contrastPanel;
    private final SharpenPanel sharpenPanel;

    public FiltersPanel() {
        runningDifferencePanel = new RunningDifferencePanel();
        opacityPanel = new OpacityPanel();
        channelMixerPanel = new ChannelMixerPanel();
        lutPanel = new SOHOLUTPanel();
        gammaCorrectionPanel = new GammaCorrectionPanel();
        contrastPanel = new ContrastPanel();
        sharpenPanel = new SharpenPanel();

        setLayout(new GridBagLayout());

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

    public void setActivejp2(AbstractView jp2view) {
        runningDifferencePanel.setJP2View(jp2view);
        opacityPanel.setJP2View(jp2view);
        channelMixerPanel.setJP2View(jp2view);
        lutPanel.setJP2View(jp2view);
        gammaCorrectionPanel.setJP2View(jp2view);
        contrastPanel.setJP2View(jp2view);
        sharpenPanel.setJP2View(jp2view);
    }

}
