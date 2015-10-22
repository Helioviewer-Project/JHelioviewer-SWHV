package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.net.URL;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;

public class SWHVHEKPlugin implements Plugin {

    private SWHVHEKPluginRenderable renderable;

    public SWHVHEKPlugin() {
        renderable = new SWHVHEKPluginRenderable();
    }

    @Override
    public void installPlugin() {
        SWHVHEKData.getSingletonInstance().requestEvents();
        Layers.addLayersListener(SWHVHEKData.getSingletonInstance());
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeLayersListener(SWHVHEKData.getSingletonInstance());
        SWHVHEKData.getSingletonInstance().reset();
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes SWEK events on the solar disk";
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
