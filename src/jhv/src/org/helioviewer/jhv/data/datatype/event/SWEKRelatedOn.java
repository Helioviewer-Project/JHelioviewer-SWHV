package org.helioviewer.jhv.data.datatype.event;

/**
 * Holds the related parameters of related events
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKRelatedOn {

    public final SWEKParameter parameterFrom;
    public final SWEKParameter parameterWith;
    public final String dbType;

    public SWEKRelatedOn(SWEKParameter parameterFrom, SWEKParameter parameterWith, String dbType) {
        this.parameterFrom = parameterFrom;
        this.parameterWith = parameterWith;
        this.dbType = dbType;
    }

}
