package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.net.URL;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugin.interfaces.Plugin;

public class SWHVHEKPlugin implements Plugin {

    private SWHVHEKPluginRenderable renderable;
    private SWHVHEKImagePanelEventPopupController controller;

    public SWHVHEKPlugin() {
        SWHVHEKData.getSingletonInstance();
        renderable = new SWHVHEKPluginRenderable();
    }

    @Override
    public void installPlugin() {
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
        controller = new SWHVHEKImagePanelEventPopupController();
        ImageViewerGui.getInputController().addPlugin(controller);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getInputController().removePlugin(controller);
        controller = null;
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
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
