package org.helioviewer.jhv.view.j2k.jpip;

import com.google.common.collect.ImmutableMap;

public class DatabinMap {

    private static final ImmutableMap<Integer, Integer> classMap = ImmutableMap.of(
            JPIPConstants.PRECINCT_DATA_BIN_CLASS, KakaduConstants.KDU_PRECINCT_DATABIN,
            JPIPConstants.TILE_HEADER_DATA_BIN_CLASS, KakaduConstants.KDU_TILE_HEADER_DATABIN,
            JPIPConstants.TILE_DATA_BIN_CLASS, JPIPConstants.TILE_DATA_BIN_CLASS,
            JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS, KakaduConstants.KDU_MAIN_HEADER_DATABIN,
            JPIPConstants.META_DATA_BIN_CLASS, KakaduConstants.KDU_META_DATABIN);

    public static Integer getKlass(int classID) {
        return classMap.get(classID);
    }

}
