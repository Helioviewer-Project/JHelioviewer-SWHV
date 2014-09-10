/**
 *
 */
package org.helioviewer.jhv.plugins.swek;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManager;
import org.helioviewer.jhv.plugins.swek.request.OutgoingRequestManager;
import org.helioviewer.jhv.plugins.swek.settings.SWEKSettings;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.viewmodelplugin.overlay.OverlayPlugin;

/**
 * Part of these developments are based on the work done in the HEKPlugin
 * (lp:~jhelioviewer-dev/jhelioviewer/hekplugin) and HEKPlugin 3d
 * (lp:~jhelioviewer-dev/jhelioviewer/hekplugin-3d).
 * 
 * @author Bram.Bourgoignie@oma.be
 * 
 */
public class SWEKPlugin extends OverlayPlugin implements Plugin {

    /** Instance of the SWEKConfiguration */
    private final SWEKConfigurationManager SWEKConfig;

    /** Instance of the SWEKDownloadManager */
    private final SWEKSourceManager SWEKSources;

    /** The outgoing request manager */
    private final OutgoingRequestManager outgoingRequestManager;

    /** the incoming request manager */
    private final IncomingRequestManager incomingRequestManager;

    /** instance of the event container */
    private final JHVEventContainer eventContainer;

    public SWEKPlugin() {
        SWEKConfig = SWEKConfigurationManager.getSingletonInstance();
        SWEKSources = SWEKSourceManager.getSingletonInstance();
        outgoingRequestManager = OutgoingRequestManager.getSingletonInstance();
        incomingRequestManager = IncomingRequestManager.getSingletonInstance();
        eventContainer = JHVEventContainer.getSingletonInstance();
        try {
            pluginLocation = new URI(SWEKSettings.PLUGIN_NAME);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void installPlugin() {
        createPluginDirectoryStructure();
        configurePlugin();
        registerPlugin();
    }

    /*
     * Plugin interface
     */
    @Override
    public String getName() {
        return "Space Weather Event Knowledgbase";
    }

    @Override
    public String getDescription() {
        return "A description";
    }

    @Override
    public void setState(String state) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getState() {
        // TODO Auto-generated method stub
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
        LayersModel.getSingletonInstance().addLayersListener(outgoingRequestManager);
        eventContainer.registerHandler(incomingRequestManager);
    }

    /**
     * Add the plugin to the JHV system.
     */
    private void registerPlugin() {
        SWEKPluginContainer container = new SWEKPluginContainer();
        container.setActive(PluginSettings.getSingeltonInstance().isOverlayInPluginActivated(pluginLocation, container.getOverlayClass(),
                true));
        container.setPosition(PluginSettings.getSingeltonInstance().getOverlayPosition(pluginLocation, container.getOverlayClass()));
        PluginManager.getSingeltonInstance().addOverlayContainer(container);
    }

}
