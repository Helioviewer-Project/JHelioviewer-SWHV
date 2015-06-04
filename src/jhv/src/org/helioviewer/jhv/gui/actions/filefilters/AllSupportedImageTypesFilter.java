package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for all supported image types.
 * 
 * @author Markus Langenberg
 */
public class AllSupportedImageTypesFilter extends ExtensionFileFilter {

    public AllSupportedImageTypesFilter() {
        extensions = new String[] { "jpg", "jpeg", "png", "fts", "fits", "jp2", "jpx" };
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "All supported files (\".jpg\", \".jpeg\", \".png\", \".fts\", \".fits\", \".jp2\", \".jpx\")";
    }

}