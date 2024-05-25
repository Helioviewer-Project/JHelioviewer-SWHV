package org.helioviewer.jhv.imagedata.nio;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

public interface RasterFactory {

    WritableRaster createRaster(SampleModel model, DataBuffer buffer, Point origin);

    RasterFactory factory = createRasterFactory();

    private static RasterFactory createRasterFactory() {
        return new GenericRasterFactory();
    }

    // Generic implementation that should work for any JRE, and creates a custom subclass of {@link WritableRaster}.
    class GenericRasterFactory implements RasterFactory {
        @Override
        public WritableRaster createRaster(SampleModel model, DataBuffer buffer, Point origin) {
            return new GenericWritableRaster(model, buffer, origin);
        }
    }

}
