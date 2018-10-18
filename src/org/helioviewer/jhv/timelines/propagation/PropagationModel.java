package org.helioviewer.jhv.timelines.propagation;

public interface PropagationModel {

    boolean isPropagated();

    long getObservationTime(long ts);

    long getViewpointTime(long ts);

}
