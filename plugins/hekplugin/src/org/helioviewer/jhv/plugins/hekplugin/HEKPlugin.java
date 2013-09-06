package org.helioviewer.jhv.plugins.hekplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugins.hekplugin.controller.ImagePanelEventPopupController;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayPlugin;

/**
 * @author Malte Nuhn
 * */
public class HEKPlugin extends OverlayPlugin implements Plugin {

    private boolean builtin_mode = false;

    /**
     * Reference to the eventPlugin
     */
    private HEKPluginContainer eventPlugin;

    /**
     * Default constructor.
     */
    public HEKPlugin() {
        this(false);
    }

    /**
     * Constructor with debug flag. If debug flag is set, the plugin name shows
     * "HEK Plugin Built-In Version"
     * 
     * @param builtin_mode
     *            - debug flag
     */
    public HEKPlugin(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;

        try {
            this.pluginLocation = new URI(HEKSettings.PLUGIN_LOCATION);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        eventPlugin = new HEKPluginContainer(builtin_mode);
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
            ImageViewerGui.getSingletonInstance().getMainImagePanel().addPlugin(new ImagePanelEventPopupController());
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
        return "HEK Overlay Plugin " + (builtin_mode ? "Built-In Version" : "");
    }

    /**
     * Wrapper around HEKEventPlugins functions.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.plugin.HEKEventPlugin
     * @see org.helioviewer.jhv.plugins.overlay.hek.plugin.HEKEventPlugin#setCurInterval
     * @param newInterval
     */
    public void setCurInterval(Interval<Date> newInterval) {
        eventPlugin.setCurInterval(newInterval);
    }

    /**
     * Wrapper around HEKEventPlugins functions.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.plugin.HEKEventPlugin
     * @see org.helioviewer.jhv.plugins.overlay.hek.plugin.HEKEventPlugin#getStructure
     * @param newInterval
     */
    public void getStructure() {
        eventPlugin.getStructure();
    }

    public void setEnabled(boolean b) {
        eventPlugin.setEnabled(b);
    }

    /**
     * {@inheritDoc}
     * 
     * null because this is an internal plugin
     */
    public String getAboutLicenseText() {
        String description = "";
        description += "<p>" + "The plugin uses the <a href=\"http://www.json.org/java/\">JSON in Java</a> Library, licensed under a <a href=\"http://www.json.org/license.html\">custom License</a>.";
        description += "<p>" + "The plugin uses the <a href=\"http://code.google.com/p/poly2tri/\">Poly2Tri</a> Library, licensed under the <a href=\"http://www.opensource.org/licenses/bsd-license.php\">BSD License</a>.";
        description += "<p>" + "The plugin uses the <a href=\"http://www.slf4j.org/\">Simple Logging Facade for Java (SLF4J)</a> Library,<br>licensed under the <a href=\"http://www.slf4j.org/license.html\">MIT License</a>.";

        return description;
    }

    public static URL getResourceUrl(String name) {
        return HEKPlugin.class.getResource(name);
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void setState(String state) {
        // TODO Implement setState for HEKPlugin
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public String getState() {
        // TODO Implement getState for HEKPlugin
        return "";
    }
}