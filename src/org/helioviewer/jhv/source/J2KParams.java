package org.helioviewer.jhv.source;

class J2KParams {

    record Decode(int frame, int level) {}

    record Read(Decode decodeParams, boolean priority) {}

}
