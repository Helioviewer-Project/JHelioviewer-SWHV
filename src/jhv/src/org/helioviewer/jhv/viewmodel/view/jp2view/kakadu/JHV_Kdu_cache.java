package org.helioviewer.jhv.viewmodel.view.jp2view.kakadu;

import java.io.IOException;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JPIPCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDatabinClass;

/**
 * @author caplins
 * @author Juan Pablo
 */
public class JHV_Kdu_cache extends Kdu_cache implements JPIPCache {

    public boolean isDataBinCompleted(JPIPDatabinClass binClass, int streamID, int binID) throws JHV_KduException {
        boolean complete[] = new boolean[1];
        try {
            Get_databin_length(binClass.kakaduClassID, streamID, binID, complete);
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
        return complete[0];
    }

    @Override
    public void addJPIPDataSegment(JPIPDataSegment data, JP2ImageCacheStatus status) throws IOException {
        try {
            Add_to_databin(data.classID.kakaduClassID, data.codestreamID, data.binID, data.data, data.offset, data.length, data.isFinal, true, false);
        } catch (KduException e) {
            throw new IOException("Internal Kakadu error: " + e.getMessage());
        }

        int compositionLayer = (int) data.codestreamID;
        if (compositionLayer >= 0) {
            if (data.classID.kakaduClassID == KakaduConstants.KDU_PRECINCT_DATABIN && status.getImageStatus(compositionLayer) == CacheStatus.HEADER)
                status.setImageStatus(compositionLayer, -1, CacheStatus.PARTIAL);
            else if (data.isFinal && data.classID.kakaduClassID == KakaduConstants.KDU_MAIN_HEADER_DATABIN)
                status.setImageStatus(compositionLayer, -1, CacheStatus.HEADER);
        }
    }

}
