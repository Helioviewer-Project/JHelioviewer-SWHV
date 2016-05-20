package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;

import org.jcodec.api.awt.AWTSequenceEncoder8Bit;

public class JCodecExporter implements MovieExporter {

    private AWTSequenceEncoder8Bit encoder;

    @Override
    public void open(String path, int w, int h, int fps) throws Exception {
        encoder = AWTSequenceEncoder8Bit.createSequenceEncoder8Bit(new File(path), fps);
    }

    @Override
    public void encode(BufferedImage image) throws Exception {
        encoder.encodeImage(image);
    }

    @Override
    public void close() throws Exception {
        encoder.finish();
    }

}
