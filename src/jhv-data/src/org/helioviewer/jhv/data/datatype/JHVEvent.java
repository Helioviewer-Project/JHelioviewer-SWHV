package org.helioviewer.jhv.data.datatype;

import java.util.Date;

/**
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 */
public interface JHVEvent {
    /**
     * 
     * @return
     */
    public abstract Date getStartDate();

    /**
     * 
     * @return
     */
    public abstract Date getEndDate();
}
