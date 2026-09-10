package org.helioviewer.jhv.view.j2k.jpip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;

import javax.annotation.Nullable;

import kdu_jni.KduException;

// A response to a JPIPRequest, encapsulates the JPIPSegments
public class JPIPResponse {

    // The status: could be EOR_WINDOW_DONE or EOR_IMAGE_DONE
    private long status = -1;

    private final String cnew;

    public JPIPResponse(String _cnew) {
        cnew = _cnew;
    }

    String getCNew() {
        return cnew;
    }

    // Tells if the response completes the last request
    public boolean isResponseComplete() {
        return status == Constants.JPIP.EOR_WINDOW_DONE || status == Constants.JPIP.EOR_IMAGE_DONE;
    }

    // The last class identifier read
    private int classID = 0;

    // The last code-stream index read
    private long codestream = 0;

    private static long readVBAS(InputStream in) throws IOException {
        return readVBAS(in, in.read());
    }

    private static long readVBAS(InputStream in, int firstByte) throws IOException {
        if (firstByte < 0)
            throw new EOFException("Missing JPIP integer");

        long value = firstByte & 0x7F;
        int nextByte = firstByte;
        for (int count = 1; (nextByte & 0x80) != 0; count++) {
            if (count == 9)
                throw new ProtocolException("JPIP integer exceeds supported length");
            nextByte = in.read();
            if (nextByte < 0)
                throw new EOFException("Truncated JPIP integer");
            value = (value << 7) | (nextByte & 0x7F);
        }
        return value;
    }

    private static int readIntVBAS(InputStream in) throws IOException {
        long value = readVBAS(in);
        if (value > Integer.MAX_VALUE)
            throw new ProtocolException("JPIP integer exceeds supported range");
        return (int) value;
    }

    @Nullable
    private JPIPSegment readSegment(InputStream in) throws IOException {
        int firstByte = in.read();
        if (firstByte < 0)
            return null; // EOF is valid only between messages.

        JPIPSegment seg = new JPIPSegment();

        if (firstByte == 0) {
            seg.isEOR = true;

            if ((seg.binID = in.read()) < 0)
                throw new EOFException("EOF reached before completing EOR message");

            seg.length = readIntVBAS(in);
        } else {
            seg.isEOR = false;
            seg.binID = readVBAS(in, firstByte & 0x8F); // Strip flags, preserve continuation.
            seg.isFinal = (firstByte & 0x10) != 0;

            int m = (firstByte & 0x7F) >> 5;
            if (m == 0)
                throw new ProtocolException("Invalid Bin-ID value format");
            if (m >= 2) {
                classID = readIntVBAS(in);
                if (m > 2)
                    codestream = readVBAS(in);
            }
            seg.codestreamID = codestream;

            int klassID = Constants.getKlass(classID);
            if (klassID == Constants.UNKNOWN_DATABIN)
                throw new ProtocolException("Invalid databin classID");
            seg.klassID = klassID;

            seg.offset = readIntVBAS(in);
            seg.length = readIntVBAS(in);
            if (seg.length > Integer.MAX_VALUE - seg.offset)
                throw new ProtocolException("JPIP databin exceeds supported range");

            if (classID == Constants.JPIP.EXTENDED_PRECINCT_DATA_BIN_CLASS || classID == Constants.JPIP.EXTENDED_TILE_DATA_BIN_CLASS)
                seg.aux = readVBAS(in);
        }

        if (seg.length > 0) {
            seg.data = in.readNBytes(seg.length);
            if (seg.data.length != seg.length)
                throw new EOFException("Truncated JPIP payload");
        }

        return seg;
    }

    void readSegments(InputStream in, JPIPCache cache, int frame) throws KduException, IOException {
        JPIPSegment seg;
        while ((seg = readSegment(in)) != null) {
            if (seg.isEOR) {
                status = seg.binID;
            } else if (seg.isFinal || seg.length > 0) { // avoid pointless segments
                cache.put(frame, seg);
            }
        }
    }

}
