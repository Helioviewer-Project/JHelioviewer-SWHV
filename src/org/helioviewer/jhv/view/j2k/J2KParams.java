package org.helioviewer.jhv.view.j2k;

class J2KParams {

    record Decode(int frame, int level) {}

    record Read(J2KView view, Decode decodeParams, boolean priority) {}

}
