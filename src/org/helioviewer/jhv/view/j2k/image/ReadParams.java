package org.helioviewer.jhv.view.j2k.image;

public class ReadParams {

    public boolean priority;
    public final DecodeParams decodeParams;

    public ReadParams(boolean _priority, DecodeParams _decodeParams) {
        priority = _priority;
        decodeParams = _decodeParams;
    }

}
