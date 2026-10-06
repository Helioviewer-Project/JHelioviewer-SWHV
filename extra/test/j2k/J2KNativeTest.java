package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sun.management.UnixOperatingSystemMXBean;

import kdu_jni.Jp2_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_compositor;

// Checks the native bridge through J2KNative. Kakadu's Java classes serve only
// as the pixel reference: the compositor, driven as J2KDecoder drives it.
public final class J2KNativeTest {

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static ByteBuffer body(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file)) {
            ByteBuffer data = ByteBuffer.allocateDirect(Math.toIntExact(channel.size()));
            while (data.hasRemaining()) {
                if (channel.read(data) < 0) throw new IOException("Truncated test file");
            }
            return data.flip();
        }
    }

    private static byte[] pixels(J2KNative source, int frame, int level) throws IOException {
        J2KNative.Frame info = source.frame(frame);
        int width = info.width()[level], height = info.height()[level];
        int size = width * height * info.channels();
        ByteBuffer output = ByteBuffer.allocateDirect(size + 16);
        output.position(7).limit(7 + size);
        try (J2KNative.Decode job = source.beginDecode(frame, level)) {
            String warning = job.run(0, 0, width, height, output);
            check(warning == null, "decoder warning: " + warning);
        }
        check(output.position() == 7 + size, "output position");
        byte[] bytes = new byte[size];
        output.clear().get(7, bytes);
        check(output.get(6) == 0 && output.get(7 + size) == 0, "output bounds");
        return bytes;
    }

    private record Reference(int width, int height, byte[] pixels) {}

    // One component of the codestream for one-byte frames, the rendered layer otherwise.
    private static Reference reference(Path path, int frame, J2KNative.Frame info, int level, int access) throws Exception {
        Jp2_family_src family = new Jp2_family_src();
        Jpx_source file = new Jpx_source();
        Kdu_region_compositor compositor = null;
        Kdu_quality_limiter limiter = new Kdu_quality_limiter(1f / 256);
        Kdu_dims dims = new Kdu_dims();
        Kdu_dims empty = new Kdu_dims();
        Kdu_dims changed = new Kdu_dims();
        try {
            family.Open(path.toString());
            check(file.Open(family, false) > 0, "reference JPX open failed");
            compositor = new Kdu_region_compositor(file);
            compositor.Set_quality_limiting(limiter, -1, -1);
            boolean gray = info.channels() == 1;
            if (gray)
                compositor.Add_primitive_ilayer(Math.toIntExact(info.stream()), new int[]{0}, access, empty, empty);
            else
                compositor.Add_ilayer(frame, empty, empty);
            compositor.Set_scale(false, false, false, 1f / (1 << level));
            check(compositor.Get_total_composition_dims(dims), "reference level failed");
            compositor.Set_buffer_surface(dims);
            Kdu_compositor_buf buffer = compositor.Get_composition_buffer(changed, true);
            while (!compositor.Is_processing_complete())
                check(compositor.Process(1_000_000, changed), "reference decoding failed");
            int width = dims.Access_size().Get_x(), height = dims.Access_size().Get_y();
            int[] words = new int[width * height];
            check(buffer.Get_region(dims, words), "reference pixels unavailable");
            byte[] out = new byte[words.length * info.channels()];
            for (int i = 0; i < words.length; i++) {
                if (gray) {
                    out[i] = (byte) words[i];
                } else {
                    out[4 * i] = (byte) (words[i] >>> 16);
                    out[4 * i + 1] = (byte) (words[i] >>> 8);
                    out[4 * i + 2] = (byte) words[i];
                    out[4 * i + 3] = (byte) (words[i] >>> 24);
                }
            }
            return new Reference(width, height, out);
        } finally {
            if (compositor != null) compositor.Native_destroy();
            limiter.Native_destroy();
            changed.Native_destroy();
            empty.Native_destroy();
            dims.Native_destroy();
            file.Native_destroy();
            family.Native_destroy();
        }
    }

    private static void compare(Path path, J2KNative source, int access) throws Exception {
        int frames = source.frames();
        for (int frame = 0; frame < frames; frame++) {
            source.xml(frame);
            source.palette(frame);
            J2KNative.Frame info = source.frame(frame);
            int levels = info.width().length;
            check(levels > 0 && info.ready() == levels, "local frame not ready");
            for (int level = 0; level < levels; level++) {
                Reference expected = reference(path, frame, info, level, access);
                String where = path + " frame " + frame + " level " + level;
                check(info.width()[level] == expected.width() && info.height()[level] == expected.height(),
                        "geometry differs from the present decoder: " + where);
                check(Arrays.equals(pixels(source, frame, level), expected.pixels()),
                        "pixels differ from the present decoder: " + where);
            }
        }
    }

    private static void local(Path path) throws Exception {
        try (J2KNative source = new J2KNative(path)) {
            compare(path, source, Kdu_global.KDU_WANT_CODESTREAM_COMPONENTS);
            // Jobs outlive their source.
            J2KNative.Frame info = source.frame(0);
            int level = info.width().length - 1, width = info.width()[level], height = info.height()[level];
            byte[] expected = pixels(source, 0, level);
            J2KNative.Decode first = source.beginDecode(0, level), second = source.beginDecode(0, level);
            source.close();
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                List<Future<byte[]>> results = new ArrayList<>();
                for (J2KNative.Decode job : List.of(first, second)) {
                    results.add(pool.submit(() -> {
                        try (job) {
                            ByteBuffer out = ByteBuffer.allocateDirect(expected.length);
                            job.run(0, 0, width, height, out);
                            byte[] bytes = new byte[expected.length];
                            out.get(0, bytes);
                            return bytes;
                        }
                    }));
                }
                for (Future<byte[]> result : results)
                    check(Arrays.equals(result.get(), expected), "decode after source close");
            }
            try { source.frames(); throw new AssertionError("closed source accepted"); }
            catch (CancellationException expectedFailure) { /* Expected. */ }
        }
        System.out.println("geometry and pixels at every level against the present decoder, jobs after close: " + path);
    }

    private static void failures(Path image) throws Exception {
        try (J2KNative missing = new J2KNative(image.resolveSibling("missing.jp2"))) { throw new AssertionError("missing file opened"); }
        catch (IOException expected) { /* Expected. */ }
        try (J2KNative source = new J2KNative(image)) {
            try { source.exportFrame(0); throw new AssertionError("local frame exported"); }
            catch (IOException expected) { /* Expected. */ }
            J2KNative.Frame info = source.frame(0);
            int level = info.width().length - 1, width = info.width()[level], height = info.height()[level];
            try { source.beginDecode(0, info.width().length).close(); throw new AssertionError("level out of range accepted"); }
            catch (IOException expected) { /* Expected. */ }
            try (J2KNative.Decode job = source.beginDecode(0, level)) {
                ByteBuffer small = ByteBuffer.allocateDirect(1);
                try { job.run(0, 0, width, height, small); throw new AssertionError("small buffer accepted"); }
                catch (IOException expected) { check(small.position() == 0, "failed output position"); }
                ByteBuffer output = ByteBuffer.allocateDirect(width * height * info.channels());
                try { job.run(-1, 0, width, height, output); throw new AssertionError("invalid region accepted"); }
                catch (IOException expected) { /* Expected. */ }
                job.run(0, 0, width, height, output);
                check(!output.hasRemaining(), "job retry after failures");
            }
        }
        UnixOperatingSystemMXBean bean = (UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long before = bean.getOpenFileDescriptorCount();
        for (int i = 0; i < 200; i++) {
            try (J2KNative source = new J2KNative(image)) {
                pixels(source, 0, source.frame(0).width().length - 1);
            }
        }
        check(bean.getOpenFileDescriptorCount() == before, "file descriptor leak");
        System.out.println("failures, retry and 200 open/decode/close cycles");
    }

    // A color description the client does not support, and a codestream Kakadu refuses.
    private static void unsupported(Path folder) throws Exception {
        Path sycc = folder.resolve("sycc.jp2");
        try (J2KNative source = new J2KNative(sycc)) {
            check(source.frame(0).channels() == 1, "unsupported color description not reduced to its first component");
            compare(sycc, source, Kdu_global.KDU_WANT_OUTPUT_COMPONENTS);
        }
        try (J2KNative source = new J2KNative(folder.resolve("malformed.jp2"))) {
            for (int i = 0; i < 2; i++) {
                try { source.frame(0); throw new AssertionError("malformed codestream described"); }
                catch (IOException expected) {
                    check(!expected.getMessage().startsWith("JPEG 2000 decoder error"), "Kakadu's text missing: " + expected.getMessage());
                }
            }
            check(source.frames() == 1, "failed inspection lost the source");
        }
        System.out.println("first component for an unsupported color description, Kakadu's error text");
    }

    // Responses written by esajpip's server code for one image, against that image read locally.
    private static void remote(Path folder, Path image) throws Exception {
        Path responses = folder.resolve("responses");
        int levels;
        byte[][] expected;
        try (J2KNative local = new J2KNative(image)) {
            levels = local.frame(0).width().length;
            expected = new byte[levels][];
            for (int level = 0; level < levels; level++)
                expected[level] = pixels(local, 0, level);
        }
        check(levels > 1, "the image needs several levels");

        ByteBuffer entry;
        try (J2KNative source = new J2KNative(null)) {
            check(source.response(body(responses.resolve("reduced-header.jpp"))) == 2, "header response");
            check(source.frames() == 1, "frame count");
            J2KNative.Frame info = source.frame(0);
            check(info.stream() == 0 && info.width().length == levels && info.ready() == 0, "frame with its header only");
            try { source.beginDecode(0, levels - 1).close(); throw new AssertionError("unready level decoded"); }
            catch (IOException expectedFailure) { check(expectedFailure.getMessage().contains("not ready"), expectedFailure.toString()); }

            check(source.response(body(responses.resolve("reduced.jpp"))) == 2, "coarsest window");
            info = source.frame(0);
            check(info.isReady(levels - 1) && !info.isReady(0), "coarsest level ready alone");
            check(Arrays.equals(pixels(source, 0, levels - 1), expected[levels - 1]), "coarsest pixels");

            check(source.response(body(responses.resolve("limited.jpp"))) == 4, "byte-limited window");
            check(!source.frame(0).isReady(0), "byte-limited window made the frame ready");
            check(source.response(body(responses.resolve("continuation.jpp"))) == 2, "continued window");
            check(source.frame(0).ready() == levels, "all levels ready");
            for (int level = 0; level < levels; level++)
                check(Arrays.equals(pixels(source, 0, level), expected[level]), "remote pixels at level " + level);
            entry = source.exportFrame(0);
        }

        try (J2KNative source = new J2KNative(null)) {
            source.response(body(responses.resolve("whole-header.jpp")));
            try { source.importFrame(0, entry.slice(0, entry.limit() - 1)); throw new AssertionError("truncated cache entry imported"); }
            catch (IOException expectedFailure) { check(source.frame(0).ready() == 0, "refused cache entry changed the source"); }
            source.importFrame(0, entry);
            check(source.frame(0).ready() == levels && source.exportFrame(0).equals(entry), "imported cache entry");
            check(Arrays.equals(pixels(source, 0, 0), expected[0]), "imported pixels");
            // The server knows nothing of an imported frame and sends it again.
            check(source.response(body(responses.resolve("whole.jpp"))) == 2, "replay after import");
            ByteBuffer changed = body(responses.resolve("whole.jpp"));
            int at = changed.limit() - 4;
            changed.put(at, (byte) (changed.get(at) ^ 1));
            try { source.response(changed); throw new AssertionError("conflicting response accepted"); }
            catch (J2KNative.Refused refused) { check(refused.getMessage().contains("conflicts"), refused.toString()); }
        }
        System.out.println("JPIP responses: readiness, continuation, pixels at every level, cache entry, refusal");
    }

    private static void benchmark(Path image) throws Exception {
        try (J2KNative source = new J2KNative(image)) {
            J2KNative.Frame info = source.frame(0);
            int width = info.width()[0], height = info.height()[0];
            ByteBuffer out = ByteBuffer.allocateDirect(width * height * info.channels());
            double[] times = new double[21];
            for (int i = -5; i < times.length; i++) {
                out.clear();
                long start = System.nanoTime();
                try (J2KNative.Decode job = source.beginDecode(0, 0)) {
                    job.run(0, 0, width, height, out);
                }
                if (i >= 0) times[i] = (System.nanoTime() - start) / 1e6;
            }
            Arrays.sort(times);
            System.out.printf("%dx%d warm decode with input creation: median %.2f ms, min %.2f ms%n", width, height, times[10], times[0]);
        }
    }

    // Arguments: Kakadu library, bridge library, folder made by run_native_test.sh, its image, then JP2/JPX files.
    public static void main(String[] args) throws Exception {
        System.load(args[0]);
        System.load(args[1]);
        J2KNative.init();
        Path folder = Path.of(args[2]), image = Path.of(args[4]);
        for (int i = 4; i < args.length; i++)
            local(Path.of(args[i]));
        failures(image);
        unsupported(folder);
        remote(folder, Path.of(args[3]));
        benchmark(image);
    }

}
