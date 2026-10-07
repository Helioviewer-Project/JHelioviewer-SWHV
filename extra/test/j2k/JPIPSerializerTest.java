package org.helioviewer.jhv.view.j2k.jpip;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.ehcache.spi.serialization.SerializerException;

public final class JPIPSerializerTest {
    public static void main(String[] arguments) {
        JPIPCacheManager.EntrySerializer serializer = new JPIPCacheManager.EntrySerializer();
        byte[] block = new byte[30000];
        for (int i = 0; i < block.length; i++)
            block[i] = (byte) (i * 7);
        JPIPCacheManager.Entry original = new JPIPCacheManager.Entry(2, block);
        ByteBuffer binary = serializer.serialize(original);
        ByteBuffer padded = ByteBuffer.allocateDirect(binary.remaining() + 9);
        padded.position(5).put(binary.duplicate()).flip().position(5);
        ByteBuffer input = padded.asReadOnlyBuffer();
        JPIPCacheManager.Entry restored = serializer.read(input);
        check(input.position() == 5, "read changed buffer position");
        check(restored.level() == 2 && Arrays.equals(restored.block(), block), "entry round trip");
        check(serializer.equals(original, input), "equal encoding");
        check(input.position() == 5, "equals changed buffer position");
        check(!serializer.equals(new JPIPCacheManager.Entry(1, block), input), "different level");
        JPIPCacheManager.Entry empty = new JPIPCacheManager.Entry(0, new byte[0]);
        check(serializer.read(serializer.serialize(empty)).block().length == 0, "empty block");
        // The block is the client's: only a missing level can be told here.
        for (int length = 0; length < Integer.BYTES; length++) {
            try {
                serializer.read(binary.duplicate().limit(length));
                throw new AssertionError("Accepted malformed entry");
            } catch (SerializerException expected) {}
        }
        System.out.println("PASS: JPIP cache entry round trips, buffer positions and truncated entries");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private JPIPSerializerTest() {}
}
