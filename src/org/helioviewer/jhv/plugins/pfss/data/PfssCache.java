package org.helioviewer.jhv.plugins.pfss.data;

import org.helioviewer.jhv.plugins.pfss.PfssSettings;

import com.jogamp.opengl.GL2;

// Datastructure to cache the Pfss-Data with preload function
public class PfssCache {

    private final PfssData[] data = new PfssData[PfssSettings.CACHE_SIZE];
    private int numberOfElementsInCache = 0;

    public void addData(PfssData pfssData) {
        if (numberOfElementsInCache < PfssSettings.CACHE_SIZE) {
            data[numberOfElementsInCache] = pfssData;
            numberOfElementsInCache++;
            bubbleSort();
        }
    }

    private void bubbleSort() {
        boolean swapped = true;
        int j = 0;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < numberOfElementsInCache - j; i++) {
                if (data[i].time > data[i + 1].time) {
                    PfssData tmp = data[i];
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
        if (found >= 0)
            return data[found];

        if (-found >= numberOfElementsInCache)
            return data[numberOfElementsInCache - 1];

        if (-found - 1 >= 0) {
            long diff1 = Math.abs(data[-found - 1].time - timestamp);
            long diff2 = Math.abs(data[-found].time - timestamp);
            if (diff1 < diff2) {
                return data[-found - 1];
            }
        }
        return data[-found];
    }

    private int binarySearch(long timestamp) {
        int low = 0;
        int high = numberOfElementsInCache - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = data[mid].time;

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
