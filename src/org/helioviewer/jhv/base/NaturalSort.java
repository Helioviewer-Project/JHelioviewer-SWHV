package org.helioviewer.jhv.base;

import java.util.Comparator;

// Alphanumeric order: digit runs compare as numbers, other runs as case-sensitive strings.
public final class NaturalSort {

    public static final Comparator<CharSequence> comparator = NaturalSort::compare;

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int chunkEnd(CharSequence s, int start) {
        int length = s.length();
        boolean digits = isDigit(s.charAt(start));
        int end = start + 1;
        while (end < length && isDigit(s.charAt(end)) == digits)
            end++;
        return end;
    }

    private static int compare(CharSequence s1, CharSequence s2) {
        int i1 = 0;
        int i2 = 0;
        int length1 = s1.length();
        int length2 = s2.length();
        while (i1 < length1 && i2 < length2) {
            int end1 = chunkEnd(s1, i1);
            int end2 = chunkEnd(s2, i2);
            String chunk1 = s1.subSequence(i1, end1).toString();
            String chunk2 = s2.subSequence(i2, end2).toString();
            i1 = end1;
            i2 = end2;

            int result;
            if (isDigit(chunk1.charAt(0)) && isDigit(chunk2.charAt(0))) {
                result = chunk1.length() - chunk2.length();
                if (result == 0)
                    result = chunk1.compareTo(chunk2);
            } else
                result = chunk1.compareTo(chunk2);
            if (result != 0)
                return result;
        }
        return length1 - length2;
    }

    private NaturalSort() {}

}
