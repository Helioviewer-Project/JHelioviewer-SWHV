package org.helioviewer.jhv.plugins.swek.sources;

import org.helioviewer.jhv.data.datatype.event.SWEKDownloader;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepDownloader;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKDownloader;

public class SWEKSourceManager {

    public static SWEKDownloader getDownloader(SWEKSource swekSource) {
        if (swekSource.getSourceName().equals("COMESEP"))
            return new ComesepDownloader();
        else if (swekSource.getSourceName().equals("HEK"))
            return new HEKDownloader();
        else
            return null;
    }

}
