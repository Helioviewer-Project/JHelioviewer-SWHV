package org.helioviewer.jhv.timelines.propagation;

public interface PropagationModel {

    long getDepropagatedTime(long ts);

    long getPropagatedTime(long ts);

}
