package org.helioviewer.viewmodel.view.jp2view.io.jpip;

import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduConstants;

/**
 * Enum describing the databin class ID's. Methods exist for getting the
 * KakaduClassID and the StandardClassID. I have also included the string
 * representations of the databins as defined for cache model updates.
 * 
 * @author caplins
 * 
 */
public enum JPIPDatabinClass {

    /** Precinct data bin class. */
    PRECINCT_DATABIN(KakaduConstants.KDU_PRECINCT_DATABIN, JPIPConstants.PRECINCT_DATA_BIN_CLASS, "P"),

    /** Tile Header data bin class. */
    TILE_HEADER_DATABIN(KakaduConstants.KDU_TILE_HEADER_DATABIN, JPIPConstants.TILE_HEADER_DATA_BIN_CLASS, "H"),

    /** Tile data bin class. */
    TILE_DATABIN(KakaduConstants.KDU_TILE_DATABIN, JPIPConstants.TILE_DATA_BIN_CLASS, "T"),

    /** Main Header data bin class. */
    MAIN_HEADER_DATABIN(KakaduConstants.KDU_MAIN_HEADER_DATABIN, JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS, "Hm"),

    /** Meta data bin class. */
    META_DATABIN(KakaduConstants.KDU_META_DATABIN, JPIPConstants.META_DATA_BIN_CLASS, "M");

    /** The classID as an integer as per the Kakadu library. */
    private int kakaduClassID;

    /** The classID as an integer as per the JPEG2000 Part-9 standard. */
    private int standardClassID;

    /**
     * The classID as a string as per the JPEG2000 Part-9 standard. Used for
     * cache model updates.
     */
    private String jpipString;

    /**
     * Constructor.
     * 
     * @param _kakaduClassID
     * @param _standardClassID
     * @param _jpipString
     */
    JPIPDatabinClass(int _kakaduClassID, int _standardClassID, String _jpipString) {
        kakaduClassID = _kakaduClassID;
        standardClassID = _standardClassID;
        jpipString = _jpipString;
    }

    /** Returns the classID as an integer as per the Kakadu library. */
    public int getKakaduClassID() {
        return kakaduClassID;
    }

    /** Returns the classID as an integer as per the JPEG2000 Part-9 standard. */
    public int getStandardClassID() {
        return standardClassID;
    }

    /**
     * Returns the classID as a string as per the JPEG2000 Part-9 standard. Used
     * for cache model updates.
     */
    public String getJpipString() {
        return jpipString;
    }

};
