package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class XuggleSimple implements MovieExporter {

    private IMediaWriter movieWriter;
    private IConverter converter;
    private float fps;
    private long position = 0;

    @Override
    public void open(String path, int w, int h, float _fps) {
        fps = _fps;

        movieWriter = ToolFactory.makeWriter(path);
        movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, w, h);

        IPixelFormat.Type pixFmt = IPixelFormat.Type.YUV420P;
        IStreamCoder coder = movieWriter.getContainer().getStream(0).getStreamCoder();
        coder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        coder.setGlobalQuality(0);
        coder.setNumPicturesInGroupOfPictures((int) fps);
        coder.setPixelType(pixFmt);

        converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, pixFmt, w, h);
    }

    @Override
    public void encode(BufferedImage im) {
        IVideoPicture pic = converter.toPicture(im, position);
        pic.setQuality(0);
        movieWriter.encodeVideo(0, pic);
        position += (1e6 / fps);
    }

    @Override
    public void close() {
        movieWriter.close();
        converter.delete();
    }

}
