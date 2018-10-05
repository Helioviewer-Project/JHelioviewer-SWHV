package org.helioviewer.jhv.timelines.propagation;

public interface PropagationModel {

    boolean isPropagated();

    long getInsituTime(long ts);

    long getViewpointTime(long ts);

}
