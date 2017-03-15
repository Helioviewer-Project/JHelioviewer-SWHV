package org.helioviewer.jhv.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

// A class which provides functions for accessing and working with files
public class FileUtils {

    private static final int BUFSIZ = 65536;

    /**
     * Return the current working directory
     *
     * @return the current working directory
     */
    public static File getWorkingDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Method copies a file from src to dst.
     *
     * @param src
     *            Source file
     * @param dst
     *            Destination file
     * @throws IOException
     */
    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[BUFSIZ];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    /**
     * Method saving a stream to dst.
     *
     * @param in
     *            Input stream, will be closed if finished
     * @param dst
     *            Destination file
     * @throws IOException
     */
    public static void save(InputStream in, File dst) throws IOException {
        // Transfer bytes from in to out
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[BUFSIZ];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        in.close();
    }

    /**
     * Returns an input stream to a resource. This function can be used even if
     * the whole program and resources are within a JAR file. The path must
     * begin with a slash and contain all subfolders, e.g.: /images/sample_image.png
     * The class loader used is the same which was used to load FileUtils.
     *
     * @param resourcePath
     *            The path to the resource
     * @return An InputStream to the resource
     */
    public static InputStream getResourceInputStream(String resourcePath) {
        return FileUtils.class.getResourceAsStream(resourcePath);
    }

    public static String convertStreamToString(InputStream is) {
        try (Scanner s = new Scanner(is, StandardCharsets.UTF_8.name())) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    /**
     * Returns an URL to a resource. This function can be used even if the whole
     * program and resources are within a JAR file. The path must begin with a
     * slash and contain all subfolders, e.g.: /images/sample_image.png
     * The class loader used is the same which was used to load FileUtils.
     *
     * @param resourcePath
     *            The path to the resource
     * @return An URL to the resource
     */
    public static URL getResourceUrl(String resourcePath) {
        return FileUtils.class.getResource(resourcePath);
    }

    public static boolean deleteDir(File dir) {
        String[] children;
        if (dir.isDirectory() && (children = dir.list()) != null) {
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static String URL2String(URL url) {
        StringBuilder sb = new StringBuilder();
        try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)))) {
            while (scanner.hasNext()) {
                sb.append(scanner.nextLine()).append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}
