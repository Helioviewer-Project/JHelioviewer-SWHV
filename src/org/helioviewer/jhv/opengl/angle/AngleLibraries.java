package org.helioviewer.jhv.opengl.angle;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.io.FileUtils;

final class AngleLibraries {
    private static final Path NATIVES_DIR = Path.of("lib", "natives-macos");

    private AngleLibraries() {
    }

    static Path libraryPath(String fileName) {
        Path libraryPath = NATIVES_DIR.resolve(fileName).toAbsolutePath();
        if (Files.exists(libraryPath))
            return libraryPath;

        try (InputStream in = FileUtils.getResource("/lib/natives-macos/" + fileName)) {
            Path extractedPath = Path.of(JHVGlobals.libCacheDir, fileName);
            Files.copy(in, extractedPath, StandardCopyOption.REPLACE_EXISTING);
            return extractedPath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve native library " + fileName, e);
        }
    }

    static void configureLwjglProperty(String lwjglProperty, String fileName) {
        if (System.getProperty(lwjglProperty) != null)
            return;
        System.setProperty(lwjglProperty, libraryPath(fileName).toString());
    }
}
