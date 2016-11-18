package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKData;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKRenderable;
import org.helioviewer.jhv.plugins.swek.view.SWEKPluginPanel;

public class SWEKPlugin implements Plugin {

    private static final SWEKRenderable renderable = new SWEKRenderable();
    private static SWEKPluginPanel swekPanel;

    public static final SWEKDownloadManager swekDM = new SWEKDownloadManager();
    public static final SWEKData swekData = new SWEKData();

    public SWEKPlugin() {
        swekPanel = new SWEKPluginPanel(SWEKConfigurationManager.loadConfiguration());
        JHVEventCache.registerHandler(swekDM);
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

    @Override
    public String getName() {
        return "Space Weather Event Knowledgebase " + "$Rev$";
    }

    @Override
    public String getDescription() {
        return "Space Weather Event Knowledgebase";
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
        return "<p>The plugin uses the <a href=\"https://github.com/stleary/JSON-java\">JSON in Java</a> Library, licensed under a custom <a href=\"http://www.json.org/license.html\">License</a>.";
    }

}
