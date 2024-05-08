package org.helioviewer.jhv.view.j2k.jpip;

import java.util.Map;

class DatabinMap {

    // KakaduConstants
    private static final int KDU_PRECINCT_DATABIN = 0;
    private static final int KDU_TILE_HEADER_DATABIN = 1;
    private static final int KDU_TILE_DATABIN = 2;
    private static final int KDU_MAIN_HEADER_DATABIN = 3;
    static final int KDU_META_DATABIN = 4;
    private static final int KDU_UNDEFINED_DATABIN = 5;

    private static final Map<Integer, Integer> classMap = Map.of(
            JPIPConstants.PRECINCT_DATA_BIN_CLASS, KDU_PRECINCT_DATABIN,
            JPIPConstants.TILE_HEADER_DATA_BIN_CLASS, KDU_TILE_HEADER_DATABIN,
            JPIPConstants.TILE_DATA_BIN_CLASS, JPIPConstants.TILE_DATA_BIN_CLASS,
            JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS, KDU_MAIN_HEADER_DATABIN,
            JPIPConstants.META_DATA_BIN_CLASS, KDU_META_DATABIN);

    static Integer getKlass(int classID) {
        return classMap.get(classID);
    }

}
