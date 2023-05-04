package org.helioviewer.jhv.view.j2k.jpip;

/**
 * The class <code>JPIPConstants</code> is used to define several JPIP global
 * constants.
 *
 * @author Juan Pablo Garcia Ortiz
 */
public class JPIPConstants {

    // Class identifier for precinct data-bins
    public static final int PRECINCT_DATA_BIN_CLASS = 0;

    // Class identifier for extended precinct data-bins
    public static final int EXTENDED_PRECINCT_DATA_BIN_CLASS = 1;

    // Class identifier for tile header data-bins
    public static final int TILE_HEADER_DATA_BIN_CLASS = 2;

    // Class identifier for tile data-bins
    public static final int TILE_DATA_BIN_CLASS = 4;

    // Class identifier for extended tile data-bins
    public static final int EXTENDED_TILE_DATA_BIN_CLASS = 5;

    // Class identifier for main header data-bins
    public static final int MAIN_HEADER_DATA_BIN_CLASS = 6;

    // Class identifier for meta-data data-bins
    public static final int META_DATA_BIN_CLASS = 8;

    // EOR code sent when the server has transferred all available image information
    // (not just information relevant to the requested view-window) to the client.
    public static final int EOR_IMAGE_DONE = 1;

    // EOR code sent when the server has transferred all available information
    // that is relevant to the requested view-window.
    public static final int EOR_WINDOW_DONE = 2;

    // EOR code sent when the server is terminating its response in order to
    // service a new request.
    public static final int EOR_WINDOW_CHANGE = 3;

    // EOR code sent when the server is terminating its response because the
    // byte limit specified in a byte limit specified in a max length request
    // field has been reached.
    public static final int EOR_BYTE_LIMIT_REACHED = 4;

    // EOR code sent when the server is terminating its response because the
    // quality limit specified in a quality request field has been reached.
    public static final int EOR_QUALITY_LIMIT_REACHED = 5;

    // EOR code sent when the server is terminating its response because some limit
    // on the session resources, e.g. a time limit, has been reached. No further
    // request should be issued using a channel ID assigned in that session.
    public static final int EOR_SESSION_LIMIT_REACHED = 6;

    // EOR code sent when the server is terminating its response because some
    // limit, e.g., a time limit, has been reached. If the request is issued in
    // a session, further requests can still be issued using a channel ID
    // assigned in that session.
    public static final int EOR_RESPONSE_LIMIT_REACHED = 7;

    // EOR code sent when there is not any specific EOR reason.
    public static final int EOR_NON_SPECIFIED = 0xFF;

    // Maximum number of layers that can be requested at the same time
    private static final int MAX_REQ_LAYERS = 1;

    // The maximum length in bytes of a JPIP request
    public static final int MAX_REQUEST_LEN = (MAX_REQ_LAYERS + 1) * (1024 * 1024);

    // The minimum length in bytes of a JPIP request
    public static final int MIN_REQUEST_LEN = 64000;

    public static final int META_REQUEST_LEN = 2000000;

}
