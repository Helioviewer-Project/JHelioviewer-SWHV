package org.helioviewer.jhv.view.jp2view.kakadu;

import java.io.IOException;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPCache;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPDatabinClass;

public class JHV_Kdu_cache extends Kdu_cache implements JPIPCache {

    public boolean isDataBinCompleted(JPIPDatabinClass binClass, int streamID, int binID) throws JHV_KduException {
        boolean complete[] = new boolean[1];
        try {
            Get_databin_length(binClass.kakaduClassID, streamID, binID, complete);
        } catch (KduException e) {
            throw new JHV_KduException("Internal Kakadu error: " + e.getMessage(), e);
        }
        return complete[0];
    }

    @Override
    public void addJPIPDataSegment(JPIPDataSegment data) throws IOException {
        try {
            Add_to_databin(data.klassID, data.codestreamID, data.binID, data.data, data.offset, data.length, data.isFinal, true, false);
        } catch (KduException e) {
            throw new IOException("Internal Kakadu error: " + e.getMessage(), e);
        }
    }

}
