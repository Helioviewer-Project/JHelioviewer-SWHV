package org.helioviewer.gl3d.plugin.pfss;

import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * Plugincontainer for Pfss
 *
 * @author Stefan Meier
 */
public class PfssPluginContainer implements Plugin {

    private boolean builtin_mode = false;
    private PfssCache pfssCache;

    public PfssPluginContainer(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "PFSS Model " + (builtin_mode ? "Built-In Version" : "");
    }

    @Override
    public void installPlugin() {
        pfssCache = new PfssCache();
        new PfssPlugin3dRenderable(pfssCache);
    }

    @Override
    public void uninstallPlugin() {
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return "";
    }

    @Override
    public String getAboutLicenseText() {
        return "";
    }

}
