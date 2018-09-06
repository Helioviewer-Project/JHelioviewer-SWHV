package org.helioviewer.jhv.view.jp2view.image;

import org.helioviewer.jhv.position.Position;

public class ImageParams {

    public boolean priority;
    public final Position viewpoint;
    public final DecodeParams decodeParams;

    public ImageParams(boolean _priority, Position _viewpoint, DecodeParams _decodeParams) {
        priority = _priority;
        viewpoint = _viewpoint;
        decodeParams = _decodeParams;
    }

}
