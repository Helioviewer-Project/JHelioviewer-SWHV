package org.helioviewer.jhv.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

import okio.Okio;
import okio.BufferedSource;

public class FileUtils {

    public static InputStream getResource(String path) throws IOException {
        InputStream is = FileUtils.class.getResourceAsStream(path);
        if (is == null)
            throw new IOException("Resource " + path + " not found");
        return is;
    }

    public static String streamToString(InputStream is) throws IOException {
        try (BufferedSource buffer = Okio.buffer(Okio.source(is))) {
            return buffer.readString(StandardCharsets.UTF_8);
        }
    }

    public static long diskUsage(File dir) throws IOException {
        AtomicLong size = new AtomicLong(0);

        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });
        return size.get();
    }

    private static final SimpleFileVisitor<Path> nukeVisitor = new SimpleFileVisitor<Path>() {

        private FileVisitResult delete(Path file) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return delete(file);
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return delete(file);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return delete(dir);
        }

    };

    public static void deleteDir(File dir) throws IOException {
        Files.walkFileTree(dir.toPath(), nukeVisitor);
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
            deleteDir(dir);
        }

        File tempDir = Files.createTempDirectory(parent.toPath(), name).toFile();
        File lock = new File(tempDir + lockSuffix);
        lock.createNewFile();
        lock.deleteOnExit();

        return tempDir;
    }

}
