package org.helioviewer.jhv.events;

// Holds the related parameters of related events
public class SWEKRelatedOn {

    public final SWEKParameter parameterFrom;
    public final SWEKParameter parameterWith;
    final String dbType;

    public SWEKRelatedOn(SWEKParameter _parameterFrom, SWEKParameter _parameterWith, String _dbType) {
        parameterFrom = _parameterFrom;
        parameterWith = _parameterWith;
        dbType = _dbType;
    }

}
