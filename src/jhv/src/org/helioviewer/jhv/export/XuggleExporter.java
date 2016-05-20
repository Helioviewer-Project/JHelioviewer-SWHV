package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class XuggleExporter implements MovieExporter {

    private static final int MIN_BITRATE = 2000000;
    private static final double BPP = 1;

    private IMediaWriter movieWriter;
    private IConverter converter;
    private long frameNo;
    private double deltat;

    @Override
    public void open(String path, int w, int h, int fps) throws Exception {
        int bitRate = (int) Math.max(w * h * fps * BPP, MIN_BITRATE);
        IRational frameRate = IRational.make(fps, 1);
        deltat = 1e6 / frameRate.getDouble();

        movieWriter = ToolFactory.makeWriter(path);
        movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, w, h);

        IPixelFormat.Type pixFmt = IPixelFormat.Type.YUV420P;
        converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, pixFmt, w, h);

        IStreamCoder coder = movieWriter.getContainer().getStream(0).getStreamCoder();
        coder.setBitRate(bitRate);
        coder.setFrameRate(frameRate);
        coder.setTimeBase(IRational.make(frameRate.getDenominator(), frameRate.getNumerator()));
        coder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        coder.setGlobalQuality(0);
        // coder.setNumPicturesInGroupOfPictures(fps);
        coder.setPixelType(pixFmt);

        frameNo = 0;
    }

    @Override
    public void encode(BufferedImage image) throws Exception {
        IVideoPicture frame = converter.toPicture(image, (long) (frameNo * deltat));
        frame.setQuality(0);

        movieWriter.encodeVideo(0, frame);
        frameNo++;
    }

    @Override
    public void close() throws Exception {
        movieWriter.close();
        converter.delete();
    }

}
