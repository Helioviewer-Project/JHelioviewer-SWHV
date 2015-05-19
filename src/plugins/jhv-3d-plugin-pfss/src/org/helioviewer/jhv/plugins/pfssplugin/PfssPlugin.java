package org.helioviewer.jhv.plugins.pfssplugin;

import java.net.URL;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugin.interfaces.Plugin;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCache;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin implements Plugin {

    private boolean builtin_mode = false;
    private static PfssCache pfssCache = new PfssCache();
    private PfssRenderable renderable;

    /**
     * Default constructor.
     */
    public PfssPlugin() {
        this(false);
    }

    /**
     * Constructor with debug flag. If debug flag is set, the plugin name shows
     * "Pfss Plugin Built-In Version"
     *
     * @param builtin_mode
     *            - debug flag
     */
    public PfssPlugin(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the default method because the internal provided filters are
     * activated by default.
     */
    @Override
    public void installPlugin() {
        renderable = new PfssRenderable();
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        pfssCache = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A description is not needed here because this plug-in is activated always
     * and will not be visible in the corresponding dialogs.
     */
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "PFSS Plugin " + "$Rev$" + (builtin_mode ? " Built-In Version" : "");
    }

    /**
     * {@inheritDoc}
     *
     * null because this is an internal plugin
     */
    @Override
    public String getAboutLicenseText() {
        String description = "";
        description += "<p>" + "The plugin uses the <a href=\"http://heasarc.gsfc.nasa.gov/docs/heasarc/fits/java/v1.0/\">Fits in Java</a> Library, licensed under a <a href=\"https://www.gnu.org/licenses/old-licenses/gpl-1.0-standalone.html\">GPL License</a>.";
        description += "<p>" + "The plugin uses the <a href=\"http://www.bzip.org\">Bzip2</a> Library, licensed under the <a href=\"http://opensource.org/licenses/bsd-license.php\">BSD License</a>.";

        return description;
    }

    public static URL getResourceUrl(String name) {
        return PfssPlugin.class.getResource(name);
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void setState(String state) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public String getState() {
        return "";
    }

    public static PfssCache getPfsscache() {
        return pfssCache;
    }

}
