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

    // The total length in bytes of the last VBAS read
    private int vbasLength = 0;

    // The first byte of the last VBAS read
    private int vbasFstByte = 0;

    // Reads an VBAS integer from the stream. The length in bytes of the VBAS is
    // stored in the vbasLength variable, and the first byte of the
    // VBAS is stored in the vbasFstByte variable.
    private long readVBAS(InputStream in) throws IOException {
        vbasLength = 0;
        long value = 0;
        int c;
        do {
            if (vbasLength >= 9)
                throw new ProtocolException("VBAS length not supported");

            if ((c = in.read()) < 0) {
                if (vbasLength > 0)
                    throw new EOFException("EOF reached before completing VBAS");
                else
                    return -1;
            }

            value = (value << 7) | (long) (c & 0x7F);

            if (vbasLength == 0)
                vbasFstByte = c;
            vbasLength++;
        } while ((c & 0x80) != 0);

        return value;
    }

    // Reads the next data segment from the stream, and stores its information
    // in the JPIPSegment object passed as parameter. The data
    // buffer is not reallocated every time. It is only reallocated if the next
    // data length is bigger than the previous one.
    @Nullable
    private JPIPSegment readSegment(InputStream in) throws IOException {
        long id;
        if ((id = readVBAS(in)) < 0)
            return null;

        JPIPSegment seg = new JPIPSegment();
        seg.binID = id;

        if (vbasFstByte == 0) {
            seg.isEOR = true;

            if ((seg.binID = in.read()) < 0)
                throw new EOFException("EOF reached before completing EOR message");

            seg.length = (int) readVBAS(in);
        } else {
            seg.isEOR = false;
            seg.binID &= ~(0x70L << ((vbasLength - 1) * 7));
            seg.isFinal = (vbasFstByte & 0x10) != 0;

            int m = (vbasFstByte & 0x7F) >> 5;
            if (m == 0)
                throw new ProtocolException("Invalid Bin-ID value format");
            if (m >= 2) {
                classID = (int) readVBAS(in);
                if (m > 2)
                    codestream = readVBAS(in);
            }
            seg.codestreamID = codestream;

            int klassID = Constants.getKlass(classID);
            if (klassID == Constants.UNKNOWN_DATABIN)
                throw new ProtocolException("Invalid databin classID");
            seg.klassID = klassID;

            seg.offset = (int) readVBAS(in);
            seg.length = (int) readVBAS(in);

            if (classID == Constants.JPIP.EXTENDED_PRECINCT_DATA_BIN_CLASS || classID == Constants.JPIP.EXTENDED_TILE_DATA_BIN_CLASS)
                seg.aux = readVBAS(in);
        }

        if (seg.length > 0) {
            seg.data = new byte[seg.length];

            int offset = 0;
            int len = seg.length;
            while (len != 0) {
                int read = in.read(seg.data, offset, len);
                if (read == -1)
                    throw new EOFException("Unexpected EOF");
                len -= read;
                offset += read;
            }
        }

        return seg;
    }

    void readSegments(InputStream in, JPIPCache cache, int frame) throws KduException, IOException {
        JPIPSegment seg;
        while ((seg = readSegment(in)) != null) {
            if (seg.isEOR)
                status = seg.binID;
            else if (seg.isFinal || seg.length > 0) { // avoid pointless segments
                cache.put(frame, seg);
            }
        }
    }

}
