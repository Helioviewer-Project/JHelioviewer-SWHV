package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;

import org.helioviewer.jhv.export.jcodec.JHVSequenceEncoder;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
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
        SeekableByteChannel input = NIOUtils.readableFileChannel(orig);
        MovieBox movie = MP4Util.createRefMovie(input, "file://" + orig.getCanonicalPath());

        File optim = new File(path + "_optim");
        new Flattern().flattern(movie, optim);
        input.close();
        optim.renameTo(orig);
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
