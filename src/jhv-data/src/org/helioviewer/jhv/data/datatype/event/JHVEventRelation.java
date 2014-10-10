/**
 * 
 */
package org.helioviewer.jhv.data.datatype.event;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVEventRelation {
    /** The unique identifier on which events are related */
    private String uniqueIdentifier;
    /** The event of the relation */
    private JHVEvent theEvent;

    /**
     * Default constructor.
     */
    public JHVEventRelation() {
        uniqueIdentifier = null;
        theEvent = null;
    }

    /**
     * Creates a relation based on the unique identifier.
     * 
     * @param uniqueIdentifier
     *            the unique identifier
     */
    public JHVEventRelation(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
        theEvent = null;
    }

    /**
     * Creates a relation based on the related event.
     * 
     * @param event
     *            the event
     */
    public JHVEventRelation(JHVEvent event) {
        theEvent = event;
        uniqueIdentifier = event.getUniqueID();
    }

    /**
     * Creates a relation based on the event and the unique identifier.
     * 
     * @param uniqueIdentifier
     *            the unique identifier
     * @param event
     *            the event
     */
    public JHVEventRelation(String uniqueIdentifier, JHVEvent event) {
        theEvent = event;
        this.uniqueIdentifier = uniqueIdentifier;
    }

    /**
     * Gets the unique identifier of the related event.
     * 
     * @return the uniqueIdentifier
     */
    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    /**
     * Sets the unique identifier of the relation.
     * 
     * @param uniqueIdentifier
     *            the uniqueIdentifier to set
     */
    public void setUniqueIdentifier(String uniqueIdentifier) {
        if (!theEvent.getUniqueID().equals(uniqueIdentifier)) {
            theEvent = null;
        }
        this.uniqueIdentifier = uniqueIdentifier;
    }

    /**
     * Gets the event of the relation.
     * 
     * @return the theEvent
     */
    public JHVEvent getTheEvent() {
        return theEvent;
    }

    /**
     * Sets the event of the relation.
     * 
     * @param theEvent
     *            the theEvent to set
     */
    public void setTheEvent(JHVEvent theEvent) {
        uniqueIdentifier = theEvent.getUniqueID();
        this.theEvent = theEvent;
    }
}
