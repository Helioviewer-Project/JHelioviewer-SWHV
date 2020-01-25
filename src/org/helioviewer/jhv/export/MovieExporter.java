package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

interface MovieExporter {

    void encode(BufferedImage image) throws Exception;

    void close() throws Exception;

    String getPath();

    int getHeight();

}
