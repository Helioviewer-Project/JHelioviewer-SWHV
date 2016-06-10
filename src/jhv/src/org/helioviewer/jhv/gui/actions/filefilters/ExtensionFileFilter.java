package org.helioviewer.jhv.gui.actions.filefilters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Abstract class representing a file filter which filters based on the
 * extension of a file.
 * 
 * @author Markus Langenberg
 */
public abstract class ExtensionFileFilter extends FileFilter {

    public static class AllSupportedImageTypesFilter extends ExtensionFileFilter {

        public AllSupportedImageTypesFilter() {
            extensions = new String[] { "jpg", "jpeg", "png", "fts", "fits", "jp2", "jpx" };
        }

        public String getDescription() {
            return "All supported files (\".jpg\", \".jpeg\", \".png\", \".fts\", \".fits\", \".jp2\", \".jpx\")";
        }

    }

    public static class FitsFilter extends ExtensionFileFilter {

        public FitsFilter() {
            extensions = new String[] { "fits", "fts" };
        }

        public String getDescription() {
            return "FITS files (\".fts\", \".fits\")";
        }

    }

    public static class JP2Filter extends ExtensionFileFilter {

        public JP2Filter() {
            extensions = new String[] { "jp2", "jpx" };
        }

        public String getDescription() {
            return "JPG2000 files (\".jp2\", \".jpx\")";
        }
    }

    public class JPGFilter extends ExtensionFileFilter {

        public JPGFilter() {
            extensions = new String[] { "jpg", "jpeg" };
        }

        public String getDescription() {
           return "JPG files (\".jpg\", \".jpeg\")";
        }

    }

    public static class PNGFilter extends ExtensionFileFilter {

        public PNGFilter() {
            extensions = new String[] { "png" };
        }
        public String getDescription() {
            return "PNG files (\".png\")";
        }

    }

    protected String[] extensions = {};

    public boolean accept(File f) {
        if (f.isDirectory())
            return true;

        String testName = f.getName().toLowerCase();
        for (String ext : extensions) {
            if (testName.endsWith(ext))
                return true;
        }
        return false;
    }

    /**
     * Returns the default extensions of this file filter.
     * 
     * By default, the first element of the list of accepted extensions will be
     * considered as the default extension.
     * 
     * @return The default extension
     */
    public String getDefaultExtension() {
        if (extensions.length > 0) {
            return extensions[0];
        }
        return null;
    }

}
