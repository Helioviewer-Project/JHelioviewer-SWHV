package org.helioviewer.jhv.astronomy;

import spice.basic.ReferenceFrame;
import spice.basic.SpiceException;

public enum Frame {
    HCI, HEEQ, HEE;

    public final ReferenceFrame frame;

    private Frame() {
        try {
            frame = new ReferenceFrame(toString());
        } catch (SpiceException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

}
