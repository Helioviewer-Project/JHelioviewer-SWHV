package org.helioviewer.jhv.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GZIPUtils {

    private static boolean isCompressed(byte[] compressed) {
        return compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC) && compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8);
    }

    public static InputStream decompress(byte[] compressed) {
        byte[] buf = compressed;
        if (buf == null || buf.length == 0)
            return new ByteArrayInputStream(new byte[0]);

        try {
            InputStream in = new ByteArrayInputStream(buf);
            if (isCompressed(buf)) {
                in = new GZIPInputStream(in);
            }
            return in;
        } catch (IOException e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

}
