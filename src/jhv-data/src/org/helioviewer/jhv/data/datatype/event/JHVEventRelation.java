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
     * @return the uniqueIdentifier
     */
    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    /**
     * @param uniqueIdentifier
     *            the uniqueIdentifier to set
     */
    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    /**
     * @return the theEvent
     */
    public JHVEvent getTheEvent() {
        return theEvent;
    }

    /**
     * @param theEvent
     *            the theEvent to set
     */
    public void setTheEvent(JHVEvent theEvent) {
        this.theEvent = theEvent;
    }
}
