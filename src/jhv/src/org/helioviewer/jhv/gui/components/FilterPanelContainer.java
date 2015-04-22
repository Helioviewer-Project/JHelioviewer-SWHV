package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.helioviewer.jhv.gui.filters.ChannelMixerPanel;
import org.helioviewer.jhv.gui.filters.ContrastPanel;
import org.helioviewer.jhv.gui.filters.GammaCorrectionPanel;
import org.helioviewer.jhv.gui.filters.OpacityPanel;
import org.helioviewer.jhv.gui.filters.RunningDifferencePanel;
import org.helioviewer.jhv.gui.filters.SOHOLUTPanel;
import org.helioviewer.jhv.gui.filters.SharpenPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager;

public class FilterPanelContainer {

    private static final FilterPanelContainer instance = new FilterPanelContainer();

    public static FilterPanelContainer getSingletonInstance() {
        return instance;
    }

    private FilterTabPanelManager filterTabPanelManager;
    private ControlPanelContainer filterPanelContainer;

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
        JPanel tab = new JPanel(new BorderLayout());
        tab.add(runningDifferencePanel, BorderLayout.NORTH);
        tab.add(compactPanel, BorderLayout.CENTER);
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
