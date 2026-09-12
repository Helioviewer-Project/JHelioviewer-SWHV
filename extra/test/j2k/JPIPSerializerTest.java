package org.helioviewer.jhv.view.j2k.jpip;

import java.lang.reflect.RecordComponent;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import org.ehcache.spi.serialization.SerializerException;

public final class JPIPSerializerTest {
    public static void main(String[] arguments) throws Exception {
        JPIPCacheManager.EntrySerializer serializer = new JPIPCacheManager.EntrySerializer();
        JPIPStream stream = new JPIPStream();
        for (int i = 0; i < 3; i++) {
            byte[] data = new byte[i * 10000];
            Arrays.fill(data, (byte) (i + 17));
            stream.databins.add(new JPIPStream.Databin(i, (1L << 40) + i, i != 1, data));
        }
        JPIPCacheManager.Entry original = new JPIPCacheManager.Entry(2, stream);
        ByteBuffer binary = serializer.serialize(original);
        ByteBuffer padded = ByteBuffer.allocateDirect(binary.remaining() + 9);
        padded.position(5).put(binary.duplicate()).flip().position(5);
        ByteBuffer input = padded.asReadOnlyBuffer();
        JPIPCacheManager.Entry restored = serializer.read(input);
        check(input.position() == 5, "read changed buffer position");
        check(restored.level() == 2 && restored.stream().databins.size() == 3, "entry header");
        for (int i = 0; i < 3; i++) {
            JPIPStream.Databin expected = stream.databins.get(i);
            JPIPStream.Databin actual = restored.stream().databins.get(i);
            // Include every record component automatically when the cache representation changes.
            for (RecordComponent component : JPIPStream.Databin.class.getRecordComponents())
                check(Objects.deepEquals(component.getAccessor().invoke(expected), component.getAccessor().invoke(actual)),
                        "databin round trip: " + component.getName());
        }
        check(serializer.equals(original, input), "equal encoding");
        check(input.position() == 5, "equals changed buffer position");
        check(!serializer.equals(new JPIPCacheManager.Entry(1, stream), input), "different level");
        JPIPCacheManager.Entry empty = new JPIPCacheManager.Entry(0, new JPIPStream());
        check(serializer.read(serializer.serialize(empty)).stream().databins.isEmpty(), "empty stream");
        for (int length : new int[]{0, 7, 8, 24, binary.limit() - 1}) {
            ByteBuffer truncated = binary.duplicate().limit(length);
            rejects(serializer, truncated);
        }
        ByteBuffer corrupt = ByteBuffer.allocate(binary.remaining()).put(binary.duplicate()).flip();
        corrupt.putInt(4, Integer.MAX_VALUE);
        rejects(serializer, corrupt);
        corrupt.putInt(4, 3).putInt(21, Integer.MAX_VALUE);
        rejects(serializer, corrupt);
        corrupt.putInt(21, -1);
        rejects(serializer, corrupt);
        corrupt.putInt(21, 0).put(20, (byte) 2);
        rejects(serializer, corrupt);
        ByteBuffer trailing = ByteBuffer.allocate(binary.remaining() + 1).put(binary.duplicate()).put((byte) 0).flip();
        rejects(serializer, trailing);
        System.out.println("PASS: JPIP cache binary round trips, buffer positions and malformed entries");
    }

    private static void rejects(JPIPCacheManager.EntrySerializer serializer, ByteBuffer buffer) {
        try {
            serializer.read(buffer);
            throw new AssertionError("Accepted malformed entry");
        } catch (SerializerException expected) {}
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private JPIPSerializerTest() {}
}
