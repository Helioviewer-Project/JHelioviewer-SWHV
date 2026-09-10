package org.helioviewer.jhv.view.j2k.jpip;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class JPIPStream {

    // A native-cache snapshot starts at offset zero. Its destination frame is supplied on restore.
    record Databin(int klassID, long binID, boolean complete, byte[] data) {}

    private static final int DATABIN_HEADER_SIZE = Integer.BYTES * 2 + Long.BYTES + 1;

    final ArrayList<Databin> databins = new ArrayList<>();

    long encodedSize() {
        long size = Integer.BYTES;
        for (Databin databin : databins)
            size += DATABIN_HEADER_SIZE + (long) databin.data().length;
        return size;
    }

    void write(ByteBuffer buffer) {
        buffer.putInt(databins.size());
        for (Databin databin : databins) {
            buffer.putInt(databin.klassID()).putLong(databin.binID());
            buffer.put((byte) (databin.complete() ? 1 : 0));
            buffer.putInt(databin.data().length).put(databin.data());
        }
    }

    static JPIPStream read(ByteBuffer buffer) {
        int count = buffer.getInt();
        if (count < 0 || count > buffer.remaining() / DATABIN_HEADER_SIZE)
            throw new IllegalArgumentException("Invalid JPIP cache databin count");

        JPIPStream stream = new JPIPStream();
        stream.databins.ensureCapacity(count);
        for (int i = 0; i < count; i++) {
            int klassID = buffer.getInt();
            long binID = buffer.getLong();
            byte complete = buffer.get();
            int length = buffer.getInt();
            if (length < 0 || length > buffer.remaining() || (complete != 0 && complete != 1))
                throw new IllegalArgumentException("Invalid JPIP cache databin");
            byte[] data = new byte[length];
            buffer.get(data);
            stream.databins.add(new Databin(klassID, binID, complete != 0, data));
        }
        if (buffer.hasRemaining())
            throw new IllegalArgumentException("Trailing JPIP cache data");
        return stream;
    }

}
