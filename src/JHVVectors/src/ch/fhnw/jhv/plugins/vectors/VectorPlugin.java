package ch.fhnw.jhv.plugins.vectors;

import java.util.ArrayList;

import ch.fhnw.jhv.gui.components.CameraChooserControlPlugin;
import ch.fhnw.jhv.gui.controller.cam.CameraCenterPointRenderPlugin;
import ch.fhnw.jhv.gui.viewport.components.SunRenderPlugin;
import ch.fhnw.jhv.plugins.PluginBundle;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;
import ch.fhnw.jhv.plugins.vectors.control.InformationControlPlugin;
import ch.fhnw.jhv.plugins.vectors.control.SettingsControlPlugin;
import ch.fhnw.jhv.plugins.vectors.control.VectorsLoaderControlPlugin;
import ch.fhnw.jhv.plugins.vectors.interpolation.FieldAnimatorRenderPlugin;
import ch.fhnw.jhv.plugins.vectors.rendering.PlaneRenderPlugin;

/**
 * Vector Plugin
 * 
 * Contains all the control plugins and the corresponding render plugins. Is
 * responsible for the install and uninstall method.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorPlugin extends PluginBundle {

    /**
     * CONTROL PLUGINS
     */
    private SettingsControlPlugin settingsControlPlugin;
    private InformationControlPlugin informationControlPlugin;
    private VectorsLoaderControlPlugin vectorsLoaderControlPlugin;
    private CameraChooserControlPlugin cameraChooserControlPlugin;

    /**
     * RENDER PLUGINS
     */
    private PlaneRenderPlugin planeRenderPlugin;
    private SunRenderPlugin sunRenderPlugin;
    private FieldAnimatorRenderPlugin fieldAnimatorRenderPlugin;
    private CameraCenterPointRenderPlugin cameraCenterPointRenderPlugin;

    /**
     * Constructor
     */
    public VectorPlugin() {
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

        /*
         * INSTANTIATE Plugins
         */
        // CONTROL PLUGIN
        settingsControlPlugin = new SettingsControlPlugin();
        informationControlPlugin = new InformationControlPlugin();
        vectorsLoaderControlPlugin = new VectorsLoaderControlPlugin();
        cameraChooserControlPlugin = new CameraChooserControlPlugin();

        controlPlugins.add(cameraChooserControlPlugin);
        controlPlugins.add(informationControlPlugin);
        controlPlugins.add(settingsControlPlugin);
        controlPlugins.add(vectorsLoaderControlPlugin);

        // RENDER PLUGIN
        planeRenderPlugin = new PlaneRenderPlugin();
        sunRenderPlugin = new SunRenderPlugin();
        fieldAnimatorRenderPlugin = new FieldAnimatorRenderPlugin();
        cameraCenterPointRenderPlugin = new CameraCenterPointRenderPlugin();

        renderPlugins.add(planeRenderPlugin);
        renderPlugins.add(sunRenderPlugin);
        renderPlugins.add(fieldAnimatorRenderPlugin);
        renderPlugins.add(cameraCenterPointRenderPlugin);

        /*
         * ACTIVATE Plugins
         */
        // CONTROL PLUGINS
        informationControlPlugin.activate();
        settingsControlPlugin.activate();
        settingsControlPlugin.setEnabled(false);
        cameraChooserControlPlugin.activate();
        vectorsLoaderControlPlugin.activate();

        // RENDER PLUGINS
        sunRenderPlugin.activate();
        fieldAnimatorRenderPlugin.activate();
        // planeRenderPlugin.activate();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.PluginBundle#uninstall()
     */

    protected void uninstall() {
        // CONTROL PLUGINS
        settingsControlPlugin.deactivate();
        informationControlPlugin.deactivate();
        vectorsLoaderControlPlugin.deactivate();
        cameraChooserControlPlugin.deactivate();

        // RENDER PLUGINS
        sunRenderPlugin.deactivate();
        planeRenderPlugin.deactivate();
        fieldAnimatorRenderPlugin.deactivate();

        if (PluginManager.getInstance().getRenderPluginByType(RenderPluginType.CAMERACENTERPOINT) != null)
            cameraCenterPointRenderPlugin.deactivate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.PluginBundle#getType()
     */

    public PluginBundleType getType() {
        return PluginBundleType.VECTOR;
    }

}
