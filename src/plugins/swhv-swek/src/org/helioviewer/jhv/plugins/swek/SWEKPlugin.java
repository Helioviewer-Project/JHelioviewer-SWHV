package org.helioviewer.jhv.plugins.swek;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.data.cache.JHVEventCache;
import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKData;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKRenderable;
import org.helioviewer.jhv.plugins.swek.view.EventTimelineRenderable;
import org.helioviewer.jhv.plugins.swek.view.EventPanel;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineTableModel;

public class SWEKPlugin implements Plugin {

    private static final JPanel swekPanel = new JPanel();
    private static final SWEKRenderable renderable = new SWEKRenderable();

    public static final SWEKDownloadManager downloadManager = new SWEKDownloadManager();
    public static final SWEKData swekData = new SWEKData();

    private static final EventTimelineRenderable em = new EventTimelineRenderable();

    public SWEKPlugin() {
        swekPanel.setLayout(new BoxLayout(swekPanel, BoxLayout.Y_AXIS));
        for (SWEKEventType eventType : SWEKConfigurationManager.loadConfiguration()) {
            swekPanel.add(new EventPanel(eventType));
        }
        JHVEventCache.registerHandler(downloadManager);
    }

    @Override
    public void installPlugin() {
        TimelineTableModel.addLineData(em);
        ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();

        SWEKData.requestEvents(true);
        Layers.addTimespanListener(swekData);
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        // TimelineTableModel.removeLineData(em);
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
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getAboutLicenseText() {
        return "Mozilla Public License Version 2.0";
    }

}
