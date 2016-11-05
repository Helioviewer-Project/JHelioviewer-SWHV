package org.helioviewer.jhv.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Scanner;

import org.helioviewer.jhv.base.logging.Log;

// A class which provides functions for accessing and working with files
public class FileUtils {

    private static final int BUFSIZ = 65536;
    private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

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
        Log.debug("Copy file " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());

        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[BUFSIZ];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        // Transfer bytes from in to out
        byte[] buf = new byte[BUFSIZ];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
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
        Log.debug("Saving stream to " + dst.getAbsolutePath());
        dst.getParentFile().mkdirs();

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

    public static String read(File dst) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(dst))) {
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str).append('\n');
            }
        }

        return sb.toString();
    }

    public static String read(InputStream strm) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(strm, "UTF-8"))) {
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str).append('\n');
            }
        }

        return sb.toString();
    }

    /**
     * Returns an input stream to a resource. This function can be used even if
     * the whole program and resources are within a JAR file.\n The path must
     * begin with a slash and contain all subfolders, e.g.:\n
     * /images/sample_image.png <br>
     * The class loader used is the same which was used to load FileUtils
     *
     * @param resourcePath
     *            The path to the resource
     * @return An InputStream to the resource
     */
    public static InputStream getResourceInputStream(String resourcePath) {
        return FileUtils.class.getResourceAsStream(resourcePath);
    }

    public static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is, "UTF-8");
        s.useDelimiter("\\A");
        String next = s.hasNext() ? s.next() : "";
        s.close();
        return next;
    }

    /**
     * Returns an URL to a resource. This function can be used even if the whole
     * program and resources are within a JAR file.\n The path must begin with a
     * slash and contain all subfolders, e.g.:\n /images/sample_image.png <br>
     * The class loader used is the same which was used to load FileUtils .
     *
     * @param resourcePath
     *            The path to the resource
     * @return An URL to the resource
     */
    public static URL getResourceUrl(String resourcePath) {
        return FileUtils.class.getResource(resourcePath);
    }

    /**
     * Converts a hex string into a byte array
     *
     * @param s
     *            The hex string
     * @return The byte array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Converts a byte array into a hex string
     *
     * @param b
     *            The byte array
     * @return The hex string
     */
    public static String byteArrayToHexString(byte b[]) {
        byte[] hex = new byte[2 * b.length];
        int index = 0;
        for (byte a : b) {
            int v = a & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }

        try {
            return new String(hex, "ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
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

}
