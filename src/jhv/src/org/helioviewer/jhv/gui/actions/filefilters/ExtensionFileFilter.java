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
    protected String[] extensions = {};

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