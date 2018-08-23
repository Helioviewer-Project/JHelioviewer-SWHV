package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.io.Serializable;
import java.util.ArrayList;

public class JPIPStream implements Serializable {

    private static final long serialVersionUID = JPIPSegment.serialVersionUID;

    public final ArrayList<JPIPSegment> segments = new ArrayList<>();

}
