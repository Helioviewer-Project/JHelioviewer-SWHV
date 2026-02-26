package org.helioviewer.jhv.base;

public class ArrayUtils {

    public static float selectKth(float[] data, int left, int right, int k) {
        while (left < right) {
            int pivotIndex = (left + right) >>> 1;
            float pivot = data[pivotIndex];
            int i = left;
            int j = right;
            while (i <= j) {
                while (data[i] < pivot) i++;
                while (data[j] > pivot) j--;
                if (i <= j) {
                    float tmp = data[i];
                    data[i++] = data[j];
                    data[j--] = tmp;
                }
            }
            if (k <= j) {
                right = j;
            } else if (k >= i) {
                left = i;
            } else {
                return data[k];
            }
        }
        return data[left];
    }

}
