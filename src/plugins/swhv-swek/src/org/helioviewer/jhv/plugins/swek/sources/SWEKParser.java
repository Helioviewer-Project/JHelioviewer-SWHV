package org.helioviewer.jhv.plugins.swek.sources;

import java.io.InputStream;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;

public interface SWEKParser {

    /**
     * If called the parser should stop all parser operations.
     */
    public abstract void stopParser();

    /**
     * Parses the event stream.
     * 
     * @param downloadInputStream
     *            the stream containing the events in specific source format
     * @param eventType
     *            the type of event parsed
     * @param swekSource
     *            the source from which the events are parsed
     * @return the stream containing the events in standard jhelioviewer format
     */
    public abstract SWEKEventStream parseEventStream(InputStream downloadInputStream, SWEKEventType eventType, SWEKSource swekSource);

}
