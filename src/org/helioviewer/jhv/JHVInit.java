package org.helioviewer.jhv;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.metadata.AIAResponse;
import org.helioviewer.jhv.view.j2k.io.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduMessageSystem;

import nom.tam.fits.FitsFactory;

class JHVInit {

    static void init() {
        FitsFactory.setUseHierarch(true);
        FitsFactory.setLongStringsEnabled(true);

        loadKDULibs();
        try {
            KakaduMessageSystem.startKduMessageSystem();
        } catch (Exception e) {
            Message.err("Failed to setup Kakadu", e.getMessage(), true);
            return;
        }
        loadKernels();

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
    }

    private static void loadKDULibs() {
        String pathlib = "";
        ArrayList<String> kduLibs = new ArrayList<>();

        if (System.getProperty("jhv.os").equals("mac") && System.getProperty("jhv.arch").equals("x86-64")) {
            pathlib = "macosx-universal/";
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-64")) {
            pathlib = "windows-amd64/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-64")) {
            pathlib = "linux-amd64/";
        }

        if (System.getProperty("jhv.os").equals("windows")) {
            kduLibs.add(System.mapLibraryName("msvcr120"));
            kduLibs.add(System.mapLibraryName("msvcp120"));
            kduLibs.add(System.mapLibraryName("kdu_v7AR"));
            kduLibs.add(System.mapLibraryName("kdu_a7AR"));
        }
        kduLibs.add(System.mapLibraryName("kdu_jni"));
        kduLibs.add(System.mapLibraryName("JNISpice"));

        String fullDir = "/natives/" + pathlib;
        kduLibs.parallelStream().forEach(k -> {
            try (InputStream in = FileUtils.getResource(fullDir + k)) {
                File f = new File(JHVGlobals.libCacheDir, k);
                Files.copy(in, f.toPath());
                System.load(f.getAbsolutePath());
            } catch (Exception e) {
                Log.error("Native library load error", e);
            }
        });
    }

    private static void loadKernels() {
        List<String> kernels = List.of("de432s.bsp", "naif0012.tls", "pck00010.tpc", "rssd0001.tf");

        kernels.parallelStream().forEach(k -> {
            try (InputStream in = FileUtils.getResource("/data/" + k)) {
                File f = new File(JHVGlobals.dataCacheDir, k);
                Files.copy(in, f.toPath());
                Spice.loadKernel(f.getAbsolutePath());
            } catch (Exception e) {
                Log.error("SPICE kernel load error", e);
            }
        });
    }

}
