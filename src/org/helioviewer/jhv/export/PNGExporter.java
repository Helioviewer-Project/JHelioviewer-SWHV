package org.helioviewer.jhv.export;

import java.io.IOException;
import java.io.File;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class PNGExporter implements MovieExporter {

    private String path;
    private int height;
    private BufferedImage image;

    @Override
    public void open(String _path, int w, int h, int fps) {
        path = _path;
        height = h;
    }

    @Override
    public Object transform(BufferedImage _image) {
        return _image;
    }

    @Override
    public void encode(Object frame) throws IOException {
        if (!(frame instanceof BufferedImage))
            throw new IOException("Not Picture");
        image = (BufferedImage) frame;
    }

    @Override
    public void close() throws IOException {
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
