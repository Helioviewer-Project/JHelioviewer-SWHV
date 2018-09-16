package org.helioviewer.jhv.view.j2k.image;

public class ImageParams {

    public boolean priority;
    public final DecodeParams decodeParams;

    public ImageParams(boolean _priority, DecodeParams _decodeParams) {
        priority = _priority;
        decodeParams = _decodeParams;
    }

}
