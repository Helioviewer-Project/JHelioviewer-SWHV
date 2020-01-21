package org.helioviewer.jhv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.metadata.AIAResponse;
import org.helioviewer.jhv.view.j2k.io.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.kakadu.KakaduMessageSystem;

import nom.tam.fits.FitsFactory;

import spice.basic.KernelDatabase;
import spice.basic.SpiceErrorException;

class JHVInit {

    static void init() {
        FitsFactory.setUseHierarch(true);
        FitsFactory.setLongStringsEnabled(true);

        try {
            loadKDULibs();
            KakaduMessageSystem.startKduMessageSystem();
        } catch (Exception e) {
            Message.err("Failed to setup Kakadu", e.getMessage(), true);
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

        try {
            loadKernels();
        } catch (Exception e) {
            Log.error("SPICE kernels load error", e);
        }
    }

    private static void loadKDULibs() throws IOException {
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

        for (String kduLib : kduLibs) {
            try (InputStream in = FileUtils.getResource("/natives/" + pathlib + kduLib)) {
                File f = new File(JHVGlobals.libCacheDir, kduLib);
                Files.copy(in, f.toPath());
                System.load(f.getAbsolutePath());
            }
        }
    }

    private static void loadKernels() throws IOException, SpiceErrorException {
        List<String> kernels = List.of("naif0012.tls");

        for (String k : kernels) {
            try (InputStream in = FileUtils.getResource("/kernels/" + k)) {
                File f = new File(JHVGlobals.kernelCacheDir, k);
                Files.copy(in, f.toPath());
                KernelDatabase.load(f.getAbsolutePath());
            }
        }

    }

}
