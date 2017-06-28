package org.helioviewer.jhv.gui.actions.filefilters;

import java.io.File;
import java.io.FilenameFilter;

public class JsonFilenameFilter implements FilenameFilter {

    private final ExtensionFileFilter.JsonFilter fileFilter = new ExtensionFileFilter.JsonFilter();

    @Override
    public boolean accept(File dir, String name) {
        return fileFilter.accept(new File(dir.getName() + File.separator + name));
    }

}
