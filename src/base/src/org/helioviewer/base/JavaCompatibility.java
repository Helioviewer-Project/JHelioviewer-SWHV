package org.helioviewer.base;

import java.lang.reflect.Array;

/**
 * This class introduces some workarounds to have commonly used JAVA 6
 * functionality methods available in JAVA 5, too
 * 
 * @author Malte Nuhn
 */
public class JavaCompatibility {

    /**
     * Workaround for having Double.MIN_NORMAL (the smallest positive value)
     * also available in Java 1.5
     */
    public static final double DOUBLE_MIN_NORMAL = Double.longBitsToDouble(0x0010000000000000L);

    /**
     * Workaround for having Arrays.CopyOf available in Java 1.5 reallocates an
     * array with a new size, and copies the contents of the old array to the
     * new array.
     * 
     * @param oldArray
     *            the old array, to be reallocated.
     * @param newSize
     *            the new array size.
     * @return A new array with the same contents.
     */
    public static <T> T[] copyArray(T[] oldArray, int newSize) {
        Object newArray = Array.newInstance(oldArray.getClass().getComponentType(), newSize);
        System.arraycopy(oldArray, 0, newArray, 0, Math.min(newSize, oldArray.length));
        @SuppressWarnings("unchecked")
        T[] res = (T[]) newArray;
        return res;
    }

    /**
     * Workaround for having Arrays.CopyOf available in Java 1.5 reallocates an
     * array with a new size, and copies the contents of the old array to the
     * new array.
     * 
     * @param oldArray
     *            the old array, to be reallocated.
     * @param newSize
     *            the new array size.
     * @return A new array with the same contents.
     */
    public static String[] copyArrayString(final String[] oldArray, final int newSize) {
        String[] newArray = new String[newSize];
        System.arraycopy(oldArray, 0, newArray, 0, Math.min(newSize, oldArray.length));
        String[] res = (String[]) newArray;
        return res;
    }

}
