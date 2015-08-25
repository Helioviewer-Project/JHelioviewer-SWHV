package org.helioviewer.viewmodel.view.jp2view.kakadu;

import java.util.concurrent.locks.ReentrantLock;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPResponse;

/**
 * Class extends the Kdu_cache class so as to implement the virtual acquire and
 * release lock function needed for a multithreaded enviroment. It prevents the
 * reader thread and the render thread from accessing the cache at the same
 * time.
 * 
 * @author caplins
 * @author Juan Pablo
 */
public class JHV_Kdu_cache extends Kdu_cache {

    /**
     * I chose to use a ReentrantLock as the mutex of choice.
     */
    private ReentrantLock cacheMutex = new ReentrantLock(true);

    private ImageCacheStatus status;

    /**
     * The targetID for the image as given by the JPIP server. Should be a
     * unique hash for the image and thus serves as a good way of naming the
     * cache file.
     */
    private final String targetID;

    /**
     * The amount of new data placed in this object via the addDataSegment
     * method starting after the initial readCacheFromFile method.
     */
    private volatile int newData;

    private static long maxCacheSize = 0;

    /**
     * Main constructor used when you want to use a cache file.
     * 
     * @param _targetID
     * @param _cachePath
     */
    public JHV_Kdu_cache(String _targetID) {
        super();
        targetID = _targetID;
        newData = 0;
    }

    /**
     * Sets the ImageCacheStatus
     * 
     */
    public void setImageCacheStatus(ImageCacheStatus imageCacheStatus) {
        status = imageCacheStatus;
    }

    /**
     * Returns the amount of new data.
     * 
     * @return Amount of new data.
     */
    public int getNewDataSize() {
        return newData;
    }

    /**
     * Returns the total amount of data in the cache object.
     * 
     * @return Total amount of data in the cache object.
     * @throws JHV_KduException
     */
    public int getTotalDataSize() throws JHV_KduException {
        int totalSize = 0;
        try {
            for (JPIPDatabinClass databinClass : JPIPDatabinClass.values())
                totalSize += Get_transferred_bytes(databinClass.getKakaduClassID());
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
        return totalSize;
    }

    /**
     * Returns whether or not the databin is complete.
     * 
     * @param _binClass
     * @param _streamID
     * @param _binID
     * @return True, if the databin is complete, false otherwise
     * @throws JHV_KduException
     */
    public boolean isDataBinCompleted(JPIPDatabinClass _binClass, int _streamID, int _binID) throws JHV_KduException {
        boolean complete[] = new boolean[1];
        try {
            Get_databin_length(_binClass.getKakaduClassID(), _streamID, _binID, complete);
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
        return complete[0];
    }

    /**
     * Overridden virtual method using a ReentrantLock to back it.
     */
    public void Acquire_lock() throws KduException {
        cacheMutex.lock();
    }

    /**
     * Overridden virtual method using a ReentrantLock to back it.
     */
    public void Release_lock() throws KduException {
        cacheMutex.unlock();
    }

    /**
     * Used to destroy the object at the end of its life cycle. If a path was
     * specified in the constructor then the cache attempts to save itself to a
     * file there before closure. Once this method is called this object should
     * not be used again.
     */
    public boolean Close() {
        try {
            super.Close();
        } catch (KduException ex) {
            ex.printStackTrace();
        }

        newData = 0;
        return true;
    }

    /**
     * Sets the read scope to the state that it needs to be in in order to pass
     * this object around for further use.
     * 
     * @throws JHV_KduException
     */
    public void setInitialScope() throws JHV_KduException {
        try {
            Set_read_scope(JPIPDatabinClass.MAIN_HEADER_DATABIN.getKakaduClassID(), 0, 0);
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
    }

    /**
     * Adds a JPIPResponse to the cache object using the addDataSegment methods.
     * 
     * @param jRes
     * @return True, the response is complete
     * @throws Exception
     */
    public boolean addJPIPResponseData(JPIPResponse jRes) throws JHV_KduException {
        JPIPDataSegment data;
        while ((data = jRes.removeJpipDataSegment()) != null && !data.isEOR)
            addDataSegment(data);
        return jRes.isResponseComplete();
    }

    /**
     * Adds a JPIPDataSegment to the cache object. Updates the newData variable.
     * 
     * @param _data
     * @throws JHV_KduException
     */
    public void addDataSegment(JPIPDataSegment _data) throws JHV_KduException {
        try {
            Add_to_databin(_data.classID.getKakaduClassID(), _data.codestreamID, _data.binID, _data.data, _data.offset, _data.length, _data.isFinal, true, false);

            newData += _data.length;

        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }

        if (status != null) {
            int compositionLayer = (int) _data.codestreamID;

            if (compositionLayer >= 0) {

                if (_data.classID.getKakaduClassID() == KakaduConstants.KDU_PRECINCT_DATABIN && status.getImageStatus(compositionLayer) == CacheStatus.HEADER)
                    status.setImageStatus(compositionLayer, CacheStatus.PARTIAL);

                else if (_data.classID.getKakaduClassID() == KakaduConstants.KDU_MAIN_HEADER_DATABIN && _data.isFinal)
                    status.setImageStatus(compositionLayer, CacheStatus.HEADER);
            }
        }
    }

}
