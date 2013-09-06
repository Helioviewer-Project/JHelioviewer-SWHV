package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for all Fits files.
 * 
 * @author Markus Langenberg
 */
public class FitsFilter extends ExtensionFileFilter {

    /**
     * Default Constructor.
     */
    public FitsFilter() {
        extensions = new String[] { "fits", "fts" };
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "FITS files (\".fts\", \".fits\")";
    }
}