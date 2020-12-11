package org.helioviewer.jhv.view.j2k.io.jpip;

import java.io.Serializable;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;

//import com.google.common.primitives.Shorts;

// The class JPIPSegment is used to construct objects to store
// segments of JPIP data. These segments can be data-bin segments as well as EOR
// messages. In this last case, the EOR code is stored in the 'id'
// field and the EOR message body is stored in the 'data' field.
class JPIPSegment implements Serializable {

    static final long serialVersionUID = 42L;

    // The data-bin in-class identifier
    public long binID;

    // The data-bin auxiliary information
    public long aux;

    // The data-bin class identifier
    public int klassID;

    // The code-stream index
    public long codestreamID;

    // Offset of this segment within the data-bin data
    public int offset;

    // Length of this segment
    public int length;

    // The segment data
    public byte[] data;

    // Indicates if this segment is the last one (when there is a data segment stream)
    public boolean isFinal;

    // Indicates if this segment is a End Of Response message
    public boolean isEOR;

/*
    public ByteBuffer toBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(length + 6).order(ByteOrder.nativeOrder());

        buffer.putShort(Shorts.checkedCast(binID));
        // buffer.putLong(aux); not used
        // buffer.putShort(Shorts.checkedCast(codestreamID)); don't need
        // buffer.putShort(Shorts.checkedCast(offset)); don't need

        buffer.putShort(Shorts.checkedCast(length));
        if (length > 0)
            buffer.put(data);

        buffer.put((byte) klassID); // fits surely

        byte flags = 0;
        if (isFinal)
            flags += 1;
        buffer.put(flags);
        buffer.rewind();

        return buffer;
    }

    public static JPIPSegment fromBuffer(ByteBuffer buffer) {
        JPIPSegment seg = new JPIPSegment();
        seg.binID = buffer.getShort();
        // seg.codestreamID = buffer.getShort();
        // seg.offset = buffer.getShort();

        seg.length = buffer.getShort();
        if (seg.length > 0) {
            seg.data = new byte[seg.length];
            buffer.get(seg.data);
        }

        seg.klassID = buffer.get();

        byte flags = buffer.get();
        if ((flags & 1) != 0)
            seg.isFinal = true;

        return seg;
    }
*/

    // Returns a string representation of the JPIP segment
    @Override
    public String toString() {
        String res = getClass().getName() + " [";
        if (isEOR)
            res += "EOR id=" + binID + " len=" + length;
        else {
            res += "class=" + klassID + " stream=" + codestreamID;
            res += " id=" + binID + " off=" + offset + " len=" + length;
            if (isFinal)
                res += " final";
        }
        return res + ']';
    }

}
