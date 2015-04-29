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
public class FilterTabPanelManager {

    private final RunningDifferencePanel runningDifferencePanel = new RunningDifferencePanel();
    final OpacityPanel opacityPanel = new OpacityPanel();
    final ChannelMixerPanel channelMixerPanel = new ChannelMixerPanel();
    final SOHOLUTPanel lUTPanel = new SOHOLUTPanel();
    final GammaCorrectionPanel gammaCorrectionPanel = new GammaCorrectionPanel();
    final ContrastPanel contrastPanel = new ContrastPanel();
    final SharpenPanel sharpenPanel = new SharpenPanel();

    public enum Area {
        TOP, CENTER, BOTTOM
    }

    private void addToGridBag(GridBagConstraints c, JPanel compactPanel, FilterAlignmentDetails details) {
        c.gridwidth = 1;

        c.gridx = 0;
        c.weightx = 0.1;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.LINE_START;

        //((JLabel) details.getTitle()).setHorizontalAlignment(SwingConstants.LEFT);
        compactPanel.add(details.getTitle(), c);
        c.gridx = 1;
        c.weightx = 1.;
        c.anchor = GridBagConstraints.CENTER;

        compactPanel.add(details.getSlider(), c);
        c.gridx = 2;
        c.weightx = 0.1;
        c.anchor = GridBagConstraints.LINE_END;
        compactPanel.add(details.getValue(), c);

    }

    private void addToGridBag2(GridBagConstraints c, JPanel compactPanel, FilterAlignmentDetails details) {
        c.gridwidth = 1;
        c.gridx = 0;
        c.weightx = 0.1;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.LINE_START;
        compactPanel.add(details.getTitle(), c);
        c.gridx = 1;
        c.weightx = 1.1;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 2;
        compactPanel.add(details.getSlider(), c);
    }

    /**
     * Setup a new CompactPanel based on all Components added to the center
     * list, that implement the FilterAlignmentDetails interface
     *
     * @see org.helioviewer.viewmodelplugin.filter# FilterAlignmentDetails
     * @return A JPanel containing all suitable components added to the center
     *         list
     */
    public JPanel createCompactPanel() {

        JPanel compactPanel = new JPanel() {
            /**
             * Override the setEnabled method in order to keep the containing
             * components' enabledState synced with the enabledState of this
             * component.
             */
            @Override
            public void setEnabled(boolean enabled) {
                for (Component c : this.getComponents()) {
                    c.setEnabled(enabled);
                }
            }
        };

        compactPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 1;

        c.gridx = 0;

        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridwidth = 3;
        compactPanel.add(runningDifferencePanel.getPanel(), c);
        c.gridwidth = 1;
        c.gridy++;
        this.addToGridBag(c, compactPanel, opacityPanel);
        c.gridy++;
        this.addToGridBag(c, compactPanel, sharpenPanel);
        c.gridy++;
        this.addToGridBag(c, compactPanel, gammaCorrectionPanel);
        c.gridy++;
        this.addToGridBag(c, compactPanel, contrastPanel);
        c.gridy++;
        this.addToGridBag(c, compactPanel, lUTPanel);
        c.gridy++;
        this.addToGridBag2(c, compactPanel, channelMixerPanel);
        c.gridy++;
        return compactPanel;
    }

    public void setActivejp2(final AbstractView jp2view) {
        runningDifferencePanel.setEnabled(true);
        runningDifferencePanel.setJP2View(jp2view);
        opacityPanel.setEnabled(true);
        opacityPanel.setJP2View(jp2view);
        channelMixerPanel.setEnabled(true);
        channelMixerPanel.setJP2View(jp2view);
        lUTPanel.setEnabled(true);
        lUTPanel.setJP2View(jp2view);
        gammaCorrectionPanel.setEnabled(true);
        gammaCorrectionPanel.setJP2View(jp2view);
        contrastPanel.setEnabled(true);
        contrastPanel.setJP2View(jp2view);
        sharpenPanel.setEnabled(true);
        sharpenPanel.setJP2View(jp2view);
        GridBagConstraints c = new GridBagConstraints();

    }

}
