package org.helioviewer.jhv.io;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public class ExtensionFileFilter {

    public static final FilenameFilter Image = new Filter(new ExtensionFilter(
            new String[]{"jpg", "jpeg", "png", "fts", "fits", "fits.gz", "jp2", "jpx"},
            "All supported files (\".jpg\", \".jpeg\", \".png\", \".fts\", \".fits\", \".fits.gz\", \".jp2\", \".jpx\")"));
    public static final FilenameFilter JHV = new Filter(new ExtensionFilter(
            new String[]{"jhv"},
            "State files (\".jhv\")"));
    public static final FilenameFilter JSON = new Filter(new ExtensionFilter(
            new String[]{"json"},
            "JSON files (\".json\")"));
    public static final FilenameFilter GGR = new Filter(new ExtensionFilter(
            new String[]{"ggr"},
            "GIMP gradient files (\".ggr\")"));

    private static class Filter implements FilenameFilter {

        private final FileFilter filter;

        Filter(FileFilter _filter) {
            filter = _filter;
        }

        @Override
        public boolean accept(File dir, String name) {
            return filter.accept(new File(dir, name));
        }

    }

    private static class ExtensionFilter extends FileFilter {

        private final String[] extensions;
        private final String description;

        ExtensionFilter(String[] _extensions, String _description) {
            extensions = _extensions;
            description = _description;
        }

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

        @Override
        public String getDescription() {
            return description;
        }

    }

}
