package org.helioviewer.base;

import java.io.BufferedInputStream;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.helioviewer.base.logging.Log;

/**
 * A class which provides functions for accessing and working with files.
 *
 * @author Benjamin Wamsler
 * @author Andre Dau
 */
public class FileUtils {
    private static Map<String, String> registeredExecutables = new HashMap<String, String>();

    public static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

    /**
     * Return the current working directory
     *
     * @return the current working directory
     */
    public static File getWorkingDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Invokes an executable whose path was registered before.
     *
     * @param identifier
     *            Identifier under which the executable is registered
     * @param arguments
     *            Arguments which should be passed to the executable
     * @throws IOException
     */
    public static Process invokeExecutable(String identifier, List<String> arguments) throws IOException {
        String exec = registeredExecutables.get(identifier);
        if (exec == null) {
            throw new IllegalArgumentException("Executable " + identifier + " not registered!");
        }
        if (arguments != null) {
            arguments.add(0, exec);
            String logExec = "";
            for (String argument : arguments) {
                logExec += " \"" + argument + "\"";
            }
            Log.debug(">> FileUtils.invokeExecutable > Execute command: " + logExec);
            return Runtime.getRuntime().exec(arguments.toArray(new String[arguments.size()]));
        } else {
            Log.debug(">> FileUtils.invokeExecutable > Execute command: " + "\"" + exec + "\"");
            return Runtime.getRuntime().exec(exec);
        }
    }

    /**
     * Logs stdout and stderr of a process. It is necessary to read from the
     * input and the error stream of a process object. Otherwise the process
     * might block when the buffer is full.
     *
     * @param process
     *            The process object
     * @param processName
     *            The name of the process (for logging pruposes)
     * @param logLevel
     *            The level with which to log the output
     * @param blockUntilFinished
     *            True, if the method should block until the process finished
     *            execution
     * @throws IOException
     */
    public static void logProcessOutput(final Process process, final String processName, final Level logLevel, boolean blockUntilFinished) throws IOException {
        final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        Thread threadStdout = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = stdout.readLine()) != null) {
                        Log.log(logLevel, ">> FileUtils.logProcessOutput(Process, " + processName + ", " + logLevel + ") > std out: " + line);
                    }
                } catch (IOException e) {
                    Log.error(">> FileUtils.logProcessOutput(Process, " + processName + ", " + logLevel + ") > Error while reading standard output", e);
                } finally {
                    try {
                        stdout.close();
                    } catch (IOException e) {
                        Log.error(">> FileUtils.logProcessOutput(Process, " + processName + ", " + logLevel + ") > Error while closing standard output stream", e);
                    }
                }
            }
        }, "FILEUTILS1");
        Thread threadStderr = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = stderr.readLine()) != null) {
                        Log.log(logLevel, ">> FileUtils.logProcessOutput(Process, " + processName + ", " + logLevel + ") > std err: " + line);
                    }
                } catch (IOException e) {
                    Log.error(">> FileUtils.logProcessOutput(Process, " + processName + ", " + logLevel + ") > Error while reading standard error", e);
                } finally {
                    try {
                        stderr.close();
                    } catch (IOException e) {
                        Log.error(">> FileUtils.logProcessOutput(Process, " + processName + ", " + logLevel + ") > Error while closing standard error stream", e);
                    }
                }
            }
        }, "FILEUTILS2");

        threadStderr.start();
        threadStdout.start();
        if (blockUntilFinished) {
            try {
                threadStderr.join();
                threadStdout.join();
            } catch (InterruptedException e) {
                Log.error(">> FileUtils.logProcessOutput(Process, " + processName + ", " + logLevel + ") > Interrupted while reading process output.", e);
            }
        }
    }

    /**
     * Registers the path of an executable. The executable can be later invoked
     * using its identifier
     *
     * @param identifier
     *            Identifier under which the executable can be accessed
     * @param path
     *            Path to the executable
     */
    public static void registerExecutable(String identifier, String path) {
        boolean registered = false;

        try {
            Log.debug(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Trying to use execFile.setExecutable from JDK 1.6+");
            File execFile = new File(path);
            registered = (Boolean) (execFile.getClass().getDeclaredMethod("setExecutable", new Class[] { boolean.class }).invoke(execFile, true));
            if (!registered) {
                Log.error("FileUtils.registerExecutable(" + identifier + ", " + path + ") > Failed to make file executable. The executable might not work properly!");
            }
        } catch (Throwable t) {
            Log.debug(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Failed using setExecutable method. Fall back to Java < 1.6 registerExecutable mode.", t);
            registered = false;
        }

        if (!registered) {
            if (!System.getProperty("jhv.os").equals("windows")) {
                String[] cmd = { "chmod", "u+x", path };
                try {
                    Log.debug(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Executing 'chmod u+x " + path + "'");
                    Process process = Runtime.getRuntime().exec(cmd);
                    logProcessOutput(process, "chmod", Level.DEBUG, true);
                    process.waitFor();
                    registered = (process.exitValue() == 0);
                    process.destroy();
                } catch (IOException e) {
                    Log.error(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Error while executing chmod on file. The executable may not work.", e);
                } catch (InterruptedException e) {
                    Log.error(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Interrupted while waiting for chmod to finish.", e);
                }
            } else {
                registered = true;
            }
        }

        if (!registered) {
            Log.fatal(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Error while registering executable '" + identifier + "' in '" + path + "'");
        } else {
            registeredExecutables.put(identifier, path);
        }
    }

    /**
     * Checks if an executable with the given identifier was registered.
     *
     * @param identifier
     *            Identifier of the executable
     * @return true, if executable is registered
     */
    public static boolean isExecutableRegistered(String identifier) {
        return registeredExecutables.containsKey(identifier);
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
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
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
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
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
    public static String read(File dst) throws IOException {
        Log.debug("Reading file " + dst.getAbsolutePath());
        BufferedReader in = new BufferedReader(new FileReader(dst));
        StringBuilder sb = new StringBuilder();

        // Transfer bytes from in to out
        String str;
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }

        in.close();
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
     * Calculates the MD5 hash of a file.
     *
     * @param fileUri
     *            The URI of the file from which the md5 hash should be
     *            calculated
     * @return The MD5 hash
     * @throws URISyntaxException
     * @throws IOException
     */
    public static byte[] calculateMd5(URI fileUri) throws URISyntaxException, IOException {
        InputStream fileStream = null;
        try {
            fileStream = new DownloadStream(fileUri, 0, 0).getInput();
            fileStream = new BufferedInputStream(fileStream);
            try {
                MessageDigest md5Algo = MessageDigest.getInstance("MD5");
                md5Algo.reset();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = fileStream.read(buffer)) != -1)
                    md5Algo.update(buffer, 0, length);
                return md5Algo.digest();
            } catch (NoSuchAlgorithmException e) {
                Log.error(">> FileUtils.calculateMd5(" + fileUri + ") > Could not md5 algorithm", e);
            }
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    Log.error(">> FileUtils.calculateMd5(" + fileUri + ") > Could not close stream.", e);
                }
            }
        }
        return null;
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

}
