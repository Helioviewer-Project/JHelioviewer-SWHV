package org.helioviewer.jhv.gui;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public abstract class ExtensionFileFilter extends FileFilter {

    public static final FilenameFilter Image = new Image();
    public static final FilenameFilter Jhv = new Jhv();
    public static final FilenameFilter Json = new Json();

    private static class Image implements FilenameFilter {

        private final FileFilter filter = new ExtensionFileFilter.ImageTypesFilter();

        @Override
        public boolean accept(File dir, String name) {
            return filter.accept(new File(dir.getName(), name));
        }

    }

    private static class Jhv implements FilenameFilter {

        private final FileFilter filter = new ExtensionFileFilter.JhvFilter();

        @Override
        public boolean accept(File dir, String name) {
            return filter.accept(new File(dir.getName(), name));
        }

    }

    private static class Json implements FilenameFilter {

        private final FileFilter filter = new ExtensionFileFilter.JsonFilter();

        @Override
        public boolean accept(File dir, String name) {
            return filter.accept(new File(dir.getName(), name));
        }

    }

    private static class ImageTypesFilter extends ExtensionFileFilter {

        ImageTypesFilter() {
            extensions = new String[] { "jpg", "jpeg", "png", "fts", "fits", "jp2", "jpx" };
        }

        @Override
        public String getDescription() {
            return "All supported files (\".jpg\", \".jpeg\", \".png\", \".fts\", \".fits\", \".jp2\", \".jpx\")";
        }

    }

    private static class JsonFilter extends ExtensionFileFilter {

        JsonFilter() {
            extensions = new String[] { "json" };
        }

        @Override
        public String getDescription() {
            return "JSON files (\".json\")";
        }

    }

    private static class JhvFilter extends ExtensionFileFilter {

        JhvFilter() {
            extensions = new String[] { "jhv", "jhvz" };
        }

        @Override
        public String getDescription() {
            return "State files (\".jhv\", \".jhvz\")";
        }

    }

    protected String[] extensions = {};

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
