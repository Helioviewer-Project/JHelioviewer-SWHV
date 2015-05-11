package org.helioviewer.gl3d.plugin.pfss.data;

import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

/**
 * Datastructur to cache the Pfss-Data with preload function
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssCache {

    private final PfssData[] data = new PfssData[PfssSettings.CACHE_SIZE];
    private int numberOfElementsInCache = 0;

    /**
     * The private constructor to support the singleton pattern.
     * */
    public PfssCache() {
    }

    public void preloadData(long time, String url) {

    }

    public void addData(PfssData pfssData) {
        if (numberOfElementsInCache < PfssSettings.CACHE_SIZE) {
            this.data[numberOfElementsInCache] = pfssData;
            numberOfElementsInCache++;
            bubbleSort();
        }
    }

    public void bubbleSort() {
        boolean swapped = true;
        int j = 0;
        PfssData tmp;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < numberOfElementsInCache - j; i++) {
                if (data[i].getTime() > data[i + 1].getTime()) {
                    tmp = data[i];
                    data[i] = data[i + 1];
                    data[i + 1] = tmp;
                    swapped = true;
                }
            }
        }
    }

    public PfssData getData(long timestamp) {
        if (numberOfElementsInCache <= 0)
            return null;
        int found = binarySearch(timestamp);
        if (found < 0) {
            if (-found < numberOfElementsInCache) {
                if (-found - 1 >= 0) {
                    long diff1 = Math.abs(data[-found - 1].getTime() - timestamp);
                    long diff2 = Math.abs(data[-found].getTime() - timestamp);
                    if (diff1 < diff2) {
                        return data[-found - 1];
                    }
                }
                return data[-found];
            } else {
                return data[numberOfElementsInCache - 1];
            }
        } else {
            return data[found];
        }
    }

    public int binarySearch(long timestamp) {
        int low = 0;
        int high = numberOfElementsInCache - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = data[mid].getTime();

            if (midVal < timestamp)
                low = mid + 1;
            else if (midVal > timestamp)
                high = mid - 1;
            else
                return mid;
        }
        return -(low + 1);
    }

    public void clear() {
        numberOfElementsInCache = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = null;
        }
    }

}
