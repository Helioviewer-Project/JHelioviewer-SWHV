package org.helioviewer.jhv.timelines.band;

import java.util.Arrays;

class IntArray {

    private static final int CHUNK = 1024;

    private int[] array;
    private int length;

    IntArray() {
        this(0);
    }

    IntArray(int size) {
        array = new int[size < CHUNK ? CHUNK : size];
    }

    void put(int v) {
        if (length + 1 > array.length) {
            array = Arrays.copyOf(array, array.length + CHUNK);
        }
        array[length] = v;
        length++;
    }

    int length() {
        return length;
    }

    int[] array() {
        return array;
    }

    void clear() {
        length = 0;
    }

}
