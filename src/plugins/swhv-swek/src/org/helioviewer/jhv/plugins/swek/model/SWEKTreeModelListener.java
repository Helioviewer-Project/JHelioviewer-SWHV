package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public interface SWEKTreeModelListener {
    /**
     * 
     */
    public abstract void expansionChanged();

    /**
     * 
     * @param eventType
     */
    public abstract void startedDownloadingEventType(SWEKEventType eventType);

    /**
     * 
     * @param eventType
     */
    public abstract void stoppedDownloadingEventType(SWEKEventType eventType);
}
