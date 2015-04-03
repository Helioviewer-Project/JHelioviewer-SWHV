package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Datastructur to cache the Pfss-Data with preload function
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssCache {
    public final static int CACHE_SIZE = 125;

    private final PfssData[] data = new PfssData[CACHE_SIZE];
    private int numberOfElementsInCache = 0;
    private final static ExecutorService pfssPool = Executors.newFixedThreadPool(5);

    /**
     * The private constructor to support the singleton pattern.
     * */
    public PfssCache() {
    }

    public void preloadData(long time, String url) {

        Thread t = new Thread(new PfssDataLoader(url, time, this), "PFFSLoader");
        pfssPool.submit(t);

    }

    public void addData(PfssData pfssData) {
        if (numberOfElementsInCache < CACHE_SIZE) {
            this.data[numberOfElementsInCache] = pfssData;
            bubbleSort();
        }
        numberOfElementsInCache++;
    }

    public void bubbleSort() {
        boolean swapped = true;
        int j = 0;
        PfssData tmp;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < data.length - j; i++) {
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
                if (-found + 1 < numberOfElementsInCache) {
                    long diff1 = Math.abs(data[-found + 1].getTime() - timestamp);
                    long diff2 = Math.abs(data[-found].getTime() - timestamp);
                    if (diff1 < diff2) {
                        return data[-found + 1];
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
}
