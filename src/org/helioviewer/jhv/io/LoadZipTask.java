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

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;

class LoadZipTask extends JHVWorker<Void, Void> {

    private final URI uri;

    LoadZipTask(URI _uri) {
        uri = _uri;
        setThreadName("MAIN--LoadZip");
    }

    @Nullable
    @Override
    protected Void backgroundWork() {
        try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + uri), new HashMap<>())) {
            for (Path root : zipfs.getRootDirectories()) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                    for (Path entry: stream) {
                        Path ext = Paths.get(JHVDirectory.TEMP.getPath() + entry);
                        Files.copy(entry, ext, StandardCopyOption.REPLACE_EXISTING);
                        Load.image.get(ext.toUri());
                    }
                }
            }
        } catch (IOException e) {
            Log.error(e);
        }
        return null;
    }

}
