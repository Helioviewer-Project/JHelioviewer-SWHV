package org.helioviewer.jhv.timelines.propagation;

public interface PropagationModel {

    boolean isPropagated();

    long getDepropagatedTime(long ts);

    long getPropagatedTime(long ts);

}
