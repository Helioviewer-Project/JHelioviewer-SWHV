package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

interface MovieExporter {

    Object transform(BufferedImage image);

    void encode(Object frame) throws Exception;

    void close() throws Exception;

    String getPath();

    int getHeight();

}
