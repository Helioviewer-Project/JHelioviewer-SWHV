package org.helioviewer.jhv.view.jp2view.io;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

/**
 * Input stream with a fixed size. After reading the expected number of bytes
 * this input stream will behave as if the end of the stream has been reached.
 */
public class FixedSizedInputStream extends TransferInputStream {

    private int remainingBytes;

    private final int expectedBytes;
    private final InputStream in;

    public FixedSizedInputStream(InputStream _in, int _expectedBytes) {
        remainingBytes = expectedBytes = _expectedBytes;
        in = _in;
    }

    public int getTotalLength() {
        return expectedBytes - remainingBytes;
    }

    @Override
    public int read() throws IOException {
        if (remainingBytes > 0) {
            --remainingBytes;
            return in.read();
        }
        return -1;
    }

    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        if (remainingBytes > 0) {
            int bytesRead = in.read(b, off, remainingBytes < len ? remainingBytes : len);
            remainingBytes -= bytesRead;
            return bytesRead;
        }
        return -1;
    }

    @Override
    public int read(@Nonnull byte[] b) throws IOException {
        if (remainingBytes > 0) {
            int bytesRead = in.read(b, 0, remainingBytes < b.length ? remainingBytes : b.length);
            remainingBytes -= bytesRead;
            return bytesRead;
        }
        return -1;
    }

}
