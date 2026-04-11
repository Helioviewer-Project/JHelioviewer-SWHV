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
    private static final Path METAL_HOST_NATIVE_DIR = Path.of("lib", "natives-macos");

    private AngleLibraries() {
    }

    static String eglLibraryName() {
        return platformLibraryName("libEGL");
    }

    static String openGlesLibraryName() {
        return platformLibraryName("libGLESv2");
    }

    static void extractRuntimeLibraries() {
        if (!Platform.isWindows())
            return;
        libraryPath("d3dcompiler_47.dll");
    }

    static Path libraryPath(String fileName) {
        Path path = METAL_HOST_NATIVE_DIR.resolve(fileName).toAbsolutePath();
        if (Files.exists(path))
            return path;

        String resourcePath = Platform.getResourceDir() + fileName;
        try (InputStream in = FileUtils.getResource(resourcePath)) {
            if (in == null)
                throw new IOException("Missing resource " + resourcePath);
            Path extractedPath = Path.of(JHVGlobals.libCacheDir, fileName);
            Files.copy(in, extractedPath, StandardCopyOption.REPLACE_EXISTING);
            return extractedPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve native library " + fileName, e);
        }
    }

    private static String platformLibraryName(String baseName) {
        if (Platform.isMacOS())
            return baseName + ".dylib";
        if (Platform.isWindows())
            return baseName + ".dll";
        if (Platform.isLinux())
            return baseName + ".so";
        throw new IllegalStateException("Unsupported OS: " + System.getProperty("os.name"));
    }

}
