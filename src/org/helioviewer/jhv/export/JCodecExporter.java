package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.export.jcodec.JHVSequenceEncoder;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.movtool.Flattern;

public class JCodecExporter implements MovieExporter {

    private JHVSequenceEncoder encoder;
    private String path;
    private int height;

    @Override
    public void open(String _path, int w, int h, int fps) throws Exception {
        path = _path;
        height = h;
        encoder = new JHVSequenceEncoder(new File(path), w, h, fps);
    }

    @Override
    public void encode(BufferedImage image) throws Exception {
        encoder.encodeNativeFrame(image);
    }

    @Override
    public void close() throws Exception {
        encoder.finish();
        prepareStream();
    }

    private void prepareStream() throws Exception {
        File orig = new File(path);
        MovieBox movie = MP4Util.createRefMovie(orig);

        File optim = new File(path + "_optim");
        new Flattern().flattern(movie, optim);
        FileUtils.copy(optim, orig); // renameTo doesn't work on Windows
        optim.delete();
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
