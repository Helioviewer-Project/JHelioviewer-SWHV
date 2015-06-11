package org.helioviewer.jhv.display;

import java.awt.image.BufferedImage;

public interface CaptureListener {

    public void start();

    public void stop();

    public BufferedImage capture();

}
