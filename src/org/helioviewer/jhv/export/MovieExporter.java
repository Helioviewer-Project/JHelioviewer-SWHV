package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

interface MovieExporter {

    void open(String path, int w, int h, int fps) throws Exception;

    Object transform(BufferedImage image);

    void encode(Object frame) throws Exception;

    void close() throws Exception;

    String getPath();

    int getHeight();

}
