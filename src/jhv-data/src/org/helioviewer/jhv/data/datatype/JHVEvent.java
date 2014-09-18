package org.helioviewer.jhv.data.datatype;

import java.util.Date;
import java.util.List;

import javax.swing.Icon;

/**
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 */
public interface JHVEvent {
    /**
     * Gets the start date of the event
     * 
     * @return the start date of the event
     */
    public abstract Date getStartDate();

    /**
     * Gets the end date of the event
     * 
     * @return the end date of the event
     */
    public abstract Date getEndDate();

    /**
     * Gets the name of the event.
     * 
     * @return the name
     */
    public abstract String getName();

    /**
     * Gets the display name of the event.
     * 
     * @return the display name
     */
    public abstract String getDisplayName();

    /**
     * Gets the icon of the event
     * 
     * @return the icon
     */
    public abstract Icon getIcon();

    /**
     * Gets the short description of the event.
     * 
     * @return the short description
     */
    public abstract String getShortDescription();

    /**
     * Gets a list with all the event parameters.
     * 
     * @return a list with all the event parameters
     */
    public abstract List<JHVEventParameter> getAllEventParameters();

    /**
     * Gets a list with all the visible configured event parameters.
     * 
     * @return a list with all the visible event parameters
     */
    public abstract List<JHVEventParameter> getVisibleEventParameters();

    /**
     * Gets a list with all the visible configured parameters that were not
     * null.
     * 
     * @return a list with visible parameters that are not null
     */
    public abstract List<JHVEventParameter> getVisibleNotNullEventParameters();

    /**
     * Gets a list with all the visible configured parameters that were null.
     * 
     * @return a list with all the visible null parameters
     */
    public abstract List<JHVEventParameter> getVisibleNullEventParameters();

    /**
     * Gets a list with all the non visible configured parameters.
     * 
     * @return a list with all the non visible parameters
     */
    public abstract List<JHVEventParameter> getNonVisibleEventParameters();

    /**
     * Gets a list with all the non visible configured parameters that were not
     * null.
     * 
     * @return a list with all not null non visible parameters
     */
    public abstract List<JHVEventParameter> getNonVisibleNotNullEventParameters();

    /**
     * Gets a list with all the non visible configured parameters that were
     * null.
     * 
     * @return a list with all null non visible parameters
     */
    public abstract List<JHVEventParameter> getNonVisibleNullEventParameters();

    /**
     * Gets the event type of the event.
     * 
     * @return the event type of the event
     */
    public abstract JHVEventType getJHVEventType();
}
