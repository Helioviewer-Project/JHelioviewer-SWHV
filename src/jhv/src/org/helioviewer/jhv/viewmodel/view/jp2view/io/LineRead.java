package org.helioviewer.jhv.viewmodel.view.jp2view.io

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class LineRead {

    private static final int CR = 13;
    private static final int LF = 10;

    private static byte[] readRawLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
        int ch;
        while ((ch = inputStream.read()) >= 0) {
            buf.write(ch);
            if (ch == LF) {
                break;
            }
        }
        return buf.toByteArray();
    }

    public static String readAsciiLine(InputStream inputStream) throws IOException {
        byte[] rawdata = readRawLine(inputStream);
        int len = rawdata.length;
        int offset = 0;
        if (len > 0 && rawdata[len - 1] == LF) {
            offset++;
            if (len > 1 && rawdata[len - 2] == CR) {
                offset++;
            }
        }
        return new String(rawdata, 0, len - offset, "US-ASCII");
    }

}
