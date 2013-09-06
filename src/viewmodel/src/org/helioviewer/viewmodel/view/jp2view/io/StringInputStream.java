package org.helioviewer.viewmodel.view.jp2view.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * 
 * The <code>StringInputStream</code> class allows reading of single lines from
 * an input stream. The end of a line is either LF (10), CR (13), or CR followed
 * immediately by LF. The end of stream is also considered a legal end of line.
 * This stream is not buffered.
 * 
 * @author Juan Pablo Garcia Ortiz
 * @see java.io.InputStream
 * @version 0.1
 * 
 */
public class StringInputStream extends InputStream {

    /** The base input stream */
    private InputStream in;

    /** The last byte read */
    private int lastByte = 0;

    /** Used for rewinding one byte of the stream */
    private boolean back = false;

    /** The CR carrier-return constant */
    static public final int CR = 13;

    /** The LF line-fees constant */
    static public final int LF = 10;

    /**
     * Constructs a new object with a <code>InputStream</code> base object.
     * 
     * @param in
     *            A <code>InputStream</code> object as a base stream.
     */
    public StringInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * This kind of stream does not support marking.
     * 
     * @return <code>False</code>.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads the next byte of the stream and returns it.
     * 
     * @return The next byte read, or -1 is there is no more data.
     * @throws java.io.IOException
     */
    public int read() throws IOException {
        if (!back)
            lastByte = in.read();
        back = false;
        return lastByte;
    }

    /** Used for rewinding one byte of the stream. */
    private void rewind() {
        back = true;
    }

    /**
     * Reads a single line of the input stream.
     * 
     * @return The new line read or <code>null</code> if there is not more data.
     * @throws java.io.IOException
     */
    public String readLine() throws IOException {
        int c;
        boolean ns = true;
        boolean end = false;
        StringWriter res = new StringWriter();

        while (!end) {
            c = read();

            if ((c == -1) && ns)
                return null;
            ns = false;

            if ((c == LF) || (c == -1))
                end = true;
            else if (c == CR) {
                if (read() != LF)
                    rewind();
                end = true;
            } else
                res.write(c);
        }

        return res.toString();
    }
}
