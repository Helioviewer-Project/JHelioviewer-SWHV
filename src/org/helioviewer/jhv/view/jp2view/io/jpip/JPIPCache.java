package org.helioviewer.jhv.view.jp2view.io.jpip;

import java.io.IOException;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.view.jp2view.kakadu.JHV_KduException;

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

    public void addJPIPSegment(JPIPSegment data) throws IOException {
        try {
            Add_to_databin(data.klassID, data.codestreamID, data.binID, data.data, data.offset, data.length, data.isFinal, true, false);
        } catch (KduException e) {
            throw new IOException("Internal Kakadu error: " + e.getMessage(), e);
        }
    }

}
