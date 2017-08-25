package org.helioviewer.jhv.gui.actions.filefilters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

abstract class ExtensionFileFilter extends FileFilter {

    public static class AllSupportedImageTypesFilter extends ExtensionFileFilter {

        public AllSupportedImageTypesFilter() {
            extensions = new String[] { "jpg", "jpeg", "png", "fts", "fits", "jp2", "jpx" };
        }

        @Override
        public String getDescription() {
            return "All supported files (\".jpg\", \".jpeg\", \".png\", \".fts\", \".fits\", \".jp2\", \".jpx\")";
        }

    }

    static class JsonFilter extends ExtensionFileFilter {

        JsonFilter() {
            extensions = new String[] { "json" };
        }

        @Override
        public String getDescription() {
            return "JSON files (\".json\")";
        }

    }

    static class JhvFilter extends ExtensionFileFilter {

        JhvFilter() {
            extensions = new String[] { "jhv" };
        }

        @Override
        public String getDescription() {
            return "State files (\".jhv\")";
        }

    }

    private static class FitsFilter extends ExtensionFileFilter {

        private FitsFilter() {
            extensions = new String[] { "fits", "fts" };
        }

        @Override
        public String getDescription() {
            return "FITS files (\".fts\", \".fits\")";
        }

    }

    private static class JP2Filter extends ExtensionFileFilter {

        private JP2Filter() {
            extensions = new String[] { "jp2", "jpx" };
        }

        @Override
        public String getDescription() {
            return "JPG2000 files (\".jp2\", \".jpx\")";
        }
    }

    private static class JPGFilter extends ExtensionFileFilter {

        private JPGFilter() {
            extensions = new String[] { "jpg", "jpeg" };
        }

        @Override
        public String getDescription() {
           return "JPG files (\".jpg\", \".jpeg\")";
        }

    }

    private static class PNGFilter extends ExtensionFileFilter {

        private PNGFilter() {
            extensions = new String[] { "png" };
        }

        @Override
        public String getDescription() {
            return "PNG files (\".png\")";
        }

    }

    String[] extensions = {};

    @Override
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
