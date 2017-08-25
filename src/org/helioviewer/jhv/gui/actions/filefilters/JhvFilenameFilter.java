package org.helioviewer.jhv.gui.actions.filefilters;

import java.io.File;
import java.io.FilenameFilter;

public class JhvFilenameFilter implements FilenameFilter {

    private final ExtensionFileFilter.JhvFilter fileFilter = new ExtensionFileFilter.JhvFilter();

    @Override
    public boolean accept(File dir, String name) {
        return fileFilter.accept(new File(dir.getName() + File.separator + name));
    }

}
