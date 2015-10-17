package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

public interface MovieInterface {

    public void open(String path, int w, int h, float fps);

    public void encode(BufferedImage im, int frame);

    public void close();

}
