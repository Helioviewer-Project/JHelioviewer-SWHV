package org.helioviewer.jhv.view.j2k.jpip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;

import kdu_jni.KduException;

public class JPIPCacheManager {

    static {
        Logger.getLogger("org.ehcache").setLevel(Level.WARNING); // shutup Ehcache info logs
    }

    private static final int MAGIC = 0x4A504950; // JPIP
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 8;
    private static final Duration EXPIRY = Duration.ofDays(7);
    private static final long MAX_STREAM_CACHE_SIZE = 8L * 1024 * 1024 * 1024;
    private static final Path levelCacheDir = Path.of(JHVDirectory.CACHE.getPath(), "JPIPLevel-5");
    private static final Path streamCacheDir = Path.of(JHVDirectory.CACHE.getPath(), "JPIPStream-5");

    private static final Object cacheLock = new Object();

    private static PersistentCacheManager levelManager;
    private static Cache<String, Integer> levelCache;
    private static Thread hook;
    private static long generation;
    private static volatile boolean enabled;
    private static boolean failureLogged;
    private static final Writer NOOP_WRITER = new Writer();

    public static void init() {
        synchronized (cacheLock) {
            enabled = false;
            try {
                ExpiryPolicy<Object, Object> expiryPolicy = ExpiryPolicyBuilder.timeToIdleExpiration(EXPIRY);

                levelManager = CacheManagerBuilder.newCacheManagerBuilder()
                        .with(CacheManagerBuilder.persistence(levelCacheDir.toString()))
                        .withCache("JPIPLevel", CacheConfigurationBuilder
                                .newCacheConfigurationBuilder(String.class, Integer.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .heap(10000, EntryUnit.ENTRIES)
                                                .disk(10, MemoryUnit.MB, true))
                                .withExpiry(expiryPolicy))
                        .build(true);

                deleteDirs("JPIPLevel-3", "JPIPStream-3", "JPIPLevel-4", "JPIPStream-4"); // delete old formats
                Files.createDirectories(streamCacheDir);
                deleteTempStreams();
                deleteStaleStreams();
                trimStreamCache();

                levelCache = levelManager.getCache("JPIPLevel", String.class, Integer.class);
                failureLogged = false;
                generation++;
                enabled = true;
            } catch (IOException | RuntimeException e) {
                Log.error(e);
                closeUnlocked();
            }
        }

        if (hook == null) {
            hook = new Thread(JPIPCacheManager::close);
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }

    public static boolean restore(@Nonnull String key, int level, @Nonnull JPIPCache cache, int frame) {
        if (!enabled)
            return false;

        try {
            synchronized (cacheLock) {
                if (!enabled)
                    return false;

                Integer clevel = levelCache.get(key);
                if (clevel == null || clevel > level)
                    return false;

                Path file = streamPath(key);
                if (!Files.isRegularFile(file)) {
                    levelCache.remove(key);
                    return false;
                }

                read(file, cache, frame);
                try {
                    Files.setLastModifiedTime(file, FileTime.from(Instant.now()));
                } catch (IOException e) {
                    Log.error(e);
                }
            }
            return true;
        } catch (Exception e) { // might get interrupted
            Log.error(e);
            synchronized (cacheLock) {
                try {
                    levelCache.remove(key);
                } catch (Exception ignore) {
                }
                deleteFile(streamPath(key));
            }
            return false;
        }
    }

    @Nonnull
    public static Writer writer(@Nullable String key, int level) {
        if (key == null || !enabled)
            return NOOP_WRITER;

        Path tempFile = null;
        try {
            synchronized (cacheLock) {
                if (!enabled)
                    return NOOP_WRITER;

                Files.createDirectories(streamCacheDir);
                Path file = streamPath(key);
                tempFile = streamCacheDir.resolve(file.getFileName() + "." + UUID.randomUUID() + ".tmp");
                boolean append = Files.exists(file);
                if (append)
                    Files.copy(file, tempFile);

                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile,
                        append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)));
                if (!append) {
                    out.writeInt(MAGIC);
                    out.writeInt(VERSION);
                }
                return new Writer(key, level, tempFile, out, append && Files.size(file) > HEADER_BYTES, generation);
            }
        } catch (Exception e) {
            disableAfterFailure(e);
            if (tempFile != null)
                deleteFile(tempFile);
            return NOOP_WRITER;
        }
    }

    static Writer noopWriter() {
        return NOOP_WRITER;
    }

    private static void read(Path file, JPIPCache cache, int frame) throws IOException, KduException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION)
                throw new IOException("Unsupported JPIP cache file: " + file);

            while (true) {
                JPIPSegment seg = new JPIPSegment();
                try {
                    seg.klassID = in.readInt();
                } catch (EOFException e) {
                    return;
                }
                seg.binID = in.readLong();
                seg.offset = in.readInt();
                seg.length = in.readInt();
                if (seg.length < 0)
                    throw new IOException("Invalid JPIP cache record length: " + seg.length);
                seg.isFinal = in.readBoolean();
                if (seg.length > 0) {
                    seg.data = new byte[seg.length];
                    in.readFully(seg.data);
                }
                cache.put(frame, seg);
            }
        }
    }

    private static Path streamPath(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return streamCacheDir.resolve(HexFormat.of().formatHex(hash) + ".jps");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteStaleStreams() throws IOException {
        Instant cutoff = Instant.now().minus(EXPIRY);
        try (Stream<Path> files = Files.list(streamCacheDir)) {
            files.filter(Files::isRegularFile)
                    .filter(JPIPCacheManager::isCommittedStreamFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            Log.error(e);
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            Log.error(e);
                        }
                    });
        }
    }

    private static void deleteTempStreams() throws IOException {
        try (Stream<Path> files = Files.list(streamCacheDir)) {
            files.filter(Files::isRegularFile)
                    .filter(JPIPCacheManager::isTempStreamFile)
                    .forEach(JPIPCacheManager::deleteFile);
        }
    }

    private static void trimStreamCache() throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(streamCacheDir)) {
            files = stream.filter(Files::isRegularFile)
                    .filter(JPIPCacheManager::isCommittedStreamFile)
                    .toList();
        }

        long size = 0;
        for (Path file : files)
            size += Files.size(file);
        if (size <= MAX_STREAM_CACHE_SIZE)
            return;

        files = new ArrayList<>(files);
        files.sort(Comparator.comparingLong(JPIPCacheManager::lastModifiedMillis));
        for (Path file : files) {
            long fileSize = Files.size(file);
            Files.deleteIfExists(file);
            size -= fileSize;
            if (size <= MAX_STREAM_CACHE_SIZE)
                return;
        }
    }

    private static boolean isCommittedStreamFile(Path file) {
        return file.getFileName().toString().endsWith(".jps");
    }

    private static boolean isTempStreamFile(Path file) {
        return file.getFileName().toString().endsWith(".tmp");
    }

    private static long lastModifiedMillis(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            Log.error(e);
            return Long.MIN_VALUE;
        }
    }

    private static void deleteDirs(String... dirs) {
        for (String dir : dirs) { // delete old versions
            try {
                FileUtils.deleteDir(Path.of(JHVDirectory.CACHE.getPath(), dir));
            } catch (Exception ignore) {}
        }
    }

    private static void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            Log.error(e);
        }
    }

    private static void commitTempFile(String key, int level, Path tempFile, long writerGeneration) {
        try {
            synchronized (cacheLock) {
                if (!enabled) {
                    deleteFile(tempFile);
                    return;
                }

                if (writerGeneration != generation) {
                    deleteFile(tempFile);
                    return;
                }

                Integer clevel = levelCache.get(key);
                if (clevel != null && clevel <= level) {
                    deleteFile(tempFile);
                    return;
                }

                move(tempFile, streamPath(key));
                levelCache.put(key, level);
                trimStreamCache();
            }
        } catch (Exception e) {
            disableAfterFailure(e);
            deleteFile(tempFile);
        }
    }

    private static void disableAfterFailure(Exception e) {
        synchronized (cacheLock) {
            if (!failureLogged) {
                Log.error(e);
                failureLogged = true;
            }
            closeUnlocked();
        }
    }

    public static final class Writer implements AutoCloseable {

        private final String key;
        private final int level;
        private final Path tempFile;
        private final long generation;

        private DataOutputStream out;
        private boolean active;
        private boolean hasRecords;

        private Writer() {
            key = "";
            level = 0;
            tempFile = null;
            generation = 0;
        }

        private Writer(String _key, int _level, Path _tempFile, DataOutputStream _out, boolean _hasRecords, long _generation) {
            key = _key;
            level = _level;
            tempFile = _tempFile;
            generation = _generation;
            out = _out;
            hasRecords = _hasRecords;
            active = true;
        }

        void write(JPIPSegment seg) {
            if (!active || seg.klassID == Constants.KDU.META_DATABIN)
                return;

            try {
                out.writeInt(seg.klassID);
                out.writeLong(seg.binID);
                out.writeInt(seg.offset);
                out.writeInt(seg.length);
                out.writeBoolean(seg.isFinal);
                if (seg.length > 0)
                    out.write(seg.data, 0, seg.length);
                hasRecords = true;
            } catch (Exception e) {
                disableAfterFailure(e);
                close();
            }
        }

        public void commit() {
            if (!active)
                return;

            active = false;
            if (!closeOutput())
                return;

            if (!hasRecords) {
                deleteFile(tempFile);
                return;
            }

            commitTempFile(key, level, tempFile, generation);
        }

        @Override
        public void close() {
            if (!active)
                return;

            active = false;
            closeOutput();
            if (tempFile != null)
                deleteFile(tempFile);
        }

        private boolean closeOutput() {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.error(e);
                    deleteFile(tempFile);
                    return false;
                } finally {
                    out = null;
                }
            }
            return true;
        }
    }

    private static void close() {
        synchronized (cacheLock) {
            closeUnlocked();
        }
    }

    private static void closeUnlocked() {
        try {
            if (levelManager != null)
                levelManager.close();
        } catch (Exception e) {
            Log.error(e);
        } finally {
            levelManager = null;
            levelCache = null;
            enabled = false;
        }
    }

    public static void clear() {
        synchronized (cacheLock) {
            if (!enabled)
                return;

            PersistentCacheManager manager = levelManager;
            closeUnlocked();
            try {
                if (manager != null)
                    manager.destroy();
                FileUtils.deleteDir(streamCacheDir);
            } catch (Exception e) {
                Log.error(e);
            }
        }
        init();
    }

    public static long getSize() {
        if (!enabled)
            return 0;

        long size = 0;
        try {
            size += FileUtils.diskUsage(levelCacheDir);
            size += FileUtils.diskUsage(streamCacheDir);
        } catch (Exception e) {
            Log.error(e);
        }
        return size;
    }

}
