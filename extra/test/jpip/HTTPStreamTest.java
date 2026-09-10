package org.helioviewer.jhv.view.j2k.jpip.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class HTTPStreamTest {

    public static void main(String[] arguments) throws Exception {
        for (boolean singleByte : new boolean[]{false, true}) {
            ByteArrayInputStream input = bytes("3\r\nabc\r\n2\r\nde\r\n0\r\n\r\nNEXT");
            ChunkedInputStream chunked = new ChunkedInputStream(input);
            check(readBody(chunked, singleByte).equals("abcde"), "chunked body");
            check(chunked.getTotalLength() == 5, "chunked byte count");
            check(input.read() == 'N', "chunked read consumed next response");
            check(chunked.read(new byte[0]) == 0, "empty chunked read at EOF");

            input = bytes("abcdeNEXT");
            FixedSizedInputStream fixed = new FixedSizedInputStream(input, 5);
            check(readBody(fixed, singleByte).equals("abcde"), "fixed-length body");
            check(fixed.getTotalLength() == 5, "fixed-length byte count");
            check(input.read() == 'N', "fixed-length read consumed next response");
            check(fixed.read(new byte[0]) == 0, "empty fixed-length read at EOF");

            fixed = new FixedSizedInputStream(bytes("a"), 2);
            try {
                readBody(fixed, singleByte);
                throw new AssertionError("Accepted truncated fixed-length body");
            } catch (EOFException expected) {
                check(fixed.getTotalLength() == 1, "truncated body byte count");
            }
            chunked = new ChunkedInputStream(bytes("3\r\na"));
            try {
                readBody(chunked, singleByte);
                throw new AssertionError("Accepted truncated chunk payload");
            } catch (EOFException expected) {
                check(chunked.getTotalLength() == 1, "truncated chunk byte count");
            }
        }

        ByteArrayInputStream input = bytes("3\r\nabc\r\n0\r\n\r\nNEXT");
        ChunkedInputStream chunked = new ChunkedInputStream(input);
        check(chunked.read() == 'a', "initial chunk byte");
        chunked.close();
        chunked.close();
        check(input.read() == 'N', "close must drain only the current response");
        System.out.println("PASS: HTTP bodies, response boundaries, byte counts, truncation and close draining");
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
