package org.helioviewer.jhv;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.metadata.AIAResponse;
import org.helioviewer.jhv.view.j2k.io.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduMessageSystem;

import nom.tam.fits.FitsFactory;

class JHVInit {

    static void init() {
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

        ProxySettings.init();
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

        if (Platform.isMacOS() && "x86-64".equals(Platform.getArch())) {
            pathlib = "macosx-universal/";
        } else if (Platform.isWindows() && "x86-64".equals(Platform.getArch())) {
            pathlib = "windows-amd64/";
        } else if (Platform.isLinux() && "x86-64".equals(Platform.getArch())) {
            pathlib = "linux-amd64/";
        }

        if (Platform.isWindows()) {
            libs.add(System.mapLibraryName("msvcr120"));
            libs.add(System.mapLibraryName("msvcp120"));
            libs.add(System.mapLibraryName("kdu_v7AR"));
            libs.add(System.mapLibraryName("kdu_a7AR"));
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
                "de432s_reduced.bsp",
                "ahead_2017_061_5295day_predict.epm.bsp",
                "solo_ANC_soc-orbit-stp_20200210-20301120_204_V1_00167_V01.bsp",
                "naif0012.tls",
                "pck00010.tpc",
                "solo_ANC_soc-ops-fk_V02.tf",
                "solo_ANC_soc-sci-fk_V07.tf");

        kernels.parallelStream().forEach(x -> {
            try (InputStream in = FileUtils.getResource("/kernels/" + x)) {
                Files.copy(in, Path.of(JHVGlobals.dataCacheDir, x));
            } catch (Exception e) {
                Log.error("SPICE kernel copy error", e);
            }
        });
        Spice.loadKernels(kernels);
    }

}
