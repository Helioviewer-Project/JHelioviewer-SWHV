package org.helioviewer.jhv.plugins.pfssplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.helioviewer.gl3d.plugin.pfss.PfssPluginContainer;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayPlugin;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin extends OverlayPlugin implements Plugin {

    private boolean builtin_mode = false;

    /**
     * Reference to the eventPlugin
     */
    private PfssPluginContainer eventPlugin;

    /**
     * Default constructor.
     */
    public PfssPlugin() {
        this(false);
    }

    /**
     * Constructor with debug flag. If debug flag is set, the plugin name shows
     * "Pfss Plugin Built-In Version"
     * 
     * @param builtin_mode
     *            - debug flag
     */
    public PfssPlugin(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;

        try {
            this.pluginLocation = new URI(PfssSettings.PLUGIN_LOCATION);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        eventPlugin = new PfssPluginContainer(builtin_mode);
        addOverlayContainer(eventPlugin);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the default method because the internal provided filters are
     * activated by default.
     */
    public void installPlugin() {
        for (OverlayContainer overlay : overlayContainerList) {
            overlay.setActive(PluginSettings.getSingeltonInstance().isOverlayInPluginActivated(pluginLocation, overlay.getOverlayClass(), true));
            overlay.setPosition(PluginSettings.getSingeltonInstance().getOverlayPosition(pluginLocation, overlay.getOverlayClass()));
            PluginManager.getSingeltonInstance().addOverlayContainer(overlay);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * A description is not needed here because this plug-in is activated always
     * and will not be visible in the corresponding dialogs.
     */
    public String getDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "Pfss Overlay Plugin " + (builtin_mode ? "Built-In Version" : "");
    }

    /**
     * {@inheritDoc}
     * 
     * null because this is an internal plugin
     */
    public String getAboutLicenseText() {
        String description = "";
        description += "<p>" + "The plugin uses the <a href=\"http://heasarc.gsfc.nasa.gov/docs/heasarc/fits/java/v1.0/\">Fits in Java</a> Library, licensed under a <a href=\"https://www.gnu.org/licenses/old-licenses/gpl-1.0-standalone.html\">GPL License</a>.";
        description += "<p>" + "The plugin uses the <a href=\"http://www.bzip.org\">Bzip2</a> Library, licensed under the <a href=\"http://opensource.org/licenses/bsd-license.php\">BSD License</a>.";

        return description;
    }

    public static URL getResourceUrl(String name) {
        return PfssPlugin.class.getResource(name);
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void setState(String state) {
        // TODO Implement setState for PfssPlugin
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public String getState() {
        // TODO Implement getState for PfssPlugin
        return "";
    }
}