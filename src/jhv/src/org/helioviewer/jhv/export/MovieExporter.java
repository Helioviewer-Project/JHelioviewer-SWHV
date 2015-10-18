package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

public interface MovieExporter {

    public void open(String path, int w, int h, int fps) throws Exception;

    public void encode(BufferedImage im) throws Exception;

    public void close() throws Exception;

}
