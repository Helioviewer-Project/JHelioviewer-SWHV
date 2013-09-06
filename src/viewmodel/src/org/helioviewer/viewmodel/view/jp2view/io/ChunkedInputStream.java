package org.helioviewer.viewmodel.view.jp2view.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;

import org.helioviewer.base.logging.Log;

/**
 * 
 * The class <code>ChunkedInputStream</code> allows to decode HTTP chunked
 * responses with a simple format. Do not support internal chunk headers.
 * 
 * @author Juan Pablo Garcia Ortiz
 * @see java.io.InputStream StringInputStream
 * @version 0.1
 * 
 */

public class ChunkedInputStream extends InputStream {

    /** The last chunk length */
    private int chunkLength = 0;

    /** Indicates if there are more chunks */
    private boolean moreChunks = true;

    /** The base input stream */
    private StringInputStream in = null;

    /** The total length in bytes of the read data */
    private int totalLength = 0;

    /**
     * Constructs a new object with a <code>InputStream</code> base object.
     * 
     * @param in
     *            A <code>InputStream</code> object as a base stream.
     */
    public ChunkedInputStream(InputStream in) {
        this.in = new StringInputStream(in);
    }

    /**
     * Returns the total length of the read data.
     */
    public int getTotalLength() {
        return totalLength;
    }

    /**
     * This kind of stream does not support marking.
     * 
     * @return <code>False</code>
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Used internally to read lines from the input stream and control if the
     * end of stream is reached unexpectedly.
     * 
     * @return A new line from the input stream
     * @throws java.io.IOException
     * 
     */
    private String readLine() throws IOException {
        String res = in.readLine();
        if (res != null)
            return res;
        else
            throw new EOFException("Unexpected end of stream decoding chunk");
    }

    /**
     * Reads the next byte of the chunked content. It will return -1 if there is
     * no more chunks to decode. If the end of stream is reached before decoding
     * correctly all the chunks, a <code>EOFException</code> is launched.
     * 
     * @return The next byte read, or -1 is there is no more data.
     * @throws java.io.IOException
     */
    public int read() throws IOException {
        if (!moreChunks)
            return -1;

        if (chunkLength > 0) {
            int res = in.read();

            if (--chunkLength == 0) {
                if (readLine().length() > 0)
                    throw new ProtocolException("An empty new line was expected after chunk");
            }

            return res;

        } else {
            String line = readLine();

            try {
                chunkLength = Integer.parseInt(line, 16);
                totalLength += chunkLength;
            } catch (NumberFormatException ex) {
                Log.error(line);
                throw new ProtocolException("Invalid chunk length format");
            }

            if (chunkLength <= 0) {
                if (readLine().length() > 0)
                    throw new ProtocolException("An empty new line was expected after chunk");

                moreChunks = false;
            }

            return read();
        }
    }
};
