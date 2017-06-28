package org.helioviewer.jhv.gui.actions.filefilters;

import java.io.File;
import java.io.FilenameFilter;

public class AllFilenameFilter implements FilenameFilter {

    private final ExtensionFileFilter.AllSupportedImageTypesFilter fileFilter = new ExtensionFileFilter.AllSupportedImageTypesFilter();

    @Override
    public boolean accept(File dir, String name) {
        return fileFilter.accept(new File(dir.getName() + File.separator + name));
    }

}
