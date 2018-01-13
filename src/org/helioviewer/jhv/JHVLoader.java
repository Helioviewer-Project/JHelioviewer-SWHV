package org.helioviewer.jhv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import org.helioviewer.jhv.base.FileUtils;

class JHVLoader {

    public static void loadKDULibs() throws IOException {
        String pathlib = "";
        ArrayList<String> kduLibs = new ArrayList<>();

        if (System.getProperty("jhv.os").equals("mac") && System.getProperty("jhv.arch").equals("x86-64")) {
            pathlib = "macosx-universal/";
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-64")) {
            pathlib = "windows-amd64/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-64")) {
            pathlib = "linux-amd64/";
        }

        if (System.getProperty("jhv.os").equals("windows"))
            kduLibs.add(System.mapLibraryName("msvcr120"));
        kduLibs.add(System.mapLibraryName("kdu_v77R"));
        kduLibs.add(System.mapLibraryName("kdu_a77R"));
        kduLibs.add(System.mapLibraryName("kdu_jni"));

        for (String kduLib : kduLibs) {
            try (InputStream in = FileUtils.getResourceInputStream("/natives/" + pathlib + kduLib)) {
                File f = new File(JHVGlobals.LibsCacheDir, kduLib);
                Files.copy(in, f.toPath());
                System.load(f.getAbsolutePath());
            }
        }
    }

}
