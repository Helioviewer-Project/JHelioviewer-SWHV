package org.helioviewer.jhv.view.j2k.jpip;

import java.io.Serial;
import java.io.Serializable;

// The class JPIPSegment is used to construct objects to store
// segments of JPIP data. These segments can be data-bin segments as well as EOR
// messages. In this last case, the EOR code is stored in the 'id'
// field and the EOR message body is stored in the 'data' field.
class JPIPSegment implements Serializable {

    @Serial
    static final long serialVersionUID = 42L;

    // The data-bin in-class identifier
    long binID;
    // The data-bin auxiliary information
    long aux;
    // The data-bin class identifier
    int klassID;
    // The code-stream index
    long codestreamID;
    // Offset of this segment within the data-bin data
    int offset;
    // Length of this segment
    int length;
    // The segment data
    byte[] data;
    // Indicates if this segment is the last one (when there is a data segment stream)
    boolean isFinal;
    // Indicates if this segment is an End-Of-Response message
    boolean isEOR;

    // Returns a string representation of the JPIP segment
    @Override
    public String toString() {
        String res = "JPIPSegment [";
        if (isEOR) {
            res += "EOR id=" + binID + " len=" + length;
        } else {
            res += "class=" + klassID + " stream=" + codestreamID;
            res += " id=" + binID + " off=" + offset + " len=" + length;
            if (isFinal)
                res += " final";
        }
        return res + ']';
    }

}
