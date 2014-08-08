package org.helioviewer.jhv.plugins.swek.sources;

import java.io.InputStream;

public interface SWEKParser {

    /**
     * If called the parser should stop all parser operations.
     */
    public abstract void stopParser();

    /**
     * Parses the event stream.
     * 
     * @param downloadInputStream
     *            The stream containing the events in specific source format
     * @return the stream containing the events in standard jhelioviewer format
     */
    public abstract SWEKEventStream parseEventStream(InputStream downloadInputStream);

}
