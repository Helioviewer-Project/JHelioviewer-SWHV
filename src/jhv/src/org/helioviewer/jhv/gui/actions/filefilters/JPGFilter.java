package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for JPEG images.
 * 
 * @author Markus Langenberg
 */
public class JPGFilter extends ExtensionFileFilter {

    /**
     * Default Constructor.
     */
    public JPGFilter() {
        extensions = new String[] { "jpg", "jpeg" };
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "JPG files (\".jpg\", \".jpeg\")";
    }
}