package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for JPEG2000 images.
 * 
 * @author Markus Langenberg
 */
public class JP2Filter extends ExtensionFileFilter {
    /**
     * Default Constructor.
     */
    public JP2Filter() {
        extensions = new String[] { "jp2", "jpx" };
    }
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "JPG2000 files (\".jp2\", \".jpx\")";
    }

}
