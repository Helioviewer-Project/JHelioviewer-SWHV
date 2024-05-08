package org.helioviewer.jhv.view.j2k.jpip;

/**
 * The class <code>JPIPConstants</code> is used to define several JPIP global
 * constants.
 *
 * @author Juan Pablo Garcia Ortiz
 */
class JPIPConstants {

    // Class identifier for precinct data-bins
    static final int PRECINCT_DATA_BIN_CLASS = 0;

    // Class identifier for extended precinct data-bins
    static final int EXTENDED_PRECINCT_DATA_BIN_CLASS = 1;

    // Class identifier for tile header data-bins
    static final int TILE_HEADER_DATA_BIN_CLASS = 2;

    // Class identifier for tile data-bins
    static final int TILE_DATA_BIN_CLASS = 4;

    // Class identifier for extended tile data-bins
    static final int EXTENDED_TILE_DATA_BIN_CLASS = 5;

    // Class identifier for main header data-bins
    static final int MAIN_HEADER_DATA_BIN_CLASS = 6;

    // Class identifier for meta-data data-bins
    static final int META_DATA_BIN_CLASS = 8;

    // EOR code sent when the server has transferred all available image information
    // (not just information relevant to the requested view-window) to the client.
    static final int EOR_IMAGE_DONE = 1;

    // EOR code sent when the server has transferred all available information
    // that is relevant to the requested view-window.
    static final int EOR_WINDOW_DONE = 2;

    // EOR code sent when the server is terminating its response in order to
    // service a new request.
    static final int EOR_WINDOW_CHANGE = 3;

    // EOR code sent when the server is terminating its response because the
    // byte limit specified in a byte limit specified in a max length request
    // field has been reached.
    static final int EOR_BYTE_LIMIT_REACHED = 4;

    // EOR code sent when the server is terminating its response because the
    // quality limit specified in a quality request field has been reached.
    static final int EOR_QUALITY_LIMIT_REACHED = 5;

    // EOR code sent when the server is terminating its response because some limit
    // on the session resources, e.g. a time limit, has been reached. No further
    // request should be issued using a channel ID assigned in that session.
    static final int EOR_SESSION_LIMIT_REACHED = 6;

    // EOR code sent when the server is terminating its response because some
    // limit, e.g., a time limit, has been reached. If the request is issued in
    // a session, further requests can still be issued using a channel ID
    // assigned in that session.
    static final int EOR_RESPONSE_LIMIT_REACHED = 7;

    // EOR code sent when there is not any specific EOR reason.
    static final int EOR_NON_SPECIFIED = 0xFF;

}
