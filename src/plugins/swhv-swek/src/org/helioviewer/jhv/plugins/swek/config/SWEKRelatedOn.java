package org.helioviewer.jhv.plugins.swek.config;

/**
 * Holds the related parameters of related events
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKRelatedOn {

    /** The parameter from the source event  */
    private final SWEKParameter parameterFrom;

    /** The parameter from the related event */
    private final SWEKParameter parameterWith;

    /**
     * Creates a related on with the given from and with event parameters.
     *
     * @param parameterFrom     The parameter from the source event
     * @param parameterWith     The parameter from the related event
     */
    public SWEKRelatedOn(SWEKParameter parameterFrom, SWEKParameter parameterWith) {
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
     * Gets the related event parameter.
     *
     * @return the parameterWith
     */
    public SWEKParameter getParameterWith() {
        return parameterWith;
    }

}
