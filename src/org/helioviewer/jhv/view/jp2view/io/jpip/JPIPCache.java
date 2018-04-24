package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.io.IOException;

import javax.annotation.Nullable;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_global;

import org.helioviewer.jhv.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduConstants;

public class JPIPCache extends Kdu_cache {

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

    private JPIPStream scan(int frame) throws KduException {
        int flags = Kdu_global.KDU_CACHE_SCAN_START | Kdu_global.KDU_CACHE_SCAN_FIX_CODESTREAM;
        int klassID[] = new int[1];
        long codestreamID[] = { frame };
        long binID[] = new long[1];
        int binLen[] = new int[1];
        boolean complete[] = new boolean[1];

        JPIPStream stream = new JPIPStream();
        while (Scan_databins(flags, klassID, codestreamID, binID, binLen, complete, null, 0)) {
            flags &= ~Kdu_global.KDU_CACHE_SCAN_START;
            byte data[] = new byte[binLen[0]];
            if (!Scan_databins(flags | Kdu_global.KDU_CACHE_SCAN_NO_ADVANCE, klassID, codestreamID, binID, binLen, complete, data, binLen[0]))
                break;
            if (klassID[0] == KakaduConstants.KDU_META_DATABIN)
                continue;

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

    public void put(int frame, JPIPSegment data) throws IOException {
        addToKdu(frame, data);
    }

    public void put(int frame, JPIPStream stream) throws IOException {
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
