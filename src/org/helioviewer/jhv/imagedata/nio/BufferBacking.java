package org.helioviewer.jhv.imagedata.nio;

import java.awt.image.DataBuffer;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;

final class BufferBacking {

    private final Buffer buffer;
    private Arena arena;
    private final Path tempPath;

    private BufferBacking(Buffer _buffer, Arena _arena, Path _tempPath) {
        buffer = _buffer;
        arena = _arena;
        tempPath = _tempPath;
    }

    static BufferBacking allocate(int type, int size, int numBanks) {
        int componentSize = DataBuffer.getDataTypeSize(type) / 8;
        long length = ((long) size) * componentSize * numBanks;

        Arena arena = Arena.ofShared();
        try {
            MemorySegment segment = arena.allocate(length, componentSize);
            return new BufferBacking(createTypedView(type, segment.asByteBuffer()), arena, null);
        } catch (RuntimeException | Error e) {
            arena.close();
            throw e;
        }
    }

    static BufferBacking mapFile(int type, int size, int numBanks) throws IOException {
        int componentSize = DataBuffer.getDataTypeSize(type) / 8;
        long length = ((long) size) * componentSize * numBanks;

        Path tempPath = Files.createTempFile(JHVGlobals.exportCacheDir.toPath(), "mbuf", null);
        Arena arena = Arena.ofShared();
        try (FileChannel channel = FileChannel.open(tempPath, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            channel.truncate(length);
            MemorySegment segment = channel.map(FileChannel.MapMode.READ_WRITE, 0, length, arena);
            return new BufferBacking(createTypedView(type, segment.asByteBuffer()), arena, tempPath);
        } catch (IOException | RuntimeException | Error e) {
            arena.close();
            Files.deleteIfExists(tempPath);
            throw e;
        }
    }

    Buffer buffer() {
        return buffer;
    }

    void close() {
        if (arena == null)
            return;

        arena.close();
        arena = null;
        if (tempPath != null) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException e) {
                Log.warn("Failed to delete mapped buffer file: " + tempPath, e);
            }
        }
    }

    private static Buffer createTypedView(int type, ByteBuffer byteBuffer) {
        ByteBuffer orderedBuffer = byteBuffer.order(ByteOrder.nativeOrder());
        return switch (type) {
            case DataBuffer.TYPE_BYTE -> orderedBuffer;
            case DataBuffer.TYPE_USHORT -> orderedBuffer.asShortBuffer();
            case DataBuffer.TYPE_INT -> orderedBuffer.asIntBuffer();
            default -> throw new IllegalArgumentException("Unsupported data type: " + type);
        };
    }

}
