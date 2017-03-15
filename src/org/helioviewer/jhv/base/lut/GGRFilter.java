package org.helioviewer.jhv.base.lut;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Filter for gimp gradient files ggr
 * 
 * @author Helge Dietert
 * 
 */
class GGRFilter extends FileFilter implements java.io.FileFilter {

    private static final String[] extensions = { "ggr" };

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
        return "GIMP gradient files (\".ggr\")";
    }

}
