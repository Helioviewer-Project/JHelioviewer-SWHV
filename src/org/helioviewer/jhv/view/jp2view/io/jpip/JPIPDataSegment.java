package org.helioviewer.jhv.view.jp2view.io.jpip;

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
