package org.helioviewer.jhv.view.j2k.image;

import org.helioviewer.jhv.view.j2k.J2KView;

public class ReadParams {

    public final J2KView view;
    public final DecodeParams decodeParams;
    public boolean priority;

    public ReadParams(J2KView _view, DecodeParams _decodeParams, boolean _priority) {
        view = _view;
        decodeParams = _decodeParams;
        priority = _priority;
    }

}
