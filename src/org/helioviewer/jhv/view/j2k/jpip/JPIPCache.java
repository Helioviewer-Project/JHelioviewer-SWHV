package org.helioviewer.jhv.view.j2k.jpip;

import javax.annotation.Nullable;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_global;

public class JPIPCache extends Kdu_cache {

    public boolean isDataBinCompleted(int klassID, long streamID, long binID) throws KduException {
        boolean[] complete = new boolean[1];
        Get_databin_length(klassID, streamID, binID, complete);
        return complete[0];
    }

    private void addToKdu(int frame, JPIPSegment data) throws KduException {
        Add_to_databin(data.klassID, frame, data.binID, data.data, data.offset, data.length, data.isFinal, true, false);
    }

    private JPIPStream scan(int frame) throws KduException {
        int flags = Kdu_global.KDU_CACHE_SCAN_START | Kdu_global.KDU_CACHE_SCAN_FIX_CODESTREAM;
        int[] klassID = new int[1];
        long[] codestreamID = {frame};
        long[] binID = new long[1];
        int[] binLen = new int[1];
        boolean[] complete = new boolean[1];

        JPIPStream stream = new JPIPStream();
        while (Scan_databins(flags, klassID, codestreamID, binID, binLen, complete, null, 0)) {
            if (klassID[0] == KakaduConstants.KDU_META_DATABIN)
                continue;

            flags &= ~Kdu_global.KDU_CACHE_SCAN_START;
            byte[] data = new byte[binLen[0]];
            if (!Scan_databins(flags | Kdu_global.KDU_CACHE_SCAN_NO_ADVANCE, klassID, codestreamID, binID, binLen, complete, data, binLen[0]))
                break;

            JPIPSegment seg = new JPIPSegment();
            seg.binID = binID[0];
            seg.klassID = klassID[0];
            seg.codestreamID = codestreamID[0];
            seg.length = binLen[0];
            seg.data = data;
            seg.isFinal = complete[0];
            stream.segments.add(seg);
        }
        return stream;
    }

    public void put(int frame, JPIPSegment seg) throws KduException {
        addToKdu(frame, seg);
    }

    public void put(int frame, JPIPStream stream) throws KduException {
        for (JPIPSegment seg : stream.segments)
            addToKdu(frame, seg);
    }

    @Nullable
    public JPIPStream get(int frame) {
        try {
            return scan(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
