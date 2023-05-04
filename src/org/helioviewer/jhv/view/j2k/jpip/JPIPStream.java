package org.helioviewer.jhv.view.j2k.jpip;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class JPIPStream implements Serializable {

    @Serial
    private static final long serialVersionUID = JPIPSegment.serialVersionUID;

    public final ArrayList<JPIPSegment> segments = new ArrayList<>();

}
