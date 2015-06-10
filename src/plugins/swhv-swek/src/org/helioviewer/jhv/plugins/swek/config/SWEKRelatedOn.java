package org.helioviewer.jhv.plugins.swek.config;

/**
 * Holds the related parameters of related events
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKRelatedOn {

    /** The parameter from the source event  */
    private SWEKParameter parameterFrom;

    /** The parameter from the related event */
    private SWEKParameter parameterWith;

    /**
     * Creates a related on with null parameter for the from and null parameter for the with.
     */
    public SWEKRelatedOn() {
        super();
        this.parameterFrom = null;
        this.parameterWith = null;
    }

    /**
     * Creates a related on with the given from and with event parameters.
     *
     * @param parameterFrom     The parameter from the source event
     * @param parameterWith     The parameter from the related event
     */
    public SWEKRelatedOn(SWEKParameter parameterFrom, SWEKParameter parameterWith) {
        super();
        this.parameterFrom = parameterFrom;
        this.parameterWith = parameterWith;
    }

    /**
     * Gets the source event parameter.
     *
     * @return the parameterFrom
     */
    public SWEKParameter getParameterFrom() {
        return parameterFrom;
    }

    /**
     * Sets the source event parameter.
     *
     * @param parameterFrom the parameterFrom to set
     */
    public void setParameterFrom(SWEKParameter parameterFrom) {
        this.parameterFrom = parameterFrom;
    }

    /**
     * Gets the related event parameter.
     *
     * @return the parameterWith
     */
    public SWEKParameter getParameterWith() {
        return parameterWith;
    }

    /**
     * Sets the related event parameter.
     *
     * @param parameterWith the parameterWith to set
     */
    public void setParameterWith(SWEKParameter parameterWith) {
        this.parameterWith = parameterWith;
    }

}
