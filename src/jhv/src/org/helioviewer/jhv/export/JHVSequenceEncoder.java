package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JHVSequenceEncoder {

    private final SeekableByteChannel ch;
    private final Picture toEncode;
    private final H264Encoder encoder;
    private final ArrayList<ByteBuffer> spsList;
    private final ArrayList<ByteBuffer> ppsList;
    private final FramesMP4MuxerTrack outTrack;
    private final ByteBuffer _out;
    private int frameNo;
    private final MP4Muxer muxer;
    private final int fps;

    public JHVSequenceEncoder(File out, int w, int h, int fps) throws IOException {
        this.ch = NIOUtils.writableFileChannel(out);

        // Muxer that will store the encoded frames
        muxer = new MP4Muxer(ch, Brand.MP4);

        this.fps = fps;
        // Add video track to muxer
        outTrack = muxer.addTrack(TrackType.VIDEO, fps);

        // Allocate a buffer big enough to hold output frames
        _out = ByteBuffer.allocate(w * h * 6);

        // Create an instance of encoder
        encoder = new H264Encoder();

        toEncode = Picture.create(w, h, encoder.getSupportedColorSpaces()[0]);

        // Encoder extra data ( SPS, PPS ) to be stored in a special place of
        // MP4
        spsList = new ArrayList<ByteBuffer>();
        ppsList = new ArrayList<ByteBuffer>();
    }

    public void encodeNativeFrame(BufferedImage img) throws IOException {
        // Perform conversion
        JHVRgbToYuv420j8Bit.transform(img, toEncode);

        // Encode image into H.264 frame, the result is stored in '_out' buffer
        _out.clear();
        ByteBuffer result = encoder.encodeFrame(toEncode, _out);

        // Based on the frame above form correct MP4 packet
        spsList.clear();
        ppsList.clear();
        H264Utils.wipePS(result, spsList, ppsList);
        H264Utils.encodeMOVPacket(result);

        // Add packet to video track
        outTrack.addFrame(new MP4Packet(result, frameNo, fps, 1, frameNo, true, null, frameNo, 0));

        frameNo++;

        if (frameNo / 10 == 0)
            System.gc();
    }

    public void finish() throws IOException {
        // Push saved SPS/PPS to a special storage in MP4
        outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList, 4));

        // Write MP4 header and finalize recording
        muxer.writeHeader();
        NIOUtils.closeQuietly(ch);
        System.gc();
    }

}
