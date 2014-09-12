package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.InputStream;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;
import org.helioviewer.jhv.plugins.swek.sources.SWEKParser;

public class HEKParser implements SWEKParser {

    @Override
    public void stopParser() {
        // TODO Auto-generated method stub

    }

    @Override
    public SWEKEventStream parseEventStream(InputStream downloadInputStream, SWEKEventType eventType, SWEKSource swekSource) {
        // TODO Auto-generated method stub
        return null;
    }

}
