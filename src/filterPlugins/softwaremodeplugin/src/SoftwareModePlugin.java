import org.helioviewer.filter.softwaremode.SoftwareModeContainer;
import org.helioviewer.viewmodelplugin.filter.FilterPlugin;

/**
 * Dummy software filter which just pass the data through, but does not support
 * OpenGL, so that the view chain switch from software to OpenGl at this point.
 * <p>
 * To see its working it allows some logging of the requests.
 * 
 * @author Helge Dietert
 */
public class SoftwareModePlugin extends FilterPlugin {
    /**
     * Installs the software filter plugin
     */
    public SoftwareModePlugin() {
        addFilterContainer(new SoftwareModeContainer());
    }

    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Plugin#getDescription()
     */
    public String getDescription() {
        return "Dummy plugin to allow switching to software mode";
    }

    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Plugin#getName()
     */
    public String getName() {
        return "Dummy software plugin";
    }

    public String getAboutLicenseText() {
        // TODO Auto-generated method stub
        return "about software plugin";
    }

}
