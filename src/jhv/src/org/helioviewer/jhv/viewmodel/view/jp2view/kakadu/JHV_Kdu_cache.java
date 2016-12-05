package org.helioviewer.jhv.viewmodel.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;

/**
 * @author caplins
 * @author Juan Pablo
 */
public class JHV_Kdu_cache extends Kdu_cache {

    private ImageCacheStatus status;

    public void setImageCacheStatus(ImageCacheStatus imageCacheStatus) {
        status = imageCacheStatus;
    }

    /**
     * Returns whether or not the databin is complete.
     * 
     * @param binClass
     * @param streamID
     * @param binID
     * @return True, if the databin is complete, false otherwise
     * @throws JHV_KduException
     */
    public boolean isDataBinCompleted(JPIPDatabinClass binClass, int streamID, int binID) throws JHV_KduException {
        boolean complete[] = new boolean[1];
        try {
            Get_databin_length(binClass.kakaduClassID, streamID, binID, complete);
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
        return complete[0];
    }

    /**
     * Adds a JPIPResponse to the cache object using the addDataSegment methods.
     * 
     * @param res
     * @throws Exception
     */
    public void addJPIPResponseData(JPIPResponse res) throws JHV_KduException {
        JPIPDataSegment data;
        while ((data = res.removeJpipDataSegment()) != null && !data.isEOR)
            addDataSegment(data);
    }

    /**
     * Adds a JPIPDataSegment to the cache object. Updates the newData variable.
     * 
     * @param data
     * @throws JHV_KduException
     */
    private void addDataSegment(JPIPDataSegment data) throws JHV_KduException {
        try {
            Add_to_databin(data.classID.kakaduClassID, data.codestreamID, data.binID, data.data, data.offset, data.length, data.isFinal, true, false);
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }

        int compositionLayer = (int) data.codestreamID;
        if (compositionLayer >= 0) {
            if (data.classID.kakaduClassID == KakaduConstants.KDU_PRECINCT_DATABIN && status.getImageStatus(compositionLayer) == CacheStatus.HEADER)
                status.setImageStatus(compositionLayer, CacheStatus.PARTIAL);
            else if (data.isFinal && data.classID.kakaduClassID == KakaduConstants.KDU_MAIN_HEADER_DATABIN)
                status.setImageStatus(compositionLayer, CacheStatus.HEADER);
        }
    }

}
