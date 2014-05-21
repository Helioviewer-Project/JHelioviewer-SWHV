package org.helioviewer.plugins.eveplugin.radio.test;

import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.radio.data.DownloadRequestData;
import org.helioviewer.plugins.eveplugin.radio.data.FrequencyInterval;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDataManagerListener;

public class DataChecker implements RadioDataManagerListener {

    private byte[] previousData;

    public DataChecker() {
        previousData = new byte[0];
    }

    @Override
    public void downloadRequestAnswered(FrequencyInterval freqInterval, Interval<Date> timeInterval, long ID, String identifier) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newDataAvailable(DownloadRequestData downloadRequestData, long ID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadFinished(long ID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, String identifier, long radioImageID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newGlobalFrequencyInterval(FrequencyInterval interval) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> ID, String identifier, Long radioImageID) {
        comparePreviousData(data);
        previousData = data;
    }

    private void comparePreviousData(byte[] data) {
        boolean different = false;
        int c = 0;
        Log.debug("Comparing this data with the previous data \n");
        if (data.length == previousData.length) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] != previousData[i]) {
                    Log.debug("Data different at position " + i + "\n");
                    different = true;
                    c++;
                }
                if (c > 200) {
                    Log.debug("...\n");
                    break;
                }
            }
            if (different) {
                Log.debug("The data was different see result \n");
            } else {
                Log.debug("The data was not different \n");
            }
        } else {
            Log.debug("Data length was different \n");
        }

    }

    @Override
    public void clearAllSavedImages(String plotIDentifier) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadRequestDataRemoved(DownloadRequestData drd, long ID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd, long ID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newDataForIDReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, Long ID, String identifier, Long imageID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void additionDownloadRequestAnswered(Long downloadID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearAllSavedImagesForID(Long downloadID, Long imageID, String plotIDentifier) {
        // TODO Auto-generated method stub

    }

    @Override
    public void intervalTooBig(long iD, String identifier) {
        // TODO Auto-generated method stub

    }

    @Override
    public void noDataInterval(List<Interval<Date>> noDataList, Long downloadID, String plotIdentifier) {
        // TODO Auto-generated method stub
        
    }

}
