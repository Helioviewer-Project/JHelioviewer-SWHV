package org.helioviewer.jhv;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.LocationChecker;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.io.SampClient;
import org.helioviewer.jhv.metadata.AIAResponse;
import org.helioviewer.jhv.view.j2k.KakaduMessageSystem;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;

import nom.tam.fits.FitsFactory;

class JHVInit {

    static void init() {
        ProxySettings.init();
        LocationChecker.setProximityServer();
        SampClient.init();
        ExitHooks.attach();

        try {
            loadLibs();
            KakaduMessageSystem.startKduMessageSystem();
            loadKernels();
        } catch (Exception e) {
            Log.error("Failed to setup native libraries", e);
            Message.fatalErr("Failed to setup native libraries:\n" + e.getMessage());
            return;
        }
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
    }

    private static void loadLibs() throws Exception {
        String pathlib = "";
        ArrayList<String> libs = new ArrayList<>();

        if (Platform.isMacOS()) {
            if ("amd64".equals(Platform.getArch()))
                pathlib = "macos-amd64/";
            else if ("aarch64".equals(Platform.getArch()))
                pathlib = "macos-arm64/";
        } else if (Platform.isWindows() && "amd64".equals(Platform.getArch())) {
            pathlib = "windows-amd64/";
        } else if (Platform.isLinux() && "amd64".equals(Platform.getArch())) {
            pathlib = "linux-amd64/";
        }

        if (Platform.isWindows()) {
            libs.add(System.mapLibraryName("kdu_v7AR"));
        }
        libs.add(System.mapLibraryName("kdu_jni"));
        libs.add(System.mapLibraryName("JNISpice"));

        List<String> xtract = new ArrayList<>(libs);
        xtract.add("ffmpeg");

        String fullDir = "/jhv/" + pathlib;
        xtract.parallelStream().forEach(x -> {
            try (InputStream in = FileUtils.getResource(fullDir + x)) {
                Files.copy(in, Path.of(JHVGlobals.libCacheDir, x));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        if (!Platform.isWindows())
            Files.setPosixFilePermissions(Path.of(JHVGlobals.libCacheDir, "ffmpeg"), Set.of(PosixFilePermission.OWNER_EXECUTE));
        for (String l : libs) {
            System.load(Path.of(JHVGlobals.libCacheDir, l).toString());
        }
    }

    private static void loadKernels() throws Exception {
        List<String> kernels = List.of(
                "naif0012.tls",
                "pck00011.tpc",
                "de432s_reduced.bsp",
                "ahead_2017_061_5295day_predict.epm.bsp",
                /* SOLO */
                "solo_ANC_soc-sc-fk_V09.tf",
                "solo_ANC_soc-sci-fk_V08.tf",
                "solo_ANC_soc-sclk_20251213_V01.tsc",
                "solo_ANC_soc-orbit-stp_20200210-20301120_394_V1_00504_V01.bsp",
                "solo_ANC_soc-default-att-stp_20200210-20301120_394_V1_00504_V01.bc");

        ArrayList<String> builtinKernels = new ArrayList<>(kernels.size());
        kernels.parallelStream().forEach(k -> { // order does not matter
            try (InputStream in = FileUtils.getResource("/kernels/" + k)) {
                Path kp = Path.of(JHVGlobals.dataCacheDir, k);
                Files.copy(in, kp);
                builtinKernels.add(kp.toString());
            } catch (Exception e) {
                Log.error("SPICE kernel copy error", e);
            }
        });
        Spice.loadKernels(builtinKernels);

        Path userKernelsPath = Path.of(JHVDirectory.KERNELS.getPath());
        try (Stream<Path> stream = Files.find(userKernelsPath, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            List<String> userKernels = stream.map(Path::toString).sorted().toList();
            Spice.loadKernels(userKernels);
        }
    }

}
