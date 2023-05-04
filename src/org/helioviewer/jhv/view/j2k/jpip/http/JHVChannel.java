package org.helioviewer.jhv.view.j2k.jpip.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

class JHVChannel {

    private static final int TIMEOUT_READ = 30000;

    static SocketChannel connect(String host, int port) throws IOException {
        SocketChannel channel = SocketChannel.open();

        channel.socket().setReceiveBufferSize(Math.max(262144 * 8, 2 * channel.socket().getReceiveBufferSize()));
        channel.socket().setTrafficClass(0x10);
        channel.socket().setSoTimeout(TIMEOUT_READ);
        channel.socket().setKeepAlive(true);
        channel.socket().setTcpNoDelay(true);

        channel.connect(new InetSocketAddress(host, port));
        return channel;
    }

}