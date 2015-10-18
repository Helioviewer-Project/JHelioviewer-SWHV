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

    private IMediaWriter movieWriter;
    private IConverter converter;
    private long position = 0;
    private long deltat;

    @Override
    public void open(String path, int w, int h, int fps) throws Exception {
        movieWriter = ToolFactory.makeWriter(path);
        movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, w, h);

        IRational frameRate = IRational.make(1, fps);
        deltat = (long) (1e6 * frameRate.getDouble());

        IPixelFormat.Type pixFmt = IPixelFormat.Type.YUV420P;
        IStreamCoder coder = movieWriter.getContainer().getStream(0).getStreamCoder();
        coder.setWidth(w);
        coder.setHeight(h);
        coder.setFrameRate(frameRate);
        coder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        coder.setGlobalQuality(0);
        coder.setNumPicturesInGroupOfPictures(fps);
        coder.setPixelType(pixFmt);

        converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, pixFmt, w, h);
    }

    @Override
    public void encode(BufferedImage im) throws Exception {
        IVideoPicture pic = converter.toPicture(im, position);
        pic.setQuality(0);
        movieWriter.encodeVideo(0, pic);
        position += deltat;
    }

    @Override
    public void close() throws Exception {
        movieWriter.close();
        converter.delete();
    }

}
