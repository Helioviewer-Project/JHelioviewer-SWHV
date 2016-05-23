package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

import org.helioviewer.jhv.base.ImageUtils;

public class PNGExporter implements MovieExporter {

    private String path;
    private int height;
    private BufferedImage image;

    @Override
    public void open(String path, int w, int h, int fps) throws Exception {
        this.path = path;
        height = h;
    }

    @Override
    public void encode(BufferedImage image) throws Exception {
        this.image = image;
    }

    @Override
    public void close() throws Exception {
        ImageUtils.writePNG(image, path);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int getHeight() {
        return height;
    }

}
