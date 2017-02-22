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
import org.helioviewer.jhv.plugins.swek.view.EventPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SWEKPlugin implements Plugin {

    private static final JPanel swekPanel = new JPanel();
    private static final SWEKRenderable renderable = new SWEKRenderable();

    public static final SWEKDownloadManager downloadManager = new SWEKDownloadManager();
    public static final SWEKData swekData = new SWEKData();

    public SWEKPlugin() {
        swekPanel.setLayout(new BoxLayout(swekPanel, BoxLayout.Y_AXIS));
        for (SWEKEventType eventType : SWEKConfigurationManager.loadConfiguration()) {
            swekPanel.add(new EventPanel(eventType));
        }
        JHVEventCache.registerHandler(downloadManager);
    }

    @Override
    public void installPlugin() {
        ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        ImageViewerGui.getLeftContentPane().revalidate();

        SWEKData.requestEvents(true);
        Layers.addTimespanListener(swekData);
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeTimespanListener(swekData);

        ImageViewerGui.getLeftContentPane().remove(swekPanel);
        ImageViewerGui.getLeftContentPane().revalidate();
    }

    @NotNull
    @Override
    public String getName() {
        return "Space Weather Event Knowledgebase Plugin";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "This plugin visualizes space weather relevant events";
    }

    @Override
    public void setState(String state) {
    }

    @Nullable
    @Override
    public String getState() {
        return null;
    }

    @NotNull
    @Override
    public String getAboutLicenseText() {
        return "Mozilla Public License Version 2.0";
    }

}
