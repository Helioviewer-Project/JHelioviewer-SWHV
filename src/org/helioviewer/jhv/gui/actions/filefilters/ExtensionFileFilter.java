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
            extensions = new String[] { "jhv", "jhvz" };
        }

        @Override
        public String getDescription() {
            return "State files (\".jhv\", \".jhvz\")";
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

}
