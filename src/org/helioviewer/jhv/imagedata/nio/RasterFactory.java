package org.helioviewer.jhv.imagedata.nio;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

public interface RasterFactory {
    WritableRaster createRaster(SampleModel model, DataBuffer buffer, Point origin);

    RasterFactory factory = GenericWritableRaster::new;
}
