package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.helioviewer.jhv.export.jcodec.JHVRgbToYuv420j8Bit;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

public class JCodecExporter implements MovieExporter {

    private String path;
    private int width, height, fps;

    private FileChannelWrapper ch;
    private H264Encoder encoder;
    private ArrayList<ByteBuffer> spsList;
    private ArrayList<ByteBuffer> ppsList;
    private FramesMP4MuxerTrack outTrack;
    private ByteBuffer _out;
    private int frameNo;
    private MP4Muxer muxer;

    @Override
    public void open(String _path, int _width, int _height, int _fps) {
        path = _path;
        width = _width;
        height = _height;
        fps = _fps;
    }

    @Override
    public Object transform(BufferedImage img) {
        Picture toEncode = Picture.create(img.getWidth(), img.getHeight(), ColorSpace.YUV420J);
        JHVRgbToYuv420j8Bit.transform(img, toEncode);
        return toEncode;
    }

    @Override
    public void encode(Object frame) throws IOException {
        if (!(frame instanceof Picture))
            throw new IOException("Not Picture");

        if (ch == null) {
            ch = new FileChannelWrapper(FileChannel.open(Paths.get(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
            // Muxer that will store the encoded frames
            muxer = new MP4Muxer(ch, Brand.MP4);
            // Add video track to muxer
            outTrack = muxer.addTrack(TrackType.VIDEO, fps);
            // Allocate a buffer big enough to hold output frames
            _out = ByteBuffer.allocate(width * height * 6);
            // Create an instance of encoder
            encoder = new H264Encoder();
            // Encoder extra data ( SPS, PPS ) to be stored in a special place of MP4
            spsList = new ArrayList<>();
            ppsList = new ArrayList<>();
        }

        // Encode image into H.264 frame, the result is stored in '_out' buffer
        _out.clear();
        ByteBuffer result = encoder.encodeFrame((Picture) frame, _out);
        // Based on the frame above form correct MP4 packet
        spsList.clear();
        ppsList.clear();
        H264Utils.wipePS(result, spsList, ppsList);
        H264Utils.encodeMOVPacket(result);
        // Add packet to video track
        outTrack.addFrame(new MP4Packet(result, frameNo, fps, 1, frameNo, true, null, frameNo, 0));
        frameNo++;
    }

    @Override
    public void close() throws IOException {
        // Push saved SPS/PPS to a special storage in MP4
        outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList, 4));
        // Write MP4 header and finalize recording
        muxer.writeHeader();
        ch.close();
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
