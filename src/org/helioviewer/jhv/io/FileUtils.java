package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.AppThread;

public final class FileUtils {

    public static InputStream getResource(String path) throws IOException {
        InputStream is = FileUtils.class.getResourceAsStream(path);
        if (is == null)
            throw new IOException("Resource " + path + " not found");
        return is;
    }

    public static String readResourceString(String path) throws IOException {
        try (InputStream is = getResource(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static long diskUsage(Path path) throws IOException {
        long[] size = {0};
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });
        return size[0];
    }

    public static List<URI> unZip(URI uri) throws IOException {
        List<URI> uriList = new ArrayList<>();
        String uriPath = uri.getPath();
        Path targetDir = tempDir(Directories.fileCacheDir, uriPath.substring(Math.max(0, uriPath.lastIndexOf('/') + 1)) + ".x").toPath();

        try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + uri), Collections.emptyMap())) {
            for (Path root : zipfs.getRootDirectories()) {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        // Make sure that we conserve the hierachy of files and folders inside the zip
                        Path relativePathInZip = root.relativize(filePath);
                        Path targetPath = targetDir.resolve(relativePathInZip.toString()).normalize();
                        if (targetPath.startsWith(targetDir)) {
                            Path parent = targetPath.getParent();
                            if (parent != null)
                                Files.createDirectories(parent);
                            // And extract the file
                            Files.copy(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            uriList.add(targetPath.toUri());
                        } // else attempted path traversal

                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        return uriList;
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
            return stream.map(Path::toUri).toList();
        }
    }

    private static List<URI> expandURI(URI uri) throws IOException {
        Path path;
        try {
            path = Path.of(uri);
        } catch (Exception e) {
            return List.of(uri);
        }

        if (!Files.isDirectory(path))
            return List.of(uri);
        return listDir(path);
    }

    public static void resolveURIListOffEDT(List<URI> uris, String threadName, Consumer<List<URI>> callback) {
        AppThread.create(() -> {
            List<URI> resolved = new ArrayList<>();
            for (URI uri : uris) {
                try {
                    resolved.addAll(expandURI(uri));
                } catch (Exception e) {
                    Log.warn("Error reading directory: " + uri, e);
                    resolved.add(uri);
                }
            }
            EventQueue.invokeLater(() -> callback.accept(resolved));
        }, threadName).start();
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

    private FileUtils() {}
}
