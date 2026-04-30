package org.helioviewer.jhv.view.j2k.jpip;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

public class JPIPCache extends Kdu_cache {

    boolean isDataBinCompleted(int klassID, long streamID, long binID) throws KduException {
        boolean[] complete = new boolean[1];
        Get_databin_length(klassID, streamID, binID, complete);
        return complete[0];
    }

    void put(int frame, JPIPSegment seg) throws KduException {
        Add_to_databin(seg.klassID, frame, seg.binID, seg.data, seg.offset, seg.length, seg.isFinal, true, false);
    }

}
