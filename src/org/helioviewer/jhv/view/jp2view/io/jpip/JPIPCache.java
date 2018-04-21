package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.io.IOException;
import java.util.HashMap;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduConstants;

public class JPIPCache extends Kdu_cache {

    private final HashMap<Integer, JPIPSegmentStream> map = new HashMap<>();

    public boolean isDataBinCompleted(int klassID, long streamID, long binID) throws JHV_KduException {
        boolean complete[] = new boolean[1];
        try {
            Get_databin_length(klassID, streamID, binID, complete);
        } catch (KduException e) {
            throw new JHV_KduException("Internal Kakadu error: " + e.getMessage(), e);
        }
        return complete[0];
    }

    private void addToKdu(int frame, JPIPSegment data) throws IOException {
        try {
            Add_to_databin(data.klassID, frame, data.binID, data.data, data.offset, data.length, data.isFinal, true, false);
        } catch (KduException e) {
            throw new IOException("Internal Kakadu error: " + e.getMessage(), e);
        }
    }

    public void put(int frame, JPIPSegment data) throws IOException {
        if (data.klassID != KakaduConstants.KDU_META_DATABIN)
            map.computeIfAbsent(frame, k -> new JPIPSegmentStream()).segments.add(data);
        addToKdu(frame, data);
    }

    public void put(int frame, JPIPSegmentStream stream) throws IOException {
        for (JPIPSegment seg : stream.segments)
            addToKdu(frame, seg);
    }

    public JPIPSegmentStream remove(int frame) {
        return map.remove(frame);
    }

}
