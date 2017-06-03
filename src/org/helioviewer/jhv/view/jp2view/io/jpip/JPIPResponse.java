package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;

/**
 * A response to a JPIPRequest, encapsulates the JPIPDataSegments
 *
 * @author Juan Pablo Garcia Ortiz
 * @author caplins
 *
 */
public class JPIPResponse {

    /** The status: could be EOR_WINDOW_DONE or EOR_IMAGE_DONE */
    private long status = -1;

    private final String cnew;

    public JPIPResponse(String _cnew) {
        cnew = _cnew;
    }

    public String getCNew() {
        return cnew;
    }

    /**
     * Tells if the response completes the last request.
     *
     * @return True, if the response is complete
     */
    public boolean isResponseComplete() {
        return status == JPIPConstants.EOR_WINDOW_DONE || status == JPIPConstants.EOR_IMAGE_DONE;
    }

    /** The last class identifier read. */
    private long classId = 0;

    /** The last code-stream index read. */
    private long codestream = 0;

    /** The total length in bytes of the last VBAS read. */
    private int vbasLength = 0;

    /** The first byte of the last VBAS read. */
    private int vbasFstByte = 0;

    /**
     * Reads an VBAS integer from the stream. The length in bytes of the VBAS is
     * stored in the <code>vbasLength</code>variable, and the first byte of the
     * VBAS is stored in the <code>vbasFstByte</code> variable.
     * 
     * @throws IOException
     */
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

    /**
     * Reads the next data segment from the stream, and stores its information
     * in the <code>JpipDataSegment</code> object passed as parameter. The data
     * buffer is not reallocated every time. It is only reallocated if the next
     * data length is bigger than the previous one.
     */
    private JPIPDataSegment readSegment(InputStream in) throws IOException {
        long id;
        if ((id = readVBAS(in)) < 0)
            return null;

        JPIPDataSegment seg = new JPIPDataSegment();
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
                classId = readVBAS(in);
                if (m > 2)
                    codestream = readVBAS(in);
            }
            seg.codestreamID = codestream;

            for (JPIPDatabinClass idEnum : JPIPDatabinClass.values())
                if (classId == idEnum.standardClassID) {
                    seg.classID = idEnum;
                    break;
                }
            if (seg.classID == null)
                throw new ProtocolException("Invalid databin classID");

            seg.offset = (int) readVBAS(in);
            seg.length = (int) readVBAS(in);

            if (classId == JPIPConstants.EXTENDED_PRECINCT_DATA_BIN_CLASS || classId == JPIPConstants.EXTENDED_TILE_DATA_BIN_CLASS)
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

    public void readSegments(InputStream in, JPIPCache cache) throws IOException {
        JPIPDataSegment seg;
        while ((seg = readSegment(in)) != null) {
            if (seg.isEOR)
                status = seg.binID;
            else
                cache.addJPIPDataSegment(seg);
        }
    }

}
