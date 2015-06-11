package org.helioviewer.jhv.plugins.pfssplugin;

import java.net.URL;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugin.interfaces.Plugin;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCache;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin implements Plugin {

    private static PfssCache pfssCache;
    private PfssRenderable renderable;

    public PfssPlugin() {
        pfssCache = new PfssCache();
        renderable = new PfssRenderable();
    }

    public static PfssCache getPfsscache() {
        return pfssCache;
    }

    @Override
    public void installPlugin() {
        Layers.addLayersListener(renderable);
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeLayersListener(renderable);
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes PFSS model data";
    }

    @Override
    public String getName() {
        return "PFSS Plugin " + "$Rev$";
    }

    @Override
    public String getAboutLicenseText() {
        String description = "<p>The plugin uses the <a href=\"http://heasarc.gsfc.nasa.gov/docs/heasarc/fits/java/v1.0/\">Fits in Java</a> Library, licensed under a <a href=\"https://www.gnu.org/licenses/old-licenses/gpl-1.0-standalone.html\">GPL License</a>.";
        description += "<p>The plugin uses the <a href=\"http://www.bzip.org\">Bzip2</a> Library, licensed under the <a href=\"http://opensource.org/licenses/bsd-license.php\">BSD License</a>.";
        return description;
    }

    public static URL getResourceUrl(String name) {
        return PfssPlugin.class.getResource(name);
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

}
