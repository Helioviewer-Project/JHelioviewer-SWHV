package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.view.jp2view.kakadu.KakaduConstants;

@SuppressWarnings("serial")
public class DatabinMap {

    private static final Map<Integer, Integer> classMap = Collections.unmodifiableMap(new HashMap<Integer, Integer>() {{
        put(JPIPConstants.PRECINCT_DATA_BIN_CLASS, KakaduConstants.KDU_PRECINCT_DATABIN);
        put(JPIPConstants.TILE_HEADER_DATA_BIN_CLASS, KakaduConstants.KDU_TILE_HEADER_DATABIN);
        put(JPIPConstants.TILE_DATA_BIN_CLASS, JPIPConstants.TILE_DATA_BIN_CLASS);
        put(JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS, KakaduConstants.KDU_MAIN_HEADER_DATABIN);
        put(JPIPConstants.META_DATA_BIN_CLASS, KakaduConstants.KDU_META_DATABIN);
    }});

    public static Integer getKlass(int classID) {
        return classMap.get(classID);
    }

}
