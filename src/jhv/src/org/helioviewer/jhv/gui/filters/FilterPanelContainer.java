package org.helioviewer.jhv.gui.filters;

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

        JPanel compactPanel = filterTabPanelManager.createCompactPanel();

        filterPanelContainer = new ControlPanelContainer();
        filterPanelContainer.setDefaultPanel(compactPanel);
    }

    public ControlPanelContainer getFilterPanelContainer() {
        return filterPanelContainer;
    }

    public FilterTabPanelManager getFilterTabPanelManager() {
        return filterTabPanelManager;
    }

}
