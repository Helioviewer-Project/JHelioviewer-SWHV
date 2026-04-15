package org.helioviewer.jhv.opengl.angle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Platform;
import org.helioviewer.jhv.io.FileUtils;

final class AngleLibraries {
    private static final String METAL_HOST_LIBRARY = "libjhvmetalhost.dylib";
    private static final Path MACOS_NATIVE_DIR = Path.of("lib", "natives-macos");

    private AngleLibraries() {
    }

    static Path libraryPath(String fileName) {
        if (Platform.isMacOS() && METAL_HOST_LIBRARY.equals(fileName)) {
            Path path = MACOS_NATIVE_DIR.resolve(fileName).toAbsolutePath();
            if (Files.exists(path))
                return path;
        }

        String resourcePath = Platform.getResourceDir() + fileName;
        try (InputStream in = FileUtils.getResource(resourcePath)) {
            Path extractedPath = Path.of(JHVGlobals.libCacheDir, fileName);
            Files.copy(in, extractedPath, StandardCopyOption.REPLACE_EXISTING);
            return extractedPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve native library " + fileName + " from " + resourcePath, e);
        }
    }

}
