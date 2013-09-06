package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Filter for gimp gradient files ggr
 * 
 * @author Helge Dietert
 * 
 */
public class GGRFilter extends FileFilter implements java.io.FileFilter {
    private final String[] extensions = { "ggr" };

    /**
     * {@inheritDoc}
     */
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
     * {@inheritDoc}
     */
    public String getDescription() {
        return "GIMP gradient files (\".ggr\")";
    }
}
