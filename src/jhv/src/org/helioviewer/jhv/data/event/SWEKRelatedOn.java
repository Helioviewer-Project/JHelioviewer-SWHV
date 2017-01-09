package org.helioviewer.jhv.data.event;

// Holds the related parameters of related events
public class SWEKRelatedOn {

    public final SWEKParameter parameterFrom;
    public final SWEKParameter parameterWith;
    public final String dbType;

    public SWEKRelatedOn(SWEKParameter _parameterFrom, SWEKParameter _parameterWith, String _dbType) {
        parameterFrom = _parameterFrom;
        parameterWith = _parameterWith;
        dbType = _dbType;
    }

}
