package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.plugin.Plugin;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.gui.EventPanel;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class SWEKPlugin implements Plugin {

    private static final JPanel swekPanel = new JPanel();

    private static final SWEKLayer layer = new SWEKLayer(null);
    private static final EventTimelineLayer etl = new EventTimelineLayer(null);

    public SWEKPlugin() {
        swekPanel.setLayout(new BoxLayout(swekPanel, BoxLayout.Y_AXIS));
        for (SWEKGroup group : SWEKConfig.load()) {
            swekPanel.add(new EventPanel(group));
        }
    }

    @Override
    public void installPlugin() {
        ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();
        ImageViewerGui.getLayers().add(layer);
        Timelines.getLayers().add(etl);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getLeftContentPane().remove(swekPanel);
        ImageViewerGui.getLeftContentPane().revalidate();
        ImageViewerGui.getLayers().remove(layer);
        Timelines.getLayers().remove(etl);
    }

    @Override
    public String getName() {
        return "Space Weather Event Knowledgebase Plugin";
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes space weather relevant events";
    }

    @Override
    public void saveState(JSONObject jo) {
        for (Component c : swekPanel.getComponents()) {
            if (c instanceof EventPanel) {
                ((EventPanel) c).serialize(jo);
            }
        }
    }

    @Override
    public void loadState(JSONObject jo) {
        for (Component c : swekPanel.getComponents()) {
            if (c instanceof EventPanel) {
                ((EventPanel) c).deserialize(jo);
            }
        }
    }

    @Override
    public String getAboutLicenseText() {
        return "Mozilla Public License Version 2.0";
    }

}
