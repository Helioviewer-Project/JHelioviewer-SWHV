package org.helioviewer.jhv.view.j2k.io.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.view.j2k.io.ByteChannelInputStream;
import org.helioviewer.jhv.view.j2k.io.LineRead;

public class HTTPChannel {

    //private static final int TIMEOUT_CONNECT = 30000;
    private static final int TIMEOUT_READ = 30000;
    private static final int PORT = 80;

    private final SocketChannel channel;

    protected final InputStream inputStream;
    protected final String host;

    protected HTTPChannel(URI uri) throws IOException {
        try {
            //socket = new Socket(ProxySettings.proxy);
            channel = SocketChannel.open();

            channel.socket().setReceiveBufferSize(Math.max(262144 * 8, 2 * channel.socket().getReceiveBufferSize()));
            channel.socket().setTrafficClass(0x10);
            channel.socket().setSoTimeout(TIMEOUT_READ);
            channel.socket().setKeepAlive(true);
            channel.socket().setTcpNoDelay(true);

            int port = uri.getPort();
            int usedPort = port <= 0 ? PORT : port;
            String usedHost = uri.getHost();
            host = usedHost + ':' + usedPort;

            //socket.connect(new InetSocketAddress(usedHost, usedPort), TIMEOUT_CONNECT);
            channel.connect(new InetSocketAddress(usedHost, usedPort));
            inputStream = new ByteChannelInputStream(channel);
        } catch (Exception e) { // redirect all to IOException
            throw new IOException(e);
        }
    }

    protected HTTPMessage recv() throws IOException {
        String line = LineRead.readAsciiLine(inputStream);
        if (!"HTTP/1.1 200 OK".equals(line))
            throw new ProtocolException("Invalid HTTP response: " + line);

        // Parses HTTP headers
        HTTPMessage res = new HTTPMessage();
        while (true) {
            line = LineRead.readAsciiLine(inputStream);
            if (line.isEmpty())
                return res;

            String[] parts = Regex.HttpField.split(line);
            if (parts.length != 2)
                throw new ProtocolException("Invalid HTTP header field: " + line);
            res.setHeader(parts[0], parts[1]);
        }
    }

    protected void write(String str) throws IOException {
        channel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
    }

    protected void close() throws IOException {
        channel.close();
    }

    public boolean isClosed() {
        return !channel.isOpen();
    }

}
