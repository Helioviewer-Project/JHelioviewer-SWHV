package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.events.EventPanel;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class SWEKPlugin extends Plugin {

    private static final JPanel swekPanel = new JPanel();

    private static final SWEKLayer layer = new SWEKLayer(null);
    private static final EventTimelineLayer etl = new EventTimelineLayer(null);

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");
        swekPanel.setLayout(new BoxLayout(swekPanel, BoxLayout.PAGE_AXIS));
        SWEKConfig.load().forEach(group -> swekPanel.add(new EventPanel(group)));
    }

    @Override
    public void install() {
        JHVFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        JHVFrame.getLeftContentPane().revalidate();
        JHVFrame.getLayers().add(layer);
        Timelines.getLayers().add(etl);
    }

    @Override
    public void uninstall() {
        JHVFrame.getLeftContentPane().remove(swekPanel);
        JHVFrame.getLeftContentPane().revalidate();
        JHVFrame.getLayers().remove(layer);
        Timelines.getLayers().remove(etl);
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

}
