package org.helioviewer.jhv.export;

import java.io.File;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class PNGExporter implements MovieExporter {

    private String path;
    private int height;
    private BufferedImage image;

    @Override
    public void open(String _path, int w, int h, int fps) throws Exception {
        path = _path;
        height = h;
    }

    @Override
    public void encode(BufferedImage _image) throws Exception {
        image = _image;
    }

    @Override
    public void close() throws Exception {
        // ImageUtils.writePNG(image, path);
        ImageIO.write(image, "png", new File(path));
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
