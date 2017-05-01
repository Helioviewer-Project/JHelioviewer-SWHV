package org.helioviewer.jhv.viewmodel.imageformat;

public class SingleChannelImageFormat implements ImageFormat {

    private final int bitDepth;

    public SingleChannelImageFormat(int _bitDepth) {
        bitDepth = _bitDepth;
    }

    public int getBitDepth() {
        return bitDepth;
    }

}
