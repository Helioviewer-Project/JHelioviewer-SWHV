package org.helioviewer.jhv.view.jp2view.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LineRead {

    private static final int CR = 13;
    private static final int LF = 10;

    private static byte[] readRawLine(InputStream in) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(64)) {
            int ch;
            while ((ch = in.read()) >= 0) {
                baos.write(ch);
                if (ch == LF)
                    break;
            }
            return baos.toByteArray();
        }
    }

    public static String readAsciiLine(InputStream in) throws IOException {
        byte[] rawdata = readRawLine(in);
        int len = rawdata.length;
        int offset = 0;
        if (len > 0 && rawdata[len - 1] == LF) {
            offset++;
            if (len > 1 && rawdata[len - 2] == CR) {
                offset++;
            }
        }
        return new String(rawdata, 0, len - offset, StandardCharsets.US_ASCII);
    }

    public static void readCRLF(InputStream in) throws IOException {
        int cr = in.read();
        int lf = in.read();
        if (cr != CR || lf != LF) {
            throw new IOException("CRLF expected: " + cr + "/" + lf);
        }
    }

}
