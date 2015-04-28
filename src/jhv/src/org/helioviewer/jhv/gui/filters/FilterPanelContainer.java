package org.helioviewer.jhv.gui.filters;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.ControlPanelContainer;

public class FilterPanelContainer {

    private static final FilterPanelContainer instance = new FilterPanelContainer();

    public static FilterPanelContainer getSingletonInstance() {
        return instance;
    }

    private final FilterTabPanelManager filterTabPanelManager;
    private final ControlPanelContainer filterPanelContainer;

    private FilterPanelContainer() {
        filterTabPanelManager = new FilterTabPanelManager();
        filterTabPanelManager.add(new OpacityPanel());
        filterTabPanelManager.add(new SOHOLUTPanel());
        filterTabPanelManager.add(new GammaCorrectionPanel());
        filterTabPanelManager.add(new ContrastPanel());
        filterTabPanelManager.add(new SharpenPanel());
        filterTabPanelManager.add(new ChannelMixerPanel());
        RunningDifferencePanel runningDifferencePanel = new RunningDifferencePanel();
        filterTabPanelManager.addAbstractFilterPanel(runningDifferencePanel);

        JPanel compactPanel = filterTabPanelManager.createCompactPanel();
        JPanel tab = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 0);
        gc.weightx = 1;
        gc.weighty = 0.0;

        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        gc.gridx = 0;
        tab.add(runningDifferencePanel, gc);
        gc.gridy = 1;
        gc.weighty = 1.;

        tab.add(compactPanel, gc);
        tab.setEnabled(true);

        filterPanelContainer = new ControlPanelContainer();
        filterPanelContainer.setDefaultPanel(tab);
    }

    public ControlPanelContainer getFilterPanelContainer() {
        return filterPanelContainer;
    }

    public FilterTabPanelManager getFilterTabPanelManager() {
        return filterTabPanelManager;
    }

}
