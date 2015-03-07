package org.helioviewer.base.math;

/**
 * A collection of useful static methods.
 * 
 * @author caplins
 * 
 */
public class MathUtils {

    public static final double radeg = 180.0 / Math.PI;

    /**
     * Returns the integer, x, closest on the number line such that
     * min(_side1,_side2) <= x <= max(_side1,_side2).
     * 
     * @param _val
     *            the value to squeee into the interval
     * @param _side1
     *            one side of the interval
     * @param _side2
     *            the other side of the interval
     * @return the closest value within the interval
     */
    public static int squeezeToInterval(int _val, int _side1, int _side2) {
        int temp = Math.max(_side1, _side2);
        _side1 = Math.min(_side1, _side2);
        _side2 = temp;
        if (_val <= _side1)
            return _side1;
        else if (_val >= _side2)
            return _side2;
        else
            return _val;
    }

    /**
     * Returns a random integer from the interval. If the interval is 2->4 then
     * there is a equal chance that the return would be 2,3, or 4.
     * 
     * @param _interval
     *            the interval from which to draw the random number
     * @return a random numbe wihtin the given interval
     */
    public static int randomInt(Interval<Integer> _interval) {
        // calculate how many integers we might have
        final int numIntegers = _interval.getEnd() - _interval.getStart() + 1;
        int rand;
        do {
            rand = (int) (Math.random() * numIntegers);
        } while (rand == numIntegers);
        return rand + _interval.getStart();
    }

    /**
     * Takes and returns the maximum value from the given args.
     * 
     * @param _is
     *            the values to compare
     * @return the maximum of the given values
     */
    public static int max(int... _is) {
        int max = Integer.MIN_VALUE;
        for (int i : _is)
            if (max < i)
                max = i;
        return max;
    }

    /**
     * Takes and returns the minimum value from the given args.
     * 
     * @param _is
     *            the values to compare
     * @return the minimum of the given values
     */
    public static int min(int... _is) {
        int min = Integer.MAX_VALUE;
        for (int i : _is)
            if (min > i)
                min = i;
        return min;
    }

    public static double mapTo0To360(double x) {
        double tmp = x % 360.0;
        if (tmp < 0) {
            return tmp + 360.0;
        } else {
            return tmp;
        }
    }

};
