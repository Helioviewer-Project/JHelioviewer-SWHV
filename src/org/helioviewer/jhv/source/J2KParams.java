package org.helioviewer.jhv.source;

class J2KParams {

    record Decode(int frame, int level) {}

    record Read(J2KView view, Decode decodeParams, boolean priority) {}

}
