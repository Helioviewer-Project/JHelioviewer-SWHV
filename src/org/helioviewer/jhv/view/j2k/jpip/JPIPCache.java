package org.helioviewer.jhv.view.j2k.jpip;

import java.util.ArrayList;
import java.util.List;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_global;

public class JPIPCache extends Kdu_cache {

    boolean isDataBinCompleted(int klassID, long streamID, long binID) throws KduException {
        boolean[] complete = new boolean[1];
        Get_databin_length(klassID, streamID, binID, complete);
        return complete[0];
    }

    void put(int frame, JPIPSegment seg) throws KduException {
        Add_to_databin(seg.klassID, frame, seg.binID, seg.data, seg.offset, seg.length, seg.isFinal, true, false);
    }

    List<JPIPSegment> scan(int frame) throws KduException {
        int flags = Kdu_global.KDU_CACHE_SCAN_START | Kdu_global.KDU_CACHE_SCAN_FIX_CODESTREAM;
        int[] klassID = new int[1];
        long[] codestreamID = {frame};
        long[] binID = new long[1];
        int[] binLen = new int[1];
        boolean[] complete = new boolean[1];

        List<JPIPSegment> segments = new ArrayList<>();
        while (Scan_databins(flags, klassID, codestreamID, binID, binLen, complete)) {
            flags &= ~Kdu_global.KDU_CACHE_SCAN_START;
            if (klassID[0] == Constants.KDU.META_DATABIN)
                continue;

            byte[] data = new byte[binLen[0]];
            if (!Scan_databins(flags | Kdu_global.KDU_CACHE_SCAN_NO_ADVANCE, klassID, codestreamID, binID, binLen, complete, data, data.length))
                break;

            JPIPSegment seg = new JPIPSegment();
            seg.binID = binID[0];
            seg.klassID = klassID[0];
            seg.length = binLen[0];
            seg.data = data;
            seg.isFinal = complete[0];
            segments.add(seg);
        }
        return segments;
    }

}
