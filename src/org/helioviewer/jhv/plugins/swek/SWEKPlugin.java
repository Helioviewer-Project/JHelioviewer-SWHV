package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.plugin.Plugin;
import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.gui.EventPanel;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.renderable.EventTimelineRenderable;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKData;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKRenderable;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class SWEKPlugin implements Plugin {

    private static final JPanel swekPanel = new JPanel();

    private static final SWEKData swekData = new SWEKData();
    private static final SWEKRenderable renderable = new SWEKRenderable(null);
    private static final EventTimelineRenderable em = new EventTimelineRenderable();

    public SWEKPlugin() {
        swekPanel.setLayout(new BoxLayout(swekPanel, BoxLayout.Y_AXIS));
        for (SWEKGroup group : SWEKConfigurationManager.loadConfig()) {
            swekPanel.add(new EventPanel(group));
        }
    }

    @Override
    public void installPlugin() {
        Timelines.getModel().addLineData(em);
        ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();

        em.cacheUpdated();
        swekData.cacheUpdated();
        Layers.addTimespanListener(swekData);
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        Timelines.getModel().removeLineData(em);
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeTimespanListener(swekData);

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
