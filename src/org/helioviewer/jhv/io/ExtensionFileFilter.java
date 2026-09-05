package org.helioviewer.jhv.io;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFileFilter {

    public static final FilenameFilter Image = new Filter(new String[]{"jpg", "jpeg", "png", "fts", "fits", "fits.gz", "jp2", "jpx", "zip"});
    public static final FilenameFilter Model = new Filter(new String[]{"gltf", "glb", "gltf.gz", "glb.gz"});
    public static final FilenameFilter Timeline = new Filter(new String[]{"json", "cdf"});
    public static final FilenameFilter JHV = new Filter(new String[]{"jhv"});

    private record Filter(String[] extensions) implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            File file = new File(dir, name);
            if (file.isDirectory())
                return true;

            String testName = file.getName().toLowerCase();
            for (String ext : extensions) {
                if (testName.endsWith("." + ext))
                    return true;
            }
            return false;
        }

    }

}
