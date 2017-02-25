package org.helioviewer.jhv.viewmodel.view.jp2view.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;

import org.jetbrains.annotations.NotNull;

/*
 * Transparently coalesces chunks of a HTTP stream that uses
 * Transfer-Encoding chunked.
 *
 * Note that this class NEVER closes the underlying stream, even when close
 * gets called.  Instead, it will read until the "end" of its chunking on close,
 * which allows for the seamless invocation of subsequent HTTP 1.1 calls, while
 * not requiring the client to remember to read the entire contents of the
 * response.
 */

/**
 * The class <code>ChunkedInputStream</code> allows to decode HTTP chunked
 * responses with a simple format. Does not support internal chunk headers.
 */
public class ChunkedInputStream extends TransferInputStream {

    private int totalLength = 0;

    /** The last chunk length */
    private int chunkLength = 0;

    /** True if we've reached the end of stream */
    private boolean eof = false;

    /** True if this stream is closed */
    private boolean closed = false;

    /** The base input stream */
    private final InputStream in;

    private final byte[] tmpRead = new byte[1];

    /**
     * Constructs a new object with a <code>InputStream</code> base object.
     * 
     * @param _in
     *            A <code>InputStream</code> object as a base stream.
     */
    public ChunkedInputStream(InputStream _in) {
        in = _in;
    }

    // Returns the length of the payload read
    public int getTotalLength() {
        return totalLength;
    }

    /**
     * This kind of stream does not support marking.
     * 
     * @return <code>False</code>
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads the next byte of the chunked content. It will return -1 if there are
     * no more chunks to decode.
     * 
     * @return The next byte read, or -1 is there is no more data.
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        int n = read(tmpRead, 0, 1);
        switch (n) {
            case -1:
                return -1;
            case 1:
                return tmpRead[0] & 0xff;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Attempt to read from closed stream");
        }

        while (true) {
            if (eof)
                return -1;

            if (chunkLength > 0) {
                int read = in.read(b, off, Math.min(chunkLength, len));
                if (read != -1) {
                    chunkLength -= read;
                    totalLength += read;
                    if (chunkLength == 0)
                        LineRead.readCRLF(in);
                }
                return read;
            }

            if (chunkLength == 0) {
                String line = LineRead.readAsciiLine(in);

                // esajpip doesn't use chunk extension
                // int separator = line.indexOf(';');
                // line = (separator > 0) ? line.substring(0, separator).trim() : line.trim();
                try {
                    chunkLength = Integer.parseInt(line, 16);
                    if (chunkLength <= 0) {
                        LineRead.readCRLF(in);
                        eof = true;
                    }
                } catch (NumberFormatException ex) {
                    throw new ProtocolException("Invalid chunk length format");
                }
            }
        }
    }

    @Override
    public int read (@NotNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                if (!eof) {
                    // read and discard the remainder of the message
                    byte buf[] = new byte[1024];
                    while (read(buf) >= 0) ;
                }
            } finally {
                eof = true;
                closed = true;
            }
        }
    }

}
