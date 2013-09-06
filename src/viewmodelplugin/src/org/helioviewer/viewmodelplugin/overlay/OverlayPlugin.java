package org.helioviewer.viewmodelplugin.overlay;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.LinkedList;

import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * This is the basic class for all plug-ins which supplies overlays for JHV. It
 * handles the registration of the overlays in JHV.
 * 
 * @author Stephan Pagel
 */
public abstract class OverlayPlugin implements Plugin {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    protected AbstractList<OverlayContainer> overlayContainerList = new LinkedList<OverlayContainer>();
    protected URI pluginLocation;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     */
    public OverlayPlugin() {
        try {
            pluginLocation = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds all supplied overlays to the list of available and active overlays.
     */
    public void installPlugin() {
        for (OverlayContainer overlay : overlayContainerList) {
            overlay.setActive(PluginSettings.getSingeltonInstance().isOverlayInPluginActivated(pluginLocation, overlay.getOverlayClass(), false));
            overlay.setPosition(PluginSettings.getSingeltonInstance().getOverlayPosition(pluginLocation, overlay.getOverlayClass()));
            PluginManager.getSingeltonInstance().addOverlayContainer(overlay);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes all supplied overlays from the list of available and active
     * overlays.
     */
    public void uninstallPlugin() {
        for (OverlayContainer overlay : overlayContainerList) {
            PluginManager.getSingeltonInstance().removeOverlayContainer(overlay);
        }
    }

    /**
     * Adds a overlay container to the internal list of overlays which the
     * plug-in supplies.
     * 
     * @param overlayContainer
     *            Overlay container which contains the overlay which should be
     *            supplied by the plug-in.
     */
    protected void addOverlayContainer(OverlayContainer overlayContainer) {
        overlayContainer.setPluginLocation(pluginLocation);
        overlayContainerList.add(overlayContainer);
    }
}
