package org.helioviewer.jhv.view.j2k.jpip.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import javax.annotation.Nonnull;

class ByteChannelInputStream extends InputStream {

    private final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
    private final ByteChannel chan;

    ByteChannelInputStream(ByteChannel _chan) {
        chan = _chan;
    }

    @Override
    public int read() throws IOException {
        ensure(1);
        return (buf.get() + 256) & 0xFF;
    }

    @Override
    public int read(@Nonnull byte[] bytes, int off, int len) throws IOException {
        ensure(len);
        buf.get(bytes, off, len);
        return len;
    }

    @Override
    public int read(@Nonnull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    private void ensure(int len) throws IOException {
        if (buf.remaining() < len) {
            buf.compact();
            buf.flip();
            do {
                buf.position(buf.limit());
                buf.limit(buf.capacity());
                chan.read(buf);
                buf.flip();
            } while (buf.remaining() < len);
        }
    }

}
