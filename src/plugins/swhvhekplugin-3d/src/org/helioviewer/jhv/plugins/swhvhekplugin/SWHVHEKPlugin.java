package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.net.URL;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugin.interfaces.Plugin;

public class SWHVHEKPlugin implements Plugin {

    private static SWHVHEKData swekData;
    private SWHVHEKPluginRenderable renderable;
    private SWHVHEKImagePanelEventPopupController controller;

    public SWHVHEKPlugin() {
        renderable = new SWHVHEKPluginRenderable();
    }

    public static SWHVHEKData getSWEKData() {
        return swekData;
    }

    @Override
    public void installPlugin() {
        controller = new SWHVHEKImagePanelEventPopupController();
        ImageViewerGui.getInputController().addPlugin(controller);

        SWHVHEKData.getSingletonInstance().requestEvents();
        LayersModel.addLayersListener(SWHVHEKData.getSingletonInstance());
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        LayersModel.removeLayersListener(SWHVHEKData.getSingletonInstance());
        SWHVHEKData.getSingletonInstance().reset();

        ImageViewerGui.getInputController().removePlugin(controller);
        controller = null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getName() {
        return "SWHV HEK Plugin " + "$Rev$";
    }

    @Override
    public String getAboutLicenseText() {
        return null;
    }

    public static URL getResourceUrl(String name) {
        return SWHVHEKPlugin.class.getResource(name);
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

}
