package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for PNG images.
 * 
 * @author Markus Langenberg
 */
public class PNGFilter extends ExtensionFileFilter {

    public PNGFilter() {
        extensions = new String[] { "png" };
    }
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "PNG files (\".png\")";
    }

}
