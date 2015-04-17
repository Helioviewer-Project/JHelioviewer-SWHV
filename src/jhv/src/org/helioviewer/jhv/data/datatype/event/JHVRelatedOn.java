package org.helioviewer.jhv.data.datatype.event;

/**
 * 
 * 
 * @author Bram Bourgoignie(Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVRelatedOn {
    /** The related on parameter of the from event */
    private JHVEventParameter relatedOnFrom;

    /** The related on parameter of the with event */
    private JHVEventParameter relatedOnWith;

    /**
     * Default constructor
     */
    public JHVRelatedOn() {

    }

    /**
     * Creates a related on for the given parameter coming from the from event,
     * and parameter from the related with event.
     * 
     * @param relatedOnFrom
     *            the related from parameter
     * @param relatedOnWith
     *            the related with parameter
     */
    public JHVRelatedOn(JHVEventParameter relatedOnFrom, JHVEventParameter relatedOnWith) {
        this.relatedOnFrom = relatedOnFrom;
        this.relatedOnWith = relatedOnWith;
    }

    /**
     * Gets the related on from parameter.
     * 
     * @return the related on from parameter
     */
    public JHVEventParameter getRelatedOnFrom() {
        return relatedOnFrom;
    }

    /**
     * Gets the related on with parameter.
     * 
     * @return the related on with parameter
     */
    public JHVEventParameter getRelatedOnWith() {
        return relatedOnWith;
    }

}
