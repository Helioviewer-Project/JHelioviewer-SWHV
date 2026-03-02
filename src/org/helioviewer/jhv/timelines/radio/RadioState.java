package org.helioviewer.jhv.timelines.radio;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisPositiveIdentityScale;

final class RadioState {

    static final RadioState INSTANCE = new RadioState();

    private final YAxis yAxis = new YAxis(400, 20, new YAxisPositiveIdentityScale("MHz"));
    private IndexColorModel colorModel;

    private RadioState() {
    }

    void setLUT(LUT lut) {
        int[] source = lut.lut8();
        colorModel = new IndexColorModel(8, source.length, source, 0, false, -1, DataBuffer.TYPE_BYTE);
    }

    YAxis yAxis() {
        return yAxis;
    }

    IndexColorModel colorModel() {
        return colorModel;
    }
}
