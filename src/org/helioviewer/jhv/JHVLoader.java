package org.helioviewer.jhv;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
        } else if (System.getProperty("jhv.os").equals("windows") && System.getProperty("jhv.arch").equals("x86-32")) {
            pathlib = "windows-i586/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-64")) {
            pathlib = "linux-amd64/";
        } else if (System.getProperty("jhv.os").equals("linux") && System.getProperty("jhv.arch").equals("x86-32")) {
            pathlib = "linux-i586/";
        }

        if (System.getProperty("jhv.os").equals("windows"))
            kduLibs.add(System.mapLibraryName("msvcr120"));
        kduLibs.add(System.mapLibraryName("kdu_v77R"));
        kduLibs.add(System.mapLibraryName("kdu_a77R"));
        kduLibs.add(System.mapLibraryName("kdu_jni"));

        final String prefix = "kdulibs";
        final String suffix = ".lock";
        // delete all kdulibs directories without a lock file
        SimpleFileVisitor<Path> nuke = new SimpleFileVisitor<Path>() {

                private FileVisitResult delete(Path file) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    return delete(file);
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return delete(file);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return delete(dir);
                }

        };
        FileFilter filter = p -> p.getName().startsWith(prefix) && !p.getName().endsWith(suffix);
        File[] dirs = JHVDirectory.LIBS.getFile().listFiles(filter);
        for (File dir : dirs) {
            if (new File(dir + suffix).exists())
                continue;
            Files.walkFileTree(dir.toPath(), nuke);
        }

        String tempDir = Files.createTempDirectory(JHVDirectory.LIBS.getFile().toPath(), prefix).toString();
        File lock = new File(tempDir + suffix);
        lock.createNewFile();
        lock.deleteOnExit();

        for (String kduLib : kduLibs) {
            try (InputStream is = JHVLoader.class.getResourceAsStream("/natives/" + pathlib + kduLib)) {
                File f = new File(tempDir, kduLib);
                FileUtils.save(is, f);
                System.load(f.getAbsolutePath());
            }
        }
    }

}
