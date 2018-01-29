package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.nio.ByteBuffer;

import com.google.common.primitives.Shorts;

/**
 * 
 * The class <code>JpipDataSegment</code> is used to construct objects to store
 * segments of JPIP data. These segments can be data-bin segments as well as EOR
 * messages. In this last case, the EOR code is stored in the <code>id</code>
 * field and the EOR message body is stored in the <code>data</code> field.
 * 
 * @author Juan Pablo Garcia Ortiz
 * @version 0.1
 * 
 */
public class JPIPDataSegment {

    // The data-bin in-class identifier
    public long binID;

    // The data-bin auxiliary information
    public long aux;

    // The data-bin class identifier
    public JPIPDatabinClass classID;

    // The code-stream index
    public long codestreamID;

    // Offset of this segment within the data-bin data
    public int offset;

    // Length of this segment
    public int length;

    // The segment data
    public byte data[];

    // Indicates if this segment is the last one (when there is a data segment stream)
    public boolean isFinal;

    // Indicates if this segment is a End Of Response message
    public boolean isEOR;

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 1 + 2 + 2 + 2 + length + 1);

        buffer.putShort(Shorts.checkedCast(binID));
        // buffer.putLong(aux); don't need
        buffer.put((byte) classID.kakaduClassID); // fits surely
        buffer.putShort(Shorts.checkedCast(codestreamID));
        buffer.putShort(Shorts.checkedCast(offset));
        buffer.putShort(Shorts.checkedCast(length));
        if (length != 0)
            buffer.put(data);

        byte flags = 0;
        if (isFinal)
            flags += 1;
        if (isEOR)
            flags += 2;
        buffer.put(flags);
        buffer.rewind();

        return buffer;
    }

    public void fromBuffer(ByteBuffer buffer) {
        binID = buffer.getShort();

        int classId = buffer.get();
        for (JPIPDatabinClass idEnum : JPIPDatabinClass.values())
            if (classId == idEnum.kakaduClassID) {
                classID = idEnum;
                break;
            }

        codestreamID = buffer.getShort();
        offset = buffer.getShort();
        length = buffer.getShort();
        if (length > 0) {
            data = new byte[length];
            buffer.get(data);
        }

        byte flags = buffer.get();
        if ((flags & 1) != 0)
            isFinal = true;
        if ((flags & 2) != 0)
            isEOR = true;
    }

    // Returns a string representation of the JPIP data segment
    @Override
    public String toString() {
        String res = getClass().getName() + " [";
        if (isEOR)
            res += "EOR id=" + binID + " len=" + length;
        else {
            res += "class=" + classID.jpipString + " stream=" + codestreamID;
            res += " id=" + binID + " off=" + offset + " len=" + length;
            if (isFinal)
                res += " final";
        }
        return res + ']';
    }

}
