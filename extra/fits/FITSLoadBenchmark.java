import java.io.File;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageFilter;
import org.helioviewer.jhv.view.uri.FITSImage;

import nom.tam.fits.FitsFactory;

public final class FITSLoadBenchmark {

    private static final Method freeImageBuffer = freeImageBufferMethod();

    private enum Mode {
        Image, Buffer
    }

    private record Options(Mode mode, ImageFilter.Type filter, int warmup, int iterations, boolean recursive, boolean checksum,
                           List<Path> inputs) {}

    private record Result(ImageBuffer buffer, String checksum) {}

    private FITSLoadBenchmark() {}

    public static void main(String[] args) throws Exception {
        Options options = parseOptions(args);
        if (options.inputs().isEmpty()) {
            usage();
            System.exit(2);
        }

        FitsFactory.setUseHierarch(true);
        FitsFactory.setLongStringsEnabled(true);

        List<Path> files = collectFiles(options.inputs(), options.recursive());
        if (files.isEmpty()) {
            System.err.println("No FITS files found");
            System.exit(1);
        }

        FITSImage reader = new FITSImage();
        System.out.println("file,bytes,width,height,format,mode,filter,iteration,total_ms,checksum,status");
        for (Path file : files)
            benchmarkFile(reader, file, options);
    }

    private static void benchmarkFile(FITSImage reader, Path path, Options options) {
        File file = path.toFile();

        try {
            for (int i = 0; i < options.warmup(); i++)
                free(load(reader, file, options));

            for (int i = 0; i < options.iterations(); i++) {
                long start = System.nanoTime();
                Result result = load(reader, file, options);
                long elapsed = System.nanoTime() - start;
                ImageBuffer buffer = result.buffer();

                System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%s,%s,%s,%d,%.3f,%s,OK%n",
                        csv(path.toString()),
                        Files.size(path),
                        buffer.width,
                        buffer.height,
                        buffer.format,
                        options.mode(),
                        options.filter(),
                        i,
                        elapsed / 1_000_000.0,
                        result.checksum());

                free(result);
            }
        } catch (Exception e) {
            System.out.printf("%s,,,,,,,,,,%s%n", csv(path.toString()), csv(e.toString()));
        }
    }

    private static Result load(FITSImage reader, File file, Options options) throws Exception {
        ImageBuffer buffer = switch (options.mode()) {
            case Image -> reader.readImage(file).buffer();
            case Buffer -> reader.readImageBuffer(file, options.filter());
        };
        return new Result(buffer, options.checksum() ? String.format("%08x", checksum(buffer)) : "");
    }

    private static long checksum(ImageBuffer imageBuffer) {
        CRC32 crc = new CRC32();
        Buffer buffer = imageBuffer.buffer;
        if (buffer instanceof ByteBuffer byteBuffer) {
            int length = imageBuffer.width * imageBuffer.height * imageBuffer.format.bytes;
            for (int i = 0; i < length; i++)
                crc.update(byteBuffer.get(i));
        } else if (buffer instanceof ShortBuffer shortBuffer) {
            int length = imageBuffer.width * imageBuffer.height;
            for (int i = 0; i < length; i++) {
                short value = shortBuffer.get(i);
                crc.update(value & 0xff);
                crc.update((value >>> 8) & 0xff);
            }
        } else {
            throw new IllegalArgumentException("Unsupported image buffer: " + buffer.getClass().getName());
        }
        return crc.getValue();
    }

    private static void free(Result result) throws Exception {
        freeImageBuffer.invoke(result.buffer());
    }

    private static Method freeImageBufferMethod() {
        try {
            Method method = ImageBuffer.class.getDeclaredMethod("free");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static List<Path> collectFiles(List<Path> inputs, boolean recursive) throws Exception {
        List<Path> files = new ArrayList<>();
        for (Path input : inputs) {
            if (Files.isDirectory(input)) {
                try (var stream = recursive ? Files.walk(input) : Files.list(input)) {
                    stream.filter(Files::isRegularFile)
                            .filter(FITSLoadBenchmark::isFitsFile)
                            .forEach(files::add);
                }
            } else if (Files.isRegularFile(input) && isFitsFile(input)) {
                files.add(input);
            }
        }
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

    private static boolean isFitsFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".fits") || name.endsWith(".fit") || name.endsWith(".fts")
                || name.endsWith(".fits.gz") || name.endsWith(".fit.gz") || name.endsWith(".fts.gz");
    }

    private static Options parseOptions(String[] args) {
        Mode mode = Mode.Image;
        ImageFilter.Type filter = ImageFilter.Type.None;
        int warmup = 1;
        int iterations = 3;
        boolean recursive = true;
        boolean checksum = true;
        List<Path> inputs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--mode" -> mode = Mode.valueOf(requireValue(args, ++i, arg));
                case "--filter" -> filter = ImageFilter.Type.valueOf(requireValue(args, ++i, arg));
                case "--warmup" -> warmup = Integer.parseInt(requireValue(args, ++i, arg));
                case "--iterations" -> iterations = Integer.parseInt(requireValue(args, ++i, arg));
                case "--no-recursive" -> recursive = false;
                case "--no-checksum" -> checksum = false;
                case "--help", "-h" -> {
                    usage();
                    System.exit(0);
                }
                default -> inputs.add(Path.of(arg));
            }
        }

        if (warmup < 0 || iterations <= 0)
            throw new IllegalArgumentException("Invalid warmup/iteration count");
        if (mode == Mode.Image && filter != ImageFilter.Type.None)
            throw new IllegalArgumentException("--filter requires --mode Buffer");

        return new Options(mode, filter, warmup, iterations, recursive, checksum, inputs);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length)
            throw new IllegalArgumentException("Missing value for " + option);
        return args[index];
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static void usage() {
        System.err.println("Usage: extra/fits/run-benchmark.sh [options] <fits-file-or-directory>...");
        System.err.println("Options:");
        System.err.println("  --mode Image|Buffer       Image includes header XML path; Buffer decodes pixels only (default: Image)");
        System.err.println("  --filter None|MGN|WOW     Only valid with --mode Buffer (default: None)");
        System.err.println("  --warmup N                Warmup loads per file (default: 1)");
        System.err.println("  --iterations N            Measured loads per file (default: 3)");
        System.err.println("  --no-recursive            Do not recurse into input directories");
        System.err.println("  --no-checksum             Skip CRC32 output checksum");
    }
}
