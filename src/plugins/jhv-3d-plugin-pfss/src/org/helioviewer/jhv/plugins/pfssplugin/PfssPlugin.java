package org.helioviewer.jhv.plugins.pfssplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.helioviewer.gl3d.plugin.pfss.PfssRenderable;
import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.viewmodelplugin.overlay.OverlayPlugin;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin extends OverlayPlugin implements Plugin {

    private boolean builtin_mode = false;
    private PfssCache pfssCache;

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

        try {
            this.pluginLocation = new URI(PfssSettings.PLUGIN_LOCATION);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the default method because the internal provided filters are
     * activated by default.
     */
    @Override
    public void installPlugin() {
        pfssCache = new PfssCache();
        new PfssRenderable(pfssCache);
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
        return "PFSS Overlay Plugin " + "$Rev$" + (builtin_mode ? " Built-In Version" : "");
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

}
