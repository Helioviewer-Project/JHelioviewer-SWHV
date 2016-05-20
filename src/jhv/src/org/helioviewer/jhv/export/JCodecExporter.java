package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;

import org.helioviewer.jhv.base.FileUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.movtool.Flattern;
import org.jcodec.scale.AWTUtil;

public class JCodecExporter implements MovieExporter {

    private JHVSequenceEncoder encoder;
    private Picture picture;
    private String path;

    @Override
    public void open(String path, int w, int h, int fps) throws Exception {
        this.path = path;
        encoder = new JHVSequenceEncoder(new File(path), w, h, fps);
        picture = Picture.create(w, h, ColorSpace.RGB);
    }

    @Override
    public void encode(BufferedImage image) throws Exception {
        AWTUtil.fromBufferedImage(image, picture);
        encoder.encodeNativeFrame(picture);
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

}
