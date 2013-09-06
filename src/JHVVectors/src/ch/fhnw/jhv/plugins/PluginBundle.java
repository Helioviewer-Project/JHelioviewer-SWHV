/**
 * 
 */
package ch.fhnw.jhv.plugins;

import java.util.ArrayList;

import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;

/**
 * Abstract plugin
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public abstract class PluginBundle {

    /**
     * Contains all the control plugins
     */
    protected ArrayList<ControlPlugin> controlPlugins = new ArrayList<ControlPlugin>();

    /**
     * Contains all the render plugins
     */
    protected ArrayList<RenderPlugin> renderPlugins = new ArrayList<RenderPlugin>();

    /**
     * Plugin types currently used
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public enum PluginBundleType {
        VECTOR, PFSS;
    }

    /**
     * Install a plugin
     */
    protected abstract void install();

    /**
     * Uninstall a plugin
     */
    protected abstract void uninstall();

    /**
     * Return the type of the plugin
     * 
     * @return PluginType type
     */
    public abstract PluginBundleType getType();
}
