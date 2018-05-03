/*
 * Copyright (c) 2010, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.helioviewer.jhv.base.image;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.*;
import java.nio.channels.FileChannel;

import org.helioviewer.jhv.JHVGlobals;

/**
 * A {@code DataBuffer} implementation that is backed by a memory mapped file.
 * Memory will be allocated outside the normal JVM heap, allowing more efficient
 * memory usage for large buffers.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedFileBuffer.java,v 1.0 Jun 12, 2010 4:56:51 PM haraldk Exp$
 * @see java.nio.channels.FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)
 */
abstract class MappedFileBuffer extends DataBuffer {

    private Buffer buffer;

    private MappedFileBuffer(int type, int size, int numBanks) throws IOException {
        super(type, size, numBanks);

        int componentSize = DataBuffer.getDataTypeSize(type) / 8;
        File tempFile = File.createTempFile("mfilebuf", null, JHVGlobals.exportCacheDir);

        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw"); FileChannel channel = raf.getChannel()) {
            long length = ((long) size) * componentSize * numBanks;
            raf.setLength(length);

            ByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, length).order(ByteOrder.nativeOrder());
            switch (type) {
                case DataBuffer.TYPE_BYTE:
                    buffer = byteBuffer;
                    break;
                case DataBuffer.TYPE_USHORT:
                    buffer = byteBuffer.asShortBuffer();
                    break;
                case DataBuffer.TYPE_INT:
                    buffer = byteBuffer.asIntBuffer();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + type);
            }
        } finally {
            if (!tempFile.delete()) { // NOTE: File can't be deleted right now on Windows, as the file is open. Let JVM clean up later
                tempFile.deleteOnExit();
            }
        }
    }

    Buffer getBuffer() {
        return buffer;
    }

    void free() {
        buffer = null;
    }

    @Override
    public String toString() {
        return String.format("MappedFileBuffer: %s", buffer);
    }

    // TODO: Is throws IOException a good idea?

    public static DataBuffer create(int type, int size, int numBanks) throws IOException {
        switch (type) {
            case DataBuffer.TYPE_BYTE:
                return new DataBufferByte(size, numBanks);
            case DataBuffer.TYPE_USHORT:
                return new DataBufferUShort(size, numBanks);
            case DataBuffer.TYPE_INT:
                return new DataBufferInt(size, numBanks);
            default:
                throw new IllegalArgumentException("Unsupported data type: " + type);
        }
    }

    public static class DataBufferByte extends MappedFileBuffer {
        private final ByteBuffer buffer;

        DataBufferByte(int size, int numBanks) throws IOException {
            super(DataBuffer.TYPE_BYTE, size, numBanks);
            buffer = (ByteBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i) & 0xff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, (byte) val);
        }
    }

    static class DataBufferUShort extends MappedFileBuffer {
        private final ShortBuffer buffer;

        DataBufferUShort(int size, int numBanks) throws IOException {
            super(DataBuffer.TYPE_USHORT, size, numBanks);
            buffer = (ShortBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i) & 0xffff;
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, (short) val);
        }
    }

    static class DataBufferInt extends MappedFileBuffer {
        private final IntBuffer buffer;

        DataBufferInt(int size, int numBanks) throws IOException {
            super(DataBuffer.TYPE_INT, size, numBanks);
            buffer = (IntBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, val);
        }
    }

}
