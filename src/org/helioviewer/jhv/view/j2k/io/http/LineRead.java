package org.helioviewer.jhv.view.j2k.io.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class LineRead {

    private static final int CR = 13;
    private static final int LF = 10;

    static String readAsciiLine(InputStream in) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(128)) {
            int ch;
            while ((ch = in.read()) >= 0 && ch != LF) {
                baos.write(ch);
            }
            return baos.toString(StandardCharsets.UTF_8).trim();
        }
    }

    static void readCRLF(InputStream in) throws IOException {
        int cr = in.read();
        int lf = in.read();
        if (cr != CR || lf != LF) {
            throw new IOException("CRLF expected: " + cr + '/' + lf);
        }
    }

}
