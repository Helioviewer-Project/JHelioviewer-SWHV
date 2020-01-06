package org.helioviewer.jhv.export.jcodec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jcodec.codecs.h264.encode.RateControl;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.movtool.Flattern;

public class JCodecUtils {

    public static void webOptimize(String path) throws IOException {
        File orig = new File(path);
        SeekableByteChannel input = NIOUtils.readableFileChannel(orig);
        MovieBox movie = MP4Util.createRefMovie(input, "file://" + orig.getCanonicalPath());

        File optim = new File(path + "_optim");
        new Flattern().flattern(movie, optim);
        input.close();

        Files.move(Path.of(path + "_optim"), Path.of(path), StandardCopyOption.REPLACE_EXISTING); // doesn't work on DOS
    }

    public static class JHVRateControl implements RateControl {

        private final int QP;

        public JHVRateControl(int _QP) {
            QP = _QP;
        }

        @Override
        public int getInitQp() {
            return QP;
        }

        @Override
        public int getQpDelta() {
            return 0;
        }

        @Override
        public boolean accept(int bits) {
            return true;
        }

        @Override
        public void reset() {
            // Do nothing, remember we are dumb
        }
    }

}
