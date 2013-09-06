/**
 * 
 */
package ch.fhnw.jhv.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import ch.fhnw.jhv.gui.components.CollapsiblePane;
import ch.fhnw.jhv.gui.components.CollapsiblePane.CollapsiblePaneResizeListener;
import ch.fhnw.jhv.plugins.PluginBundle.PluginBundleType;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin.ControlPluginType;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;

/**
 * Plugin Manager is responsible for handling all the plugins correctly
 * 
 * At the moment there exists the following two plugin types: - RenderPlugin
 * (Have direct access to the GLCanvas) - ControlPlugin (Are located at the left
 * pane)
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PluginManager {

    /**
     * Stores the size of the left pane
     */
    private int leftPaneSize;

    /**
     * List of plugins bundles
     */
    private ArrayList<PluginBundle> pluginBundles;

    /**
     * All currently active plugin bundles
     */
    private ArrayList<PluginBundle> activePluginBundles;

    /**
     * Contains all the control plugins in a JPanel for the leftPane
     */
    private JPanel controlPanels = new JPanel();

    /**
     * Only existing instance of the PluginManager
     */
    private static PluginManager instance = new PluginManager();

    /**
     * Get the only existing instance of the PluginManager
     * 
     * @return PluginManager pluginManager
     */
    public static PluginManager getInstance() {
        return instance;
    }

    /**
     * Constructor
     */
    private PluginManager() {
        this.pluginBundles = new ArrayList<PluginBundle>();
        this.activePluginBundles = new ArrayList<PluginBundle>();

        // Panel settings
        controlPanels.setLayout(new BoxLayout(controlPanels, BoxLayout.Y_AXIS));
    }

    /**
     * Return a RenderPlugin by type.
     * 
     * If it didnt find the Pluging it returns null
     * 
     * @param type
     *            RenderPluginType
     * 
     * @return RenderPlugin or null if it has not been found
     */
    public RenderPlugin getRenderPluginByType(RenderPluginType type) {

        // NOTE: atm it always returns the first found object and not all
        // objects with the same type!

        for (PluginBundle activePlugin : activePluginBundles) {
            for (RenderPlugin renderPlugin : activePlugin.renderPlugins) {
                if (renderPlugin.isActive() && renderPlugin.getType().equals(type)) {
                    return renderPlugin;
                }
            }
        }

        return null;
    }

    /**
     * Return a ControlPlugin by type.
     * 
     * If it didnt find the Pluging it returns null
     * 
     * @param type
     *            ControlPluginType
     * 
     * @return ControlPlugin or null if it has not been found
     */
    public ControlPlugin getControlPluginByType(ControlPluginType type) {

        // NOTE: atm it always returns the first found object and not all
        // objects with the same type!

        for (PluginBundle activePlugin : activePluginBundles) {
            for (ControlPlugin controlPlugin : activePlugin.controlPlugins) {
                if (controlPlugin.isActive() && controlPlugin.getType().equals(type)) {
                    return controlPlugin;
                }
            }
        }

        return null;
    }

    /**
     * Activate Render Plugin
     * 
     * @param renderPluginType
     *            RenderPluginType
     */
    public void activateRenderPluginType(RenderPluginType renderPluginType) {

        for (PluginBundle activePlugin : activePluginBundles) {
            for (RenderPlugin renderPlugin : activePlugin.renderPlugins) {
                if (renderPlugin.getType().equals(renderPluginType)) {
                    renderPlugin.activate();
                }
            }
        }
    }

    /**
     * Deactivate Render Plugin
     * 
     * @param renderPluginType
     *            RenderPluginType
     */
    public void deactivateRenderPluginType(RenderPluginType renderPluginType) {
        for (PluginBundle activePlugin : activePluginBundles) {
            for (RenderPlugin renderPlugin : activePlugin.renderPlugins) {
                if (renderPlugin.getType().equals(renderPluginType)) {
                    renderPlugin.deactivate();
                }
            }
        }
    }

    /**
     * Activate Control Plugin
     * 
     * @param controlPluginType
     *            ControlPluginType
     */
    public void activateControlPluginType(ControlPluginType controlPluginType) {

        for (PluginBundle activePlugin : activePluginBundles) {
            for (ControlPlugin controlPlugin : activePlugin.controlPlugins) {
                if (controlPlugin.getType().equals(controlPluginType)) {
                    controlPlugin.activate();
                }
            }
        }
    }

    /**
     * Deactivate Control Plugin
     * 
     * @param controlPluginType
     *            ControlPluginType
     */
    public void deactivateControlPluginType(ControlPluginType controlPluginType) {

        for (PluginBundle activePlugin : activePluginBundles) {
            for (ControlPlugin controlPlugin : activePlugin.controlPlugins) {
                if (controlPlugin.getType().equals(controlPluginType)) {
                    controlPlugin.deactivate();
                }
            }
        }
    }

    /**
     * Return the Control panels
     * 
     * @param listener
     *            CollapsiblePaneResizeListener
     * 
     * @return JPanel all the control panels combined in one JPanel
     */
    public JPanel getControlPanels(CollapsiblePaneResizeListener listener) {

        // remove all existing control panels
        controlPanels.removeAll();

        for (PluginBundle activePlugin : activePluginBundles) {

            for (ControlPlugin controlPlugin : activePlugin.controlPlugins) {
                if (controlPlugin.isActive()) {
                    CollapsiblePane collapsiblePane = new CollapsiblePane(controlPlugin.getTitle(), controlPlugin.getComponent(), controlPlugin.shouldStartExpanded(), leftPaneSize);

                    // add listener
                    if (listener != null)
                        collapsiblePane.addListener(listener);

                    controlPanels.add(collapsiblePane);
                }
            }
        }

        return controlPanels;
    }

    /**
     * Update a renderPlugin Object from outside
     * 
     * @param renderPluginType
     *            RenderPluginType
     * @param replacementRenderPlugin
     *            RenderPlugin
     */
    public void updateRenderPlugingReference(RenderPluginType renderPluginType, RenderPlugin renderPlugin) {

        // update plugins
        for (PluginBundle plugin : pluginBundles) {

            Iterator<RenderPlugin> iter = plugin.renderPlugins.iterator();

            while (iter.hasNext()) {
                RenderPlugin val = iter.next();

                if (val.getType() == renderPluginType) {
                    iter.remove();
                    break;
                }
            }

            plugin.renderPlugins.add(renderPlugin);
        }

        // update active plugins
        for (PluginBundle plugin : activePluginBundles) {

            Iterator<RenderPlugin> iterActivePlugins = plugin.renderPlugins.iterator();

            while (iterActivePlugins.hasNext()) {
                RenderPlugin val = iterActivePlugins.next();

                if (val.getType() == renderPluginType) {
                    iterActivePlugins.remove();
                    break;
                }
            }

            plugin.renderPlugins.add(renderPlugin);
        }
    }

    /**
     * Get all render plugins
     * 
     * @return renderPlugins List<RenderPlugin>
     */
    public List<RenderPlugin> getRenderPlugins() {
        List<RenderPlugin> listOfRenderPlugins = new ArrayList<RenderPlugin>();

        for (PluginBundle activePlugin : activePluginBundles) {
            for (RenderPlugin renderPlugin : activePlugin.renderPlugins) {

                if (renderPlugin.isActive()) {
                    listOfRenderPlugins.add(renderPlugin);
                }
            }
        }

        return listOfRenderPlugins;
    }

    /**
     * Install a plugin
     * 
     * @param type
     *            PluginBundleType
     */
    public void installPluginBundle(PluginBundleType type) {

        for (PluginBundle plugin : pluginBundles) {
            if (plugin.getType() == type) {

                // put it into the active plugins arraylist
                activePluginBundles.add(plugin);

                // call install method
                plugin.install();
            }
        }
    }

    /**
     * Uninstall a plugin
     * 
     * @param type
     *            PluginBundleType
     */
    public void uninstallPluginBundle(PluginBundleType type) {

        for (PluginBundle plugin : pluginBundles) {
            if (plugin.getType() == type) {
                // call uninstall method
                plugin.uninstall();

                // put it into the active plugins arraylist
                if (activePluginBundles.contains(plugin)) {
                    activePluginBundles.remove(plugin);
                } else {
                    System.out.println("Plugin doesnt exist in the activePlugins, Cant remove it..");
                }
            }
        }

        activePluginBundles = new ArrayList<PluginBundle>();
    }

    /**
     * Add a plugin to the plugin system
     * 
     * @param plugin
     *            PluginBundle
     */
    public void addPlugin(PluginBundle plugin) {
        if (pluginBundles.contains(plugin)) {
            System.out.println("Cannot add plugin twice");
        } else {
            pluginBundles.add(plugin);
        }
    }

    /**
     * Return all current added plugins
     * 
     * @return plugins ArrayList<Plugin>
     */
    public ArrayList<PluginBundle> getPlugins() {
        return this.pluginBundles;
    }

    /**
     * Return all current active added plugins
     * 
     * @return ArrayList<Plugin> all active plugins
     */
    public ArrayList<PluginBundle> getActivePlugins() {
        return this.activePluginBundles;
    }

    /**
     * Set the left pane size
     * 
     * @param leftPaneSize
     *            size of the left pane
     */
    public void setLeftPaneSize(int leftPaneSize) {
        this.leftPaneSize = leftPaneSize;
    }
}
