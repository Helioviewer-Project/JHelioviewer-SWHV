package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKData;
import org.helioviewer.jhv.plugins.swek.renderable.SWEKRenderable;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManager;
import org.helioviewer.jhv.plugins.swek.view.SWEKPluginPanel;
import org.helioviewer.jhv.threads.JHVWorker;

public class SWEKPlugin implements Plugin {

    /** Instance of the SWEKConfiguration */
    private final SWEKConfigurationManager SWEKConfig;

    /** the incoming request manager */
    private final IncomingRequestManager incomingRequestManager;

    /** instance of the event container */
    private final JHVEventCache eventCache;

    private final SWEKRenderable renderable;

    public SWEKPlugin() {
        SWEKConfig = SWEKConfigurationManager.getSingletonInstance();
        incomingRequestManager = IncomingRequestManager.getSingletonInstance();
        eventCache = JHVEventCache.getSingletonInstance();
        renderable = new SWEKRenderable();
    }

    @Override
    public void installPlugin() {
        JHVWorker<Void, Void> loadPlugin = new JHVWorker<Void, Void>() {

            @Override
            protected Void backgroundWork() {
                SWEKConfig.loadConfiguration();
                return null;
            }

            @Override
            protected void done() {
                eventCache.registerHandler(incomingRequestManager);
                ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", SWEKPluginPanel.getSWEKPluginPanelInstance(), true);
                ImageViewerGui.getLeftContentPane().revalidate();

                SWEKData.getSingletonInstance().requestEvents(true);
                Layers.addTimespanListener(SWEKData.getSingletonInstance());
                ImageViewerGui.getRenderableContainer().addRenderable(renderable);
            }

        };
        loadPlugin.setThreadName("SWEK--LoadPlugin");
        JHVGlobals.getExecutorService().execute(loadPlugin);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeTimespanListener(SWEKData.getSingletonInstance());
        SWEKData.getSingletonInstance().reset();

        ImageViewerGui.getLeftContentPane().remove(SWEKPluginPanel.getSWEKPluginPanelInstance());
        ImageViewerGui.getLeftContentPane().revalidate();
    }

    /*
     * Plugin interface
     */
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
