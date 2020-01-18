package org.helioviewer.jhv.io;

import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import com.google.common.util.concurrent.FutureCallback;

class LoadZip implements Callable<Void> {

    static Future<Void> get(URI uri) {
        return EventQueueCallbackExecutor.pool.submit(new LoadZip(uri), new Callback());
    }

    private final URI remoteUri;

    private LoadZip(URI _remoteUri) {
        remoteUri = _remoteUri;
    }

    @Override
    public Void call() throws Exception {
        HashSet<Path> exclude = new HashSet<>();
        URI uri = NetFileCache.get(remoteUri);
        String tmpDir = FileUtils.tempDir(JHVGlobals.fileCacheDir, uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/') + 1)) + ".x").toString();
        try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + uri), new HashMap<>())) {
            for (Path root : zipfs.getRootDirectories()) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.jhv")) {
                    for (Path entry : stream) {
                        Path ext = Path.of(tmpDir + entry);
                        exclude.add(ext);
                        Files.copy(entry, ext, StandardCopyOption.REPLACE_EXISTING);
                        Load.state.get(ext.toUri());
                    }
                }
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.json")) {
                    for (Path entry : stream) {
                        Path ext = Path.of(tmpDir + entry);
                        exclude.add(ext);
                        Files.copy(entry, ext, StandardCopyOption.REPLACE_EXISTING);
                        Load.request.get(ext.toUri());
                    }
                }
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                    for (Path entry : stream) {
                        Path ext = Path.of(tmpDir + entry);
                        if (exclude.contains(ext))
                            continue;
                        Files.copy(entry, ext, StandardCopyOption.REPLACE_EXISTING);
                        Load.image.get(ext.toUri());
                    }
                }
            }
        }
        return null;
    }

    private static class Callback implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file: ", t);
            Message.err("An error occurred while opening the remote file: ", t.getMessage(), false);
        }

    }

}
