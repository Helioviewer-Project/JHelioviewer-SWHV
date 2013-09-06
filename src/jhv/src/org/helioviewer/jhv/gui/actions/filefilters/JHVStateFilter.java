package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for JHV state files.
 * 
 * @author Markus Langenberg
 */
public class JHVStateFilter extends ExtensionFileFilter {

    /**
     * Default Constructor.
     */
    public JHVStateFilter() {
        extensions = new String[] { "jhv" };
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "JHV State files (\".jhv\")";
    }
}