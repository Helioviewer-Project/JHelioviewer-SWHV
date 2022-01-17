package org.helioviewer.jhv.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import okio.Okio;
import okio.BufferedSource;

public class FileUtils {

    public static InputStream getResource(String path) throws IOException {
        InputStream is = FileUtils.class.getResourceAsStream(path);
        if (is == null)
            throw new IOException("Resource " + path + " not found");
        return is;
    }

    public static InputStream decompressStream(InputStream input) throws IOException {
        PushbackInputStream pb = new PushbackInputStream(input, 2); // pushbackstream for looking ahead
        byte[] signature = new byte[2];
        int len = pb.read(signature); // read the signature
        pb.unread(signature, 0, len); // push back the signature to the stream
        if (signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b) // check if matches standard gzip magic number
            return new GZIPInputStream(pb);
        else
            return pb;
    }

    public static String streamToString(InputStream is) throws IOException {
        try (BufferedSource buffer = Okio.buffer(Okio.source(is))) {
            return buffer.readString(StandardCharsets.UTF_8);
        }
    }

    public static long diskUsage(Path path) throws IOException {
        AtomicLong size = new AtomicLong(0);

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });
        return size.get();
    }

    private static class DeleteFileVisitor extends SimpleFileVisitor<Path> {

        private final long olderThan;
        private final boolean deleteDir;

        DeleteFileVisitor(long _olderThan, boolean _deleteDir) {
            olderThan = _olderThan;
            deleteDir = _deleteDir;
        }

        private static FileVisitResult delete(Path file) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (olderThan < 0 || System.currentTimeMillis() - attrs.lastModifiedTime().toMillis() > olderThan)
                return delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return delete(file);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return deleteDir ? delete(dir) : FileVisitResult.CONTINUE;
        }

    }

    private static final DeleteFileVisitor nukeVisitor = new DeleteFileVisitor(-1, true);

    public static void deleteDir(Path path) throws IOException {
        Files.walkFileTree(path, nukeVisitor);
    }

    public static void deleteFromDir(Path path, long olderThan, boolean deleteDir) throws IOException {
        Files.walkFileTree(path, new DeleteFileVisitor(olderThan, deleteDir));
    }

    public static void deleteFromDir(Path path, DirectoryStream.Filter<Path> filter) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {
            for (Path p : stream)
                Files.delete(p);
        }
    }

    public static List<URI> listDir(Path path) throws IOException {
        try (Stream<Path> stream = Files.find(path, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            return stream.map(Path::toUri).collect(Collectors.toList());
        }
    }

    private static final String lockSuffix = ".lck";

    public static File tempDir(File parent, String name) throws IOException {
        // delete all directories without a lock file
        FileFilter filter = p -> p.getName().startsWith(name) && !p.getName().endsWith(lockSuffix);
        File[] dirs = parent.listFiles(filter);
        if (dirs == null)
            throw new IOException("I/O error or not a directory: " + parent);

        for (File dir : dirs) {
            if (new File(dir + lockSuffix).exists())
                continue;
            deleteDir(dir.toPath());
        }

        File tempDir = Files.createTempDirectory(parent.toPath(), name).toFile();
        File lock = new File(tempDir + lockSuffix);
        lock.createNewFile();
        lock.deleteOnExit();

        return tempDir;
    }

}
