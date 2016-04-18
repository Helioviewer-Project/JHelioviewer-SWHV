package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

public final class HTTPConstants {

    /** The maximum HTTP version supported */
    public static final double version = 1.1;

    /** The version in standard formated text */
    public static final String versionText = "HTTP/" + Double.toString(version);

    /** The array of bytes that contains the CRLF codes */
    public static final byte CRLFBytes[] = { 13, 10 };

    /** The string representation of the CRLF codes */
    public static final String CRLF = new String(CRLFBytes);

}
