package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.plugin.Plugin;
import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.gui.EventPanel;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class SWEKPlugin implements Plugin {

    private static final JPanel swekPanel = new JPanel();

    private static final SWEKData swekData = new SWEKData();
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
        Timelines.getLayers().add(etl);
        ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();

        etl.cacheUpdated();
        swekData.cacheUpdated();
        Movie.addTimespanListener(swekData);
        ImageViewerGui.getLayers().add(layer);
    }

    @Override
    public void uninstallPlugin() {
        Timelines.getLayers().remove(etl);
        ImageViewerGui.getLayers().remove(layer);
        Movie.removeTimespanListener(swekData);

        ImageViewerGui.getLeftContentPane().remove(swekPanel);
        ImageViewerGui.getLeftContentPane().revalidate();
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
