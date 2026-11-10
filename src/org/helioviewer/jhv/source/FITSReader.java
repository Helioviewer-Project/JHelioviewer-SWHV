package org.helioviewer.jhv.source;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.thread.ParallelRange;

import org.lwjgl.system.LibraryResource;

// Java owns input storage. CFITSIO owns only handles scoped to each native call.
@SuppressWarnings("restricted")
public final class FITSReader {

    private static final MethodHandle read;
    private static final MethodHandle readMask;

    static {
        if (Platform.getResourceDir() == null)
            Platform.init();
        Path path = LibraryResource.load(FITSReader.class, "jhv",
                Platform.getResourceDir().substring(1) + System.mapLibraryName("jhvfits"));
        SymbolLookup library = SymbolLookup.libraryLookup(path, Arena.global());
        try {
            MethodHandle initialize = Linker.nativeLinker().downcallHandle(library.find("jhv_fits_init").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT));
            if ((int) initialize.invokeExact() != 0)
                throw new IllegalStateException("CFITSIO initialization failed or non-reentrant build");
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
        read = Linker.nativeLinker().downcallHandle(library.find("jhv_fits_read_float").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        readMask = Linker.nativeLinker().downcallHandle(library.find("jhv_fits_read_mask").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                        ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
    }

    static float[] read(Path path, int hdu, int bitpix, int width, int height, int plane,
                       int tileRows, int stepX, int stepY) throws IOException, InterruptedException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ); Arena arena = Arena.ofShared()) {
            // Inputs remain stable during reads. Arena closure unmaps before the channel closes.
            MemorySegment input = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size(), arena);
            int count = Math.multiplyExact((width - 1) / stepX + 1, (height - 1) / stepY + 1);
            MemorySegment output = arena.allocate(Math.multiplyExact((long) count, Float.BYTES), Float.BYTES);
            if (stepX != 1 || stepY != 1) {
                invoke(input, hdu, bitpix, width, height, plane, 0, height, stepX, stepY, output);
            } else {
                int tiles = (height - 1) / tileRows + 1;
                AtomicReference<RuntimeException> failure = new AtomicReference<>();
                AtomicReference<Error> fatal = new AtomicReference<>();
                // A tile belongs to one worker, including a partial final tile.
                ParallelRange.run(tiles, (from, to) -> {
                    int first = from * tileRows;
                    int last = (int) Math.min((long) to * tileRows, height);
                    long offset = (long) first * width * Float.BYTES;
                    long bytes = (long) (last - first) * width * Float.BYTES;
                    // Join every worker before releasing the shared arena, including on failure.
                    try {
                        invoke(input, hdu, bitpix, width, height, plane, first, last - first, 1, 1,
                                output.asSlice(offset, bytes));
                    } catch (RuntimeException e) {
                        failure.compareAndSet(null, e);
                    } catch (Error e) {
                        fatal.compareAndSet(null, e);
                    }
                });
                if (fatal.get() != null)
                    throw fatal.get();
                if (failure.get() != null)
                    throw failure.get();
            }
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            return output.toArray(ValueLayout.JAVA_FLOAT);
        }
    }

    // Packaged resources have no filesystem path and use the same native pixel reader.
    public static byte[] readMask(byte[] bytes, int width, int height) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment input = arena.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
            MemorySegment output = arena.allocate(Math.multiplyExact(width, height));
            MemorySegment error = arena.allocate(128);
            int status = (int) readMask.invokeExact(input, input.byteSize(), width, height,
                    output, output.byteSize(), error);
            if (status != 0)
                throw new IllegalArgumentException("CFITSIO " + status + ": " + error.getString(0));
            return output.toArray(ValueLayout.JAVA_BYTE);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static void invoke(MemorySegment input, int hdu, int bitpix, int width, int height,
                               int plane, int first, int rows, int stepX, int stepY, MemorySegment output) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = arena.allocate(128);
            int status = (int) read.invokeExact(input, input.byteSize(), hdu, bitpix, width, height,
                    plane, first, rows, stepX, stepY, output, output.byteSize(), error);
            if (status != 0)
                throw new IllegalArgumentException("CFITSIO " + status + ": " + error.getString(0));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private FITSReader() {}
}
