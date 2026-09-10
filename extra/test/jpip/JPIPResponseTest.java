package org.helioviewer.jhv.view.j2k.jpip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class JPIPResponseTest {

    // Inspect parsed fields without requiring native KDU objects or changing production visibility.
    private static final Method readSegment;

    static {
        try {
            readSegment = JPIPResponse.class.getDeclaredMethod("readSegment", InputStream.class);
            readSegment.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] arguments) throws Exception {
        JPIPResponse response = new JPIPResponse(null);
        // Explicit main-header class and stream 7, followed by inherited class/stream,
        // then explicit precinct class with the same inherited stream.
        InputStream input = new ByteArrayInputStream(new byte[]{
                0x73, 6, 7, 0, 3, 10, 20, 30,
                0x24, 3, 0,
                0x45, 0, 0, 1, 42});
        JPIPSegment segment = read(response, input);
        check(segment.binID == 3 && segment.klassID == 3 && segment.codestreamID == 7,
                "explicit identifiers");
        check(segment.isFinal && !segment.isEOR && segment.offset == 0 && segment.length == 3,
                "message flags and bounds");
        check(Arrays.equals(segment.data, new byte[]{10, 20, 30}), "payload");
        segment = read(response, input);
        check(segment.binID == 4 && segment.klassID == 3 && segment.codestreamID == 7,
                "inherited identifiers");
        check(!segment.isFinal && segment.offset == 3 && segment.length == 0 && segment.data == null,
                "empty message");
        segment = read(response, input);
        check(segment.binID == 5 && segment.klassID == 0 && segment.codestreamID == 7
                && segment.data[0] == 42, "class change preserves stream");
        check(read(response, input) == null, "EOF between messages");

        int[] wireClasses = {0, 2, 4, 6, 8};
        for (int i = 0; i < wireClasses.length; i++) {
            segment = read(new JPIPResponse(null), new ByteArrayInputStream(
                    new byte[]{0x40, (byte) wireClasses[i], 0, 0}));
            check(segment.klassID == i, "wire class mapping " + wireClasses[i]);
        }
        // Maximum nine-byte VBAS, plus a bin ID whose first byte also holds flags.
        segment = read(new JPIPResponse(null), new ByteArrayInputStream(concat(
                new byte[]{(byte) 0xF1, 0, 6}, vbas(Long.MAX_VALUE),
                vbas(Integer.MAX_VALUE), new byte[]{0})));
        check(segment.binID == 128 && segment.isFinal && segment.codestreamID == Long.MAX_VALUE
                && segment.offset == Integer.MAX_VALUE, "valid numeric boundaries");

        byte[] valid = {0x70, 6, 1, 0, 3, 1, 2, 3};
        for (int length = 1; length < valid.length; length++)
            reject(Arrays.copyOf(valid, length), "truncated message at " + length);
        reject(new byte[]{0}, "missing EOR reason");
        reject(new byte[]{0, 2}, "missing EOR length");
        reject(new byte[]{0, 2, 1}, "missing EOR payload");
        reject(new byte[]{1}, "reserved bin format");
        reject(new byte[]{0x40, 3, 0, 0}, "unknown class");
        reject(concat(new byte[]{0x40}, vbas(1L << 32), new byte[]{0, 0}), "class overflow");
        reject(concat(new byte[]{0x20}, vbas(1L << 31), new byte[]{0}), "offset overflow");
        reject(concat(new byte[]{0x20, 0}, vbas(1L << 31)), "length overflow");
        reject(concat(new byte[]{0x20}, vbas(Integer.MAX_VALUE), new byte[]{1}), "end overflow");
        // Run with a small heap: a bogus length must not allocate a 2 GB array.
        reject(concat(new byte[]{0x20, 0}, vbas(Integer.MAX_VALUE)), "huge missing payload");
        byte[] overlong = new byte[10];
        Arrays.fill(overlong, (byte) 0x80);
        reject(concat(new byte[]{0x20}, overlong), "overlong VBAS");

        for (int reason : new int[]{1, 2, 3, 4, 5, 6, 7, 255}) {
            response = new JPIPResponse(null);
            check(!response.isResponseComplete(), "new response incomplete");
            response.readSegments(new ByteArrayInputStream(new byte[]{0, (byte) reason, 0}), null, 0);
            check(response.isResponseComplete() == (reason == 1 || reason == 2), "EOR " + reason);
        }
        System.out.println("PASS: JPIP identifiers, inheritance, payloads, numeric bounds, truncation and EOR");
    }

    private static JPIPSegment read(JPIPResponse response, InputStream input) throws Exception {
        try {
            return (JPIPSegment) readSegment.invoke(response, input);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause)
                throw cause;
            throw e;
        }
    }

    private static void reject(byte[] bytes, String description) throws Exception {
        try {
            read(new JPIPResponse(null), new ByteArrayInputStream(bytes));
            throw new AssertionError("Accepted " + description);
        } catch (IOException expected) {
            // Only protocol/EOF failures are expected, not arbitrary runtime errors.
        }
    }

    private static byte[] vbas(long value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int shift = 0;
        while ((value >>> shift) > 127)
            shift += 7;
        for (; shift >= 0; shift -= 7)
            bytes.write((int) ((value >>> shift) & 127) | (shift > 0 ? 128 : 0));
        return bytes.toByteArray();
    }

    private static byte[] concat(byte[]... parts) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (byte[] part : parts)
            bytes.write(part);
        return bytes.toByteArray();
    }

    private static void check(boolean condition, String description) {
        if (!condition)
            throw new AssertionError(description);
    }
}
