package org.helioviewer.jhv.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.samp.SampClient;
import org.helioviewer.jhv.metadata.AIAResponse;
import org.helioviewer.jhv.metadata.DetectorMask;
import org.helioviewer.jhv.view.j2k.KakaduMessageSystem;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;

import nom.tam.fits.FitsFactory;

public final class JHVInit {

    public static void init(boolean webProfilePopup) throws Exception {
        SampClient.init(webProfilePopup);
        ExitHooks.attach();

        loadLibs(Platform.getResourceDir());
        KakaduMessageSystem.startKduMessageSystem();

        try {
            JPIPCacheManager.init();
        } catch (Exception e) {
            Log.error("JPIP cache initialization error", e);
        }
        try {
            AIAResponse.load();
        } catch (Exception e) {
            Log.error("AIA response map load error", e);
        }

        FitsFactory.setUseHierarch(true);
        FitsFactory.setLongStringsEnabled(true);
        DetectorMask.loadBuiltins();
    }

    @SuppressWarnings("restricted")
    private static void loadLib(String name, String resourceDir) throws Exception {
        String libraryName = System.mapLibraryName(name);
        try (InputStream in = FileUtils.getResource(resourceDir + libraryName)) {
            Path libraryPath = Path.of(JHVDirectory.libCacheDir, libraryName);
            Files.copy(in, libraryPath, StandardCopyOption.REPLACE_EXISTING);
            System.load(libraryPath.toString());
        }
    }

    private static void loadLibs(String resourceDir) throws Exception {
        if (Platform.isWindows()) {
            loadLib("kdu_v7AR", resourceDir);
        }
        loadLib("kdu_jni", resourceDir);

        Path ffmpegPath = Path.of(JHVDirectory.libCacheDir, "ffmpeg");
        try (InputStream in = FileUtils.getResource(resourceDir + "ffmpeg")) {
            Files.copy(in, ffmpegPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.getFileAttributeView(ffmpegPath, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(ffmpegPath,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        }
    }

    public static void loadSpice() throws Exception {
        loadLib("JNISpice", Platform.getResourceDir());

        List<String> kernels = List.of(
                "naif0012.tls",
                "pck00011.tpc",
                "de432s_reduced.bsp",
                "ahead_2017_061_5295day_predict.epm.bsp",
                /* SOLO */
                "solo_ANC_soc-sc-fk_V09.tf",
                "solo_ANC_soc-sci-fk_V08.tf",
                "solo_ANC_soc-sclk-fict_20000101_V01.tsc",
                "solo_ANC_soc-orbit-stp_20200210-20301120_408_V1_00536_V01.bsp",
                "solo_ANC_soc-default-att-stp_20200210-20301120_408_V1_00536_V01.bc");

        List<String> builtinKernels = kernels.parallelStream().map(k -> { // order does not matter
            try (InputStream in = FileUtils.getResource("/kernels/" + k)) {
                Path kp = Path.of(JHVDirectory.dataCacheDir, k);
                Files.copy(in, kp, StandardCopyOption.REPLACE_EXISTING);
                return kp.toString();
            } catch (Exception e) {
                Log.error("SPICE kernel copy error", e);
                return null;
            }
        }).filter(Objects::nonNull).toList();
        Spice.loadKernels(builtinKernels);

        Path userKernelsPath = Path.of(JHVDirectory.KERNELS.getPath());
        List<String> userKernels = new ArrayList<>();
        Files.walkFileTree(userKernelsPath, Set.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && Files.isReadable(filePath)) {
                    userKernels.add(filePath.toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path filePath, IOException exc) {
                Log.warn("Skipping inaccessible kernel path: " + filePath, exc);
                return FileVisitResult.CONTINUE;
            }
        });
        userKernels.sort(String::compareTo);
        Spice.loadKernels(userKernels);
    }

    private JHVInit() {}
}
