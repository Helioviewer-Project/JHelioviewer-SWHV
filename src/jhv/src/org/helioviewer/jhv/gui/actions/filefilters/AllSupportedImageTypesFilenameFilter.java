package org.helioviewer.jhv.gui.actions.filefilters;

import java.io.File;
import java.io.FilenameFilter;

public class AllSupportedImageTypesFilenameFilter implements FilenameFilter {

    private static AllSupportedImageTypesFilter fileFilter;

    public AllSupportedImageTypesFilenameFilter() {
        fileFilter = new AllSupportedImageTypesFilter();
    }

    @Override
    public boolean accept(File dir, String name) {
        return fileFilter.accept(new File(dir.getName() + File.separator + name));
    }

}
