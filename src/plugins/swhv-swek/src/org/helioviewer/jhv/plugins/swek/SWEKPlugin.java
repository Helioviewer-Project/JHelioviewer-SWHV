package org.helioviewer.jhv.plugins.swek;

import java.io.File;

import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManager;
import org.helioviewer.jhv.plugins.swek.settings.SWEKSettings;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;
import org.helioviewer.jhv.plugins.swek.view.SWEKPluginPanel;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * Part of these developments are based on the work done in the HEKPlugin
 * (lp:~jhelioviewer-dev/jhelioviewer/hekplugin) and HEKPlugin 3d
 * (lp:~jhelioviewer-dev/jhelioviewer/hekplugin-3d).
 * 
 * @author Bram.Bourgoignie@oma.be
 * 
 */
public class SWEKPlugin implements Plugin {

    /** Instance of the SWEKConfiguration */
    private final SWEKConfigurationManager SWEKConfig;

    /** Instance of the SWEKDownloadManager */
    private final SWEKSourceManager SWEKSources;

    /** the incoming request manager */
    private final IncomingRequestManager incomingRequestManager;

    /** instance of the event container */
    private final JHVEventContainer eventContainer;

    private final boolean loadExternalJars;

    public SWEKPlugin() {
        SWEKConfig = SWEKConfigurationManager.getSingletonInstance();
        SWEKSources = SWEKSourceManager.getSingletonInstance();
        loadExternalJars = true;
        SWEKSources.setPlugin(this);
        SWEKSources.loadExternalJars(loadExternalJars);

        incomingRequestManager = IncomingRequestManager.getSingletonInstance();
        eventContainer = JHVEventContainer.getSingletonInstance();
    }

    /**
     * Creates a SWEKPlugin that loads or doesn't load the external jars
     * 
     * @param loadExternalJars
     *            true is the source jar should be loaded, false if the source
     *            jars should not be loaded.
     */
    public SWEKPlugin(boolean loadExternalJars) {
        SWEKConfig = SWEKConfigurationManager.getSingletonInstance();
        SWEKSources = SWEKSourceManager.getSingletonInstance();
        this.loadExternalJars = loadExternalJars;
        SWEKSources.loadExternalJars(loadExternalJars);

        incomingRequestManager = IncomingRequestManager.getSingletonInstance();
        eventContainer = JHVEventContainer.getSingletonInstance();
    }

    @Override
    public void installPlugin() {
        createPluginDirectoryStructure();
        configurePlugin();
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
        String description = "";
        description += "<p>The plugin uses the <a href=\"http://www.json.org/java/\">JSON in Java</a> Library, licensed under a <a href=\"http://www.json.org/license.html\">custom License</a>.";
        return description;
    }

    /**
     * Creates the directory structure in the home directory of the JHelioviewer
     */
    private void createPluginDirectoryStructure() {
        File swekHomeFile = new File(SWEKSettings.SWEK_HOME);
        if (!swekHomeFile.isDirectory()) {
            swekHomeFile.mkdirs();
        }
        File swekSourceJarDirectory = new File(SWEKSettings.SWEK_SOURCES);
        if (!swekSourceJarDirectory.isDirectory()) {
            swekSourceJarDirectory.mkdirs();
        }
    }

    /**
     * Configures the SWEK plugin.
     */
    private void configurePlugin() {
        SWEKConfig.loadConfiguration();
        SWEKSources.loadSources();
        eventContainer.registerHandler(incomingRequestManager);
        ImageViewerGui.getLeftContentPane().add("Space Weather Event Knowledgebase", SWEKPluginPanel.getSWEKPluginPanelInstance(), false);
        ImageViewerGui.getLeftContentPane().revalidate();
    }

    @Override
    public void uninstallPlugin() {
    }

}
