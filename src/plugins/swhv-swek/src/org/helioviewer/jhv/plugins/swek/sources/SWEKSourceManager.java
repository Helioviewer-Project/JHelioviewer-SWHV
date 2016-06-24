package org.helioviewer.jhv.plugins.swek.sources;

import org.helioviewer.jhv.data.datatype.event.SWEKDownloader;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepDownloader;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepParser;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKParser;

/**
 * Manages all the downloaders and downloads of the SWEK plugin.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKSourceManager {

    private static SWEKSourceManager instance;
    private static String ComesepSource = "COMESEP";
    private static String HekSource = "HEK";

    private SWEKSourceManager() {
    }

    public static SWEKSourceManager getSingletonInstance() {
        if (instance == null) {
            instance = new SWEKSourceManager();
        }
        return instance;
    }

    public SWEKDownloader getDownloader(SWEKSource swekSource) {
        if (swekSource.getSourceName().equals(ComesepSource))
            return new ComesepDownloader();
        else if (swekSource.getSourceName().equals(HekSource))
            return new HEKDownloader();
        else
            return null;

    }

    public SWEKParser getParser(SWEKSource swekSource) {
        if (swekSource.getSourceName().equals(ComesepSource))
            return new ComesepParser();
        else if (swekSource.getSourceName().equals(HekSource))
            return new HEKParser();
        else
            return null;
    }
}
