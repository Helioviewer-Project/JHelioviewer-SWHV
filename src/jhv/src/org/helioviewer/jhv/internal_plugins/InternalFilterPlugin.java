package org.helioviewer.jhv.internal_plugins;

import java.net.URI;
import java.net.URISyntaxException;

import org.helioviewer.filter.runningdifference.RunningDifferenceContainer;
import org.helioviewer.filter.runningdifference.RunningDifferencePlugin;
import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.SOHOLUTFilterPlugin;
import org.helioviewer.jhv.internal_plugins.filter.channelMixer.ChannelMixerPlugin;
import org.helioviewer.jhv.internal_plugins.filter.contrast.ContrastPlugin;
import org.helioviewer.jhv.internal_plugins.filter.gammacorrection.GammaCorrectionPlugin;
import org.helioviewer.jhv.internal_plugins.filter.opacity.OpacityPlugin;
import org.helioviewer.jhv.internal_plugins.filter.sharpen.SharpenPlugin;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.viewmodelplugin.filter.FilterContainer;
import org.helioviewer.viewmodelplugin.filter.FilterPlugin;

/**
 * This class acts as a default plug-in and contains all internal supplied
 * filters. The plug-in will not appear in the dialog where the user can
 * activate or deactivate plug-ins because this plug-in is activated always.
 * 
 * @author Stephan Pagel
 */
public class InternalFilterPlugin extends FilterPlugin implements InternalPlugin {

    /**
     * Default constructor.
     */
    public InternalFilterPlugin() {
        try {
            pluginLocation = new URI("internal");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        addFilterContainer(new OpacityPlugin());
        addFilterContainer(new SOHOLUTFilterPlugin());
        addFilterContainer(new ChannelMixerPlugin());
        addFilterContainer(new ContrastPlugin());
        addFilterContainer(new GammaCorrectionPlugin());
        addFilterContainer(new SharpenPlugin());
        addFilterContainer(new RunningDifferenceContainer());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the default method because the internal provided filters are
     * activated by default.
     */

    public void installPlugin() {
        for (FilterContainer filter : filterContainerList) {
            filter.setActive(PluginSettings.getSingeltonInstance().isFilterInPluginActivated(pluginLocation, filter.getFilterClass(), true));
            filter.setPosition(PluginSettings.getSingeltonInstance().getFilterPosition(pluginLocation, filter.getFilterClass()));
            PluginManager.getSingeltonInstance().addFilterContainer(filter);
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
        return "internal filters plugin";
    }

    /**
     * {@inheritDoc}
     * 
     * null because this is an internal plugin
     */
    public String getAboutLicenseText() {
        return null;
    }
}
