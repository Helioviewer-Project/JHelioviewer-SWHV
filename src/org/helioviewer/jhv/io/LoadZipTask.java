package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;

class LoadZipTask extends JHVWorker<Void, Void> {

    private final URI remoteUri;

    LoadZipTask(URI _remoteUri) {
        remoteUri = _remoteUri;
        setThreadName("MAIN--LoadZip");
    }

    @Nullable
    @Override
    protected Void backgroundWork() {
        try {
            HashSet<Path> exclude = new HashSet<>();
            URI uri = NetFileCache.get(remoteUri);
            String tmpDir = FileUtils.tempDir(JHVGlobals.fileCacheDir, uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/') + 1)) + ".x").toString();
            try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + uri), new HashMap<>())) {
                for (Path root : zipfs.getRootDirectories()) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.jhv")) {
                        for (Path entry: stream) {
                            Path ext = Paths.get(tmpDir + entry);
                            exclude.add(ext);
                            Files.copy(entry, ext, StandardCopyOption.REPLACE_EXISTING);
                            Load.state.get(ext.toUri());
                        }
                    }
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.json")) {
                        for (Path entry: stream) {
                            Path ext = Paths.get(tmpDir + entry);
                            exclude.add(ext);
                            Files.copy(entry, ext, StandardCopyOption.REPLACE_EXISTING);
                            Load.request.get(ext.toUri());
                        }
                    }
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                        for (Path entry: stream) {
                            Path ext = Paths.get(tmpDir + entry);
                            if (exclude.contains(ext))
                                continue;
                            Files.copy(entry, ext, StandardCopyOption.REPLACE_EXISTING);
                            Load.image.get(ext.toUri());
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.error(e);
        }
        return null;
    }

}
