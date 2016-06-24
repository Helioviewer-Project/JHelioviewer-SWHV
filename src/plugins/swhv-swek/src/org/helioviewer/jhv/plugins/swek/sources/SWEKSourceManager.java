package org.helioviewer.jhv.plugins.swek.sources;

import org.helioviewer.jhv.data.datatype.event.SWEKDownloader;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepDownloader;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepParser;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKParser;

public class SWEKSourceManager {

    public static SWEKDownloader getDownloader(SWEKSource swekSource) {
        if (swekSource.getSourceName().equals("COMESEP"))
            return new ComesepDownloader();
        else if (swekSource.getSourceName().equals("HEK"))
            return new HEKDownloader();
        else
            return null;

    }

    public static SWEKParser getParser(SWEKSource swekSource) {
        if (swekSource.getSourceName().equals("COMESEP"))
            return new ComesepParser();
        else if (swekSource.getSourceName().equals("HEK"))
            return new HEKParser();
        else
            return null;
    }
}
