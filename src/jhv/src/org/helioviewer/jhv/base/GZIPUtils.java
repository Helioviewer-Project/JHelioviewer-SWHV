package org.helioviewer.jhv.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPUtils {

    public static byte[] compress(final String str) throws IOException {
        if ((str == null) || (str.length() == 0)) {
            return null;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return obj.toByteArray();
    }

    private static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public static InputStream decompress(byte[] compressed) {
        byte[] buf = compressed;
        if (buf == null || buf.length == 0)
            buf = new byte[0];

        try {
            InputStream in = new ByteArrayInputStream(buf);
            if (buf.length != 0 && isCompressed(buf)) {
                in = new GZIPInputStream(in);
            }
            return in;
        } catch (IOException e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

}
