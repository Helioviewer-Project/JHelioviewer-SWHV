/**
 * 
 */
package ch.fhnw.jhv.plugins.pfss;

import java.util.ArrayList;

import ch.fhnw.jhv.gui.components.CameraChooserControlPlugin;
import ch.fhnw.jhv.gui.viewport.components.SunRenderPlugin;
import ch.fhnw.jhv.plugins.PluginBundle;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;
import ch.fhnw.jhv.plugins.pfss.control.PfssLoaderControlPlugin;
import ch.fhnw.jhv.plugins.pfss.control.PfssVisualizationChooserControlPlugin;
import ch.fhnw.jhv.plugins.pfss.rendering.PfssRenderer;

/**
 * PFSS Plugin Container
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PfssPlugin extends PluginBundle {

    /**
     * CONTROL PLUGINS
     */
    CameraChooserControlPlugin cameraChooserControlPlugin;
    PfssLoaderControlPlugin loader;
    PfssVisualizationChooserControlPlugin pfssVisualizationChooser;

    /**
     * RENDER PLUGINS
     */
    PfssRenderer renderer;
    SunRenderPlugin sun;

    /**
     * Empty constructor
     */
    public PfssPlugin() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.PluginBundle#install()
     */

    protected void install() {

        // Reset the render/control plugins
        renderPlugins = new ArrayList<RenderPlugin>();
        controlPlugins = new ArrayList<ControlPlugin>();

        // RENDER PLUGINS
        renderer = new PfssRenderer();
        sun = new SunRenderPlugin();

        renderer.activate();
        sun.activate();

        renderPlugins.add(renderer);
        renderPlugins.add(sun);

        // CONTROL PLUGINS
        cameraChooserControlPlugin = new CameraChooserControlPlugin();
        loader = new PfssLoaderControlPlugin();
        pfssVisualizationChooser = new PfssVisualizationChooserControlPlugin(renderer);

        controlPlugins.add(cameraChooserControlPlugin);
        controlPlugins.add(pfssVisualizationChooser);
        controlPlugins.add(loader);

        cameraChooserControlPlugin.activate();
        pfssVisualizationChooser.activate();
        loader.activate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.PluginBundle#uninstall()
     */

    protected void uninstall() {
        // CONTROL PLUGINS
        cameraChooserControlPlugin.deactivate();
        loader.deactivate();

        // RENDER PLUGINS
        renderer.deactivate();
        sun.deactivate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.PluginBundle#getType()
     */

    public PluginBundleType getType() {
        return PluginBundleType.PFSS;
    }

}
