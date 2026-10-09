package org.helioviewer.jhv.source.jpip.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public final class HTTPStreamTest {

    public static void main(String[] arguments) throws Exception {
        for (boolean singleByte : new boolean[]{false, true}) {
            ByteArrayInputStream input = bytes("3\r\nabc\r\n2\r\nde\r\n0\r\n\r\nNEXT");
            ChunkedInputStream chunked = new ChunkedInputStream(input);
            check(readBody(chunked, singleByte).equals("abcde"), "chunked body");
            check(input.read() == 'N', "chunked read consumed next response");
            check(chunked.read(new byte[0]) == 0, "empty chunked read at EOF");

            input = bytes("abcdeNEXT");
            FixedSizedInputStream fixed = new FixedSizedInputStream(input, 5);
            check(readBody(fixed, singleByte).equals("abcde"), "fixed-length body");
            check(input.read() == 'N', "fixed-length read consumed next response");
            check(fixed.read(new byte[0]) == 0, "empty fixed-length read at EOF");

            fixed = new FixedSizedInputStream(bytes("a"), 2);
            try {
                readBody(fixed, singleByte);
                throw new AssertionError("Accepted truncated fixed-length body");
            } catch (EOFException expected) {
            }
            chunked = new ChunkedInputStream(bytes("3\r\na"));
            try {
                readBody(chunked, singleByte);
                throw new AssertionError("Accepted truncated chunk payload");
            } catch (EOFException expected) {
            }
        }

        for (String incomplete : new String[]{"", "HTTP/1.1 200 OK", "Content-Length: 3\r"}) {
            try {
                LineRead.readAsciiLine(bytes(incomplete));
                throw new AssertionError("Accepted incomplete HTTP line");
            } catch (EOFException expected) {
            }
        }
        check(LineRead.readAsciiLine(bytes("\r\n")).isEmpty(), "complete empty HTTP line");
        ByteArrayInputStream invalidBody = bytes("-1\r\nUNREAD");
        try (InputStream invalid = new ChunkedInputStream(invalidBody)) {
            readBody(invalid, false);
            throw new AssertionError("Accepted negative chunk length");
        } catch (IOException expected) {
        }
        check(invalidBody.read() == 'U', "close tried to drain invalid chunk framing");

        // Timeouts in the header, payload, chunk CRLF and final CRLF must not
        // cause close() to read the failed response again.
        for (String prefix : new String[]{"", "3\r\na", "1\r\na\r", "0\r\n\r"}) {
            ByteArrayInputStream body = bytes(prefix);
            int[] attempts = {0};
            InputStream failing = new InputStream() {
                @Override
                public int read() throws IOException {
                    attempts[0]++;
                    int value = body.read();
                    if (value < 0)
                        throw new SocketTimeoutException("Stalled response");
                    return value;
                }
            };
            ChunkedInputStream stalled = new ChunkedInputStream(failing);
            try {
                readBody(stalled, true);
                throw new AssertionError("Expected a read timeout");
            } catch (SocketTimeoutException expected) {
            }
            int reads = attempts[0];
            stalled.close();
            check(attempts[0] == reads, "close retried a timed-out read");
        }

        ByteArrayInputStream input = bytes("3\r\nabc\r\n0\r\n\r\nNEXT");
        ChunkedInputStream chunked = new ChunkedInputStream(input);
        check(chunked.read() == 'a', "initial chunk byte");
        chunked.close();
        chunked.close();
        check(input.read() == 'N', "close must drain only the current response");
        System.out.println("PASS: HTTP bodies, response boundaries, truncation and close draining");
    }

    private static String readBody(InputStream input, boolean singleByte) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (singleByte) {
            int value;
            while ((value = input.read()) >= 0)
                output.write(value);
        } else {
            byte[] buffer = new byte[2];
            int count;
            while ((count = input.read(buffer)) >= 0)
                output.write(buffer, 0, count);
        }
        return output.toString(StandardCharsets.US_ASCII);
    }

    private static ByteArrayInputStream bytes(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
