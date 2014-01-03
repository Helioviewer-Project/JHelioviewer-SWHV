package org.helioviewer.viewmodel.view.jp2view.kakadu;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus.CacheStatus;
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

    /**
     * The cache file to use. Null if its not using a chace file.
     */
    private final File cacheFile;

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

    /**
     * This flags indicates if the server has to be loaded/saved to disk.
     */
    private boolean iamPersistent = true;

    private static long maxCacheSize = 0;

    /**
     * Main constructor used when you want to use a cache file.
     * 
     * @param _targetID
     * @param _cachePath
     */
    public JHV_Kdu_cache(String _targetID, File _cachePath) {
        super();
        targetID = _targetID;

        if (_cachePath != null)
            cacheFile = new File(_cachePath.getAbsolutePath() + File.separator + targetID + ".hvc");
        else
            cacheFile = null;
        newData = 0;

        if (cacheFile != null)
            readCacheFromFile();
    }

    /**
     * Main constructor used when you want to use a cache file.
     * 
     * @param _targetID
     * @param _cachePath
     */
    public JHV_Kdu_cache(String _targetID, File _cachePath, boolean _iamPersistent) {
        super();
        targetID = _targetID;
        iamPersistent = _iamPersistent;

        if (_cachePath != null)
            cacheFile = new File(_cachePath.getAbsolutePath() + File.separator + targetID + ".hvc");
        else
            cacheFile = null;
        newData = 0;

        if ((cacheFile != null) && iamPersistent)
            readCacheFromFile();
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
        if ((cacheFile != null) && iamPersistent)
            writeCacheToFile();

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

    /**
     * Attempts to write the present cache object to a file as specified by the
     * cacheFile variable. The file format is firmly based on the one used in
     * the kdu_show application.
     * 
     * @return true, if the file could be written successfully, false otherwise
     */
    private boolean writeCacheToFile() {
        File fTarget = cacheFile;
        FileOutputStream fStream = null;
        try {
            if (fTarget.exists() && !fTarget.delete())
                return false;
            if (!fTarget.createNewFile())
                return false;
            fStream = new FileOutputStream(fTarget);
            fStream.write("hvc/1.0\n".getBytes());
        } catch (Exception ex) {
            Log.error("Error in writeToCacheMethod in file (" + fTarget + ") and FileOutputStream creation.");
            ex.printStackTrace();
            return false;
        }

        int bufferIndex, i;
        byte[] headerBuffer = new byte[24];
        byte[] byteBuffer = new byte[512];

        long codestreamID, databinID;
        boolean isComplete[] = new boolean[1];
        int databinClassID, length, idBits, codestreamBits;

        try {
            codestreamID = this.Get_next_codestream(-1);
            while (codestreamID >= 0) {
                for (databinClassID = 0; databinClassID < 5; databinClassID++) {
                    databinID = this.Get_next_lru_databin(databinClassID, codestreamID, -1, false);
                    while (databinID >= 0) {
                        length = this.Get_databin_length(databinClassID, codestreamID, databinID, isComplete);
                        if ((length > 0) || isComplete[0]) {
                            if (length > byteBuffer.length) {
                                byteBuffer = new byte[byteBuffer.length + length + 256];
                            }
                            length = this.Get_databin_prefix(databinClassID, codestreamID, databinID, byteBuffer, length);
                            bufferIndex = 0;
                            if (isComplete[0])
                                headerBuffer[bufferIndex++] = (byte) (2 * databinClassID + 1);
                            else
                                headerBuffer[bufferIndex++] = (byte) (2 * databinClassID);
                            for (codestreamBits = 0; (codestreamID >> codestreamBits) > 0; codestreamBits += 8)
                                ;
                            for (idBits = 0; (databinID >> idBits) > 0; idBits += 8)
                                ;
                            headerBuffer[bufferIndex++] = (byte) ((codestreamBits << 1) | (idBits >> 3));
                            for (i = codestreamBits - 8; i >= 0; i -= 8)
                                headerBuffer[bufferIndex++] = (byte) (codestreamID >> i);
                            for (i = idBits - 8; i >= 0; i -= 8)
                                headerBuffer[bufferIndex++] = (byte) (databinID >> i);
                            for (i = 24; i >= 0; i -= 8)
                                headerBuffer[bufferIndex++] = (byte) (length >> i);
                            fStream.write(headerBuffer, 0, bufferIndex);
                            fStream.write(byteBuffer, 0, length);
                        }
                        databinID = this.Get_next_lru_databin(databinClassID, codestreamID, databinID, false);
                    }
                }
                codestreamID = this.Get_next_codestream(codestreamID);
            }
        } catch (Exception ex) {
            Log.error("Error in writeToCacheMethod in actual writing algorithm.");
            ex.printStackTrace();
            return false;
        } finally {
            byteBuffer = null;
            headerBuffer = null;
        }

        try {
            fStream.flush();
            fStream.close();
        } catch (Exception ex) {
            Log.error("Error in writeToCacheMethod in file and FileOutputStream closure.");
            ex.printStackTrace();
            return false;
        } finally {
            fStream = null;
        }

        return true;
    }

    /**
     * Attempts to read data from a file as specified by the cacheFile variable.
     * All data inserted into the cache object from a file is marked. This mark
     * signifies that the servers cache model has not yet been informed about
     * the data.
     * 
     * @return true, if the cache file could be read successfully, false
     *         otherwise
     */
    private boolean readCacheFromFile() {
        File fTarget = cacheFile;
        // FileInputStream fStream = null;
        BufferedInputStream fStream = null;

        try {
            if (!fTarget.exists())
                return false;

            // fStream = new FileInputStream(fTarget);
            fStream = new BufferedInputStream(new FileInputStream(fTarget), 1000000);

            char c;
            String str = "";
            while ((c = (char) fStream.read()) != '\n') {
                str += c;
            }
            if (!str.equalsIgnoreCase("hvc/1.0")){
                fStream.close();
                throw new JHV_KduException("Wrong cache file format.");
            }
        } catch (Exception ex) {
            Log.error("Error in readFromCacheMethod in file and FileInputStream creation.");
            ex.printStackTrace();
            return false;
        }

        int bufferIndex, i;
        byte[] byteBuffer = new byte[512];
        int[] intBuffer;

        long databinID, codestreamID;
        int idBytes, codestreamBytes, length, databinClassID;

        try {

            while (fStream.read(byteBuffer, 0, 2) == 2) {

                intBuffer = uByteToInt(byteBuffer);
                codestreamBytes = (intBuffer[1] >> 4) & 0x0F;
                idBytes = intBuffer[1] & 0x0F;
                if (fStream.read(byteBuffer, 2, (codestreamBytes + idBytes + 4)) != (codestreamBytes + idBytes + 4))
                    break;
                intBuffer = uByteToInt(byteBuffer);
                for (codestreamID = 0, bufferIndex = 2, i = 0; i < codestreamBytes; i++)
                    codestreamID = (codestreamID << 8) + intBuffer[bufferIndex++];
                for (databinID = 0, i = 0; i < idBytes; i++)
                    databinID = (databinID << 8) + intBuffer[bufferIndex++];
                for (length = 0, i = 0; i < 4; i++)
                    length = (length << 8) + intBuffer[bufferIndex++];
                boolean isComplete = ((intBuffer[0] & 1) == 1) ? true : false;
                databinClassID = (intBuffer[0] >> 1);
                if (length > byteBuffer.length) {
                    byteBuffer = new byte[byteBuffer.length + length + 256];
                }
                if (fStream.read(byteBuffer, 0, length) != length)
                    break;
                if ((databinClassID >= 0) && (databinClassID < 5))
                    this.Add_to_databin(databinClassID, codestreamID, databinID, byteBuffer, 0, length, isComplete, false, true);
            }
        } catch (Exception ex) {
            Log.error("Error in readFromCacheMethod in actual read algorithm.");
            ex.printStackTrace();
            return false;
        } finally {
            byteBuffer = null;
            intBuffer = null;
        }

        try {
            fStream.close();
        } catch (Exception ex) {
            Log.error("Error in readFromCacheMethod in file and FileInputStream closure.");
            ex.printStackTrace();
            return false;
        } finally {
            fStream = null;
        }

        return true;
    }

    /**
     * This method walks through all the databins and finds all the marked
     * databins and unmarks them. It builds a String out of these databins that
     * can be sent to the JPIP server using the 'model' header to update the
     * servers cache model.
     * 
     * @return String that can be sent to the JPIP server
     * @throws JHV_KduException
     */
    public String buildCacheModelUpdateString(boolean force) throws JHV_KduException {
        int length;
        long codestreamID, databinID;
        boolean isComplete[] = new boolean[1];
        StringBuilder cacheModel = new StringBuilder(1000);

        try {
            codestreamID = this.Get_next_codestream(-1);

            while (codestreamID >= 0) {
                // System.out.println("Modelling the codestream " + codestreamID
                // + "...");
                // Append the codestream label
                cacheModel.append("[" + codestreamID + "],");
                for (JPIPDatabinClass databinClass : JPIPDatabinClass.values()) {
                    databinID = this.Get_next_lru_databin(databinClass.getKakaduClassID(), codestreamID, -1, false);
                    while (databinID >= 0) {
                        if (force || Mark_databin(databinClass.getKakaduClassID(), codestreamID, databinID, false)) {
                            length = this.Get_databin_length(databinClass.getKakaduClassID(), codestreamID, databinID, isComplete);
                            // Append the databinClass String and the databinID
                            cacheModel.append(databinClass.getJpipString() + (databinClass == JPIPDatabinClass.MAIN_HEADER_DATABIN ? "" : String.valueOf(databinID)));
                            // If its not complete append the length of the
                            // databin
                            if (!isComplete[0])
                                cacheModel.append(":" + String.valueOf(length));
                            cacheModel.append(",");
                        }
                        databinID = this.Get_next_lru_databin(databinClass.getKakaduClassID(), codestreamID, databinID, false);
                    }
                }
                codestreamID = this.Get_next_codestream(codestreamID);
            }
            if (cacheModel.length() > 0)
                cacheModel.deleteCharAt(cacheModel.length() - 1);

        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
        return cacheModel.toString();
    }

    /**
     * Private helped method to convert a unsigned byte array to an integer
     * array. This method is only necessary since Java does not have unsigned
     * types.
     * 
     * @param _x
     *            unsigned byte array to convert
     * @return converted integer array
     */
    private static int[] uByteToInt(byte[] _x) {
        int[] ret = new int[_x.length];
        for (int i = 0; i < _x.length; i++)
            ret[i] = (_x[i] & 0xFF);
        return ret;
    }

    /**
     * @return All the cache files stored in the cache directory.
     */
    public static File[] getCacheFiles(File cachePath) {
        return cachePath.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.toUpperCase().endsWith(".HVC"));
            }
        });
    }

    /**
     * @return The associated cache file.
     */
    public File getCacheFile() {
        return cacheFile;
    }

    public static void updateCacheDirectory(File cachePath, double maxSize) {
        maxCacheSize = Math.round(maxSize * 1048576.0);
        updateCacheDirectory(cachePath);
    }

    /**
     * This method allows to remove the cache files according to the size limit
     * specified in the properties of the application. The files are removed
     * following a LRU order.
     */
    public static void updateCacheDirectory(File cachePath) {
        if (maxCacheSize <= 0)
            return;

        File[] list = getCacheFiles(cachePath);

        Arrays.sort(list, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return (int) (o1.lastModified() - o2.lastModified());
            }
        });

        long total = 0;

        for (File f : list)
            total += f.length();

        for (int i = 0; (total > maxCacheSize) && (i < list.length); i++) {
            total -= list[i].length();
            list[i].delete();
        }
    }
};
