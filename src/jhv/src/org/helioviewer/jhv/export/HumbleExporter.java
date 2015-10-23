package org.helioviewer.jhv.export;

import io.humble.video.Codec;
import io.humble.video.Encoder;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.Muxer;
import io.humble.video.MuxerFormat;
import io.humble.video.PixelFormat;
import io.humble.video.Rational;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import java.awt.image.BufferedImage;

//Using jars from http://mvnrepository.com/artifact/io.humble/humble-video-all
public class HumbleExporter implements MovieExporter {
    private Muxer muxer;
    private MuxerFormat format;
    private Codec codec;
    private Encoder encoder;
    private MediaPicture picture;
    private MediaPacket packet;
    private int framenumber;

    @Override
    public void open(String path, int w, int h, int fps) throws Exception {
        final Rational framerate = Rational.make(1, fps);
        framenumber = 0;
        packet = MediaPacket.make();
        muxer = Muxer.make(path, null, null);
        format = muxer.getFormat();
        codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());
        encoder = Encoder.make(codec);

        encoder.setWidth(w);
        encoder.setHeight(h);
        final PixelFormat.Type pixelformat = PixelFormat.Type.PIX_FMT_YUV420P;
        encoder.setPixelFormat(pixelformat);
        encoder.setTimeBase(framerate);
        if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER))
            encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
        encoder.open(null, null);
        muxer.addNewStream(encoder);
        muxer.open(null, null);

        picture = MediaPicture.make(encoder.getWidth(), encoder.getHeight(), pixelformat);
        picture.setTimeBase(framerate);

    }

    @Override
    public void encode(BufferedImage im) throws Exception {
        MediaPictureConverter converter = MediaPictureConverterFactory.createConverter(im, picture);
        converter.toPicture(picture, im, framenumber++);

        do {
            encoder.encode(packet, picture);
            if (packet.isComplete())
                muxer.write(packet, false);
        } while (packet.isComplete());
    }

    @Override
    public void close() throws Exception {
        do {
            encoder.encode(packet, null);
            if (packet.isComplete())
                muxer.write(packet, false);
        } while (packet.isComplete());
        muxer.close();
        muxer.delete();
        encoder.delete();
        codec.delete();
        format.delete();
        packet.delete();
        muxer = null;
        encoder = null;
        codec = null;
        format = null;
        packet = null;
    }
}
