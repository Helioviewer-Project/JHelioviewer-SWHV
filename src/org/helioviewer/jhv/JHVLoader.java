package org.helioviewer.jhv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.helioviewer.jhv.base.FileUtils;

class JHVLoader {

    public static void copyKDULibs() throws IOException {
        String pathlib = "";
        ArrayList<String> kduLibs = new ArrayList<>();

        if (System.getProperty("jhv.os").equals("mac") && System.getProperty("jhv.arch").equals("x86-64")) {
            kduLibs.add("libkdu_v77R.so");
            kduLibs.add("libkdu_a77R.so");
            kduLibs.add("libkdu_jni.jnilib");
            pathlib = "macosx-universal/";
            /* obsolete computer
            } else if (System.getProperty("jhv.os").equals("mac") && System.getProperty("jhv.arch").equals("x86-32")) {
                kduLibs.add("libkdu_jni-mac-x86-32.jnilib");
                pathlib = "macosx-universal/"; */
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-64")) {
            kduLibs.add("msvcr120.dll");
            kduLibs.add("kdu_v77R.dll");
            kduLibs.add("kdu_a77R.dll");
            kduLibs.add("kdu_jni.dll");
            pathlib = "windows-amd64/";
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-32")) {
            kduLibs.add("msvcr120.dll");
            kduLibs.add("kdu_v77R.dll");
            kduLibs.add("kdu_a77R.dll");
            kduLibs.add("kdu_jni.dll");
            pathlib = "windows-i586/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-64")) {
            kduLibs.add("libkdu_v77R.so");
            kduLibs.add("libkdu_a77R.so");
            kduLibs.add("libkdu_jni.so");
            pathlib = "linux-amd64/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-32")) {
            kduLibs.add("libkdu_v77R.so");
            kduLibs.add("libkdu_a77R.so");
            kduLibs.add("libkdu_jni.so");
            pathlib = "linux-i586/";
        }

        for (String kduLib : kduLibs) {
            try (InputStream is = JHVLoader.class.getResourceAsStream("/natives/" + pathlib + kduLib)) {
                File f = new File(JHVDirectory.LIBS.getPath() + kduLib);
                FileUtils.save(is, f);
                System.load(f.getAbsolutePath());
            }
        }
    }

}
