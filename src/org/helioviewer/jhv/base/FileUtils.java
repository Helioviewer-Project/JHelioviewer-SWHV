package org.helioviewer.jhv.base;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

public class FileUtils {

    private static final int BUFSIZ = 65536;

    public static InputStream newBufferedInputStream(File src) throws IOException {
        return new BufferedInputStream(Files.newInputStream(src.toPath()), BUFSIZ);
    }

    public static InputStream getResourceInputStream(String resourcePath) {
        return FileUtils.class.getResourceAsStream(resourcePath);
    }

    public static String convertStreamToString(InputStream is) {
        try (Scanner s = new Scanner(is, StandardCharsets.UTF_8.name())) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
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

}
