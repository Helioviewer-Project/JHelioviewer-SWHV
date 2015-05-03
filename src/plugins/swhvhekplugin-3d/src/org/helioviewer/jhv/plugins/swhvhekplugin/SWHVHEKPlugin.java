package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.net.URL;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * @author Malte Nuhn
 * */
public class SWHVHEKPlugin implements Plugin {

    private boolean builtin_mode = false;

    private SWHVHEKPluginRenderable renderable;
    private SWHVHEKImagePanelEventPopupController controller;

    public SWHVHEKPlugin() {
        this(false);
    }

    /**
     * Constructor with debug flag. If debug flag is set, the plugin name shows
     * "HEK Plugin Built-In Version"
     *
     * @param builtin_mode
     *            - debug flag
     */
    public SWHVHEKPlugin(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;
        SWHVHEKData.getSingletonInstance();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the default method because the internal provided filters are
     * activated by default.
     */
    @Override
    public void installPlugin() {
        controller = new SWHVHEKImagePanelEventPopupController();
        renderable = new SWHVHEKPluginRenderable();
        ImageViewerGui.getInputController().addPlugin(controller);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getInputController().removePlugin(controller);
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        controller = null;
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
        return "SWHV HEK Plugin " + "$Rev$" + (builtin_mode ? " Built-In Version" : "");
    }

    @Override
    public String getAboutLicenseText() {
        String description = "";
        description += "<p>" + "The plugin uses the <a href=\"http://www.json.org/java/\">JSON in Java</a> Library, licensed under a <a href=\"http://www.json.org/license.html\">custom License</a>.";
        description += "<p>" + "The plugin uses the <a href=\"http://code.google.com/p/poly2tri/\">Poly2Tri</a> Library, licensed under the <a href=\"http://www.opensource.org/licenses/bsd-license.php\">BSD License</a>.";
        description += "<p>" + "The plugin uses the <a href=\"http://www.slf4j.org/\">Simple Logging Facade for Java (SLF4J)</a> Library,<br>licensed under the <a href=\"http://www.slf4j.org/license.html\">MIT License</a>.";

        return description;
    }

    public static URL getResourceUrl(String name) {
        return SWHVHEKPlugin.class.getResource(name);
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return "";
    }

}
