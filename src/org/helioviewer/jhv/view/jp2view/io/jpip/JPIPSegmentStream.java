package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;

class JPIPSegmentStream implements Serializable  {

    private static final long serialVersionUID = JPIPSegment.serialVersionUID;

    public int level = Integer.MAX_VALUE;
    public final ConcurrentLinkedQueue<JPIPSegment> segments = new ConcurrentLinkedQueue<>();

}
