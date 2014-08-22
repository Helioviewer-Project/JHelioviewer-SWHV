/*
 * This class provides conversions to ASCII strings without breaking
 * compatibility with Java 1.5.
 */
package nom.tam.util;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tmcglynn
 */
public class AsciiFuncs {

    public final static String ASCII = "US-ASCII";

    /** Convert to ASCII or return null if not compatible */
    public static String asciiString(byte[] buf) {
        return asciiString(buf, 0, buf.length);
    }

    /** Convert to ASCII or return null if not compatible */
    public static String asciiString(byte[] buf, int start, int len) {
        try {
            return new String(buf, start, len, ASCII);
        } catch (java.io.UnsupportedEncodingException e) {
            // Shouldn't happen
            System.err.println("AsciiFuncs.asciiString error finding ASCII encoding");
            return null;
        }
    }

    /** Convert an ASCII string to bytes */
    public static byte[] getBytes(String in) {
        try {
            return in.getBytes(ASCII);
        } catch (UnsupportedEncodingException ex) {
            System.err.println("Unable to find ASCII encoding");
            return null;
        }
    }
}
