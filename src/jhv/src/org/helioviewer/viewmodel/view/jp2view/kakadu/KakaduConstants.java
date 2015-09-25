package org.helioviewer.viewmodel.view.jp2view.kakadu;

public final class KakaduConstants {

    /** Kakadu constant */
    public static final int KDU_PRECINCT_DATABIN = 0;
    /** Kakadu constant */
    public static final int KDU_TILE_HEADER_DATABIN = 1;
    /** Kakadu constant */
    public static final int KDU_TILE_DATABIN = 2;
    /** Kakadu constant */
    public static final int KDU_MAIN_HEADER_DATABIN = 3;
    /** Kakadu constant */
    public static final int KDU_META_DATABIN = 4;
    /** Kakadu constant */
    public static final int KDU_UNDEFINED_DATABIN = 5;
    /** Kakadu constant */
    public static final int KDU_WANT_OUTPUT_COMPONENTS = 0;
    /** Kakadu constant */
    public static final int KDU_WANT_CODESTREAM_COMPONENTS = 1;

    /** Maximum of samples to process per rendering iteration */
    public static final int MAX_RENDER_SAMPLES = 1024 * 1024;

    /** The amount of cache to allocate to each codestream */
    public static final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

}
