package org.helioviewer.jhv.view.j2k.jpip;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_global;

public class JPIPCache extends Kdu_cache {

    boolean isDataBinCompleted(int klassID, long streamID, long binID) throws KduException {
        boolean[] complete = new boolean[1];
        Get_databin_length(klassID, streamID, binID, complete);
        return complete[0];
    }

    JPIPStream scan(int frame) throws KduException {
        int flags = Kdu_global.KDU_CACHE_SCAN_START | Kdu_global.KDU_CACHE_SCAN_FIX_CODESTREAM;
        int[] klassID = new int[1];
        long[] codestreamID = {frame};
        long[] binID = new long[1];
        int[] binLen = new int[1];
        boolean[] complete = new boolean[1];

        JPIPStream stream = new JPIPStream();
        while (Scan_databins(flags, klassID, codestreamID, binID, binLen, complete, null, 0)) {
            flags &= ~Kdu_global.KDU_CACHE_SCAN_START;
            if (klassID[0] == Constants.KDU.META_DATABIN)
                continue;

            byte[] data = new byte[binLen[0]];
            if (!Scan_databins(flags | Kdu_global.KDU_CACHE_SCAN_NO_ADVANCE, klassID, codestreamID, binID, binLen, complete, data, binLen[0]))
                break;

            stream.databins.add(new JPIPStream.Databin(klassID[0], binID[0], complete[0], data));
        }
        return stream;
    }

    void put(int frame, JPIPSegment seg) throws KduException {
        Add_to_databin(seg.klassID, frame, seg.binID, seg.data, seg.offset, seg.length, seg.isFinal, true, false);
    }

    public void put(int frame, JPIPStream stream) throws KduException {
        for (JPIPStream.Databin databin : stream.databins)
            Add_to_databin(databin.klassID(), frame, databin.binID(), databin.data(), 0,
                    databin.data().length, databin.complete(), true, false);
    }

}
