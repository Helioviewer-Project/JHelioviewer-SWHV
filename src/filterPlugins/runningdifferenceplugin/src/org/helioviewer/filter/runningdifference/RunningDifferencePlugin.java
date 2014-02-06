package org.helioviewer.filter.runningdifference;
import org.helioviewer.viewmodelplugin.filter.FilterPlugin;

/**
 * Filter to support running differences in a movie.
 * <p>
 * This basis plugin class must be in the default package with the exactly the
 * same name as the generated jar file
 * 
 * @author Helge Dietert
 */
public class RunningDifferencePlugin extends FilterPlugin {
    /**
     * Creates a new running difference filter
     */
    public RunningDifferencePlugin() {
        addFilterContainer(new RunningDifferenceContainer());
    }

    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Plugin#getDescription()
     */
    public String getDescription() {
        return "This plugin supports running difference in movies";
    }

    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Plugin#getName()
     */
    public String getName() {
        return "Running Difference";
    }

	public String getAboutLicenseText() {
		// TODO Auto-generated method stub
		return "about ...";
	}

}
