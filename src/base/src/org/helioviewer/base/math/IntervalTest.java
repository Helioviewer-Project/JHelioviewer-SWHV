package org.helioviewer.base.math;

import static org.junit.Assert.assertTrue;

import java.util.Vector;

import org.junit.Test;

public class IntervalTest {
    public class TestEvent extends Interval<Integer> {

        public TestEvent(Integer start, Integer end) {
            super(new Interval<Integer>(start, end));
        }

        public String toString() {
            return "<TestEvent " + super.toString() + ">";
        }

    }

    // BASED ON THIS CONFIGURATION

    //
    // [ I1 ] [ I6 ]
    // [ I2 ] [ I5 ]
    // [ I3 ]
    // [ I4 ]

    // Interval<Date> I1 = new Interval<Date>(new Date(1273506656), new
    // Date(1273507656));

    TestEvent I1 = new TestEvent(0, 90);
    TestEvent I2 = new TestEvent(10, 40);
    TestEvent I3 = new TestEvent(20, 70);
    TestEvent I4 = new TestEvent(30, 60);
    TestEvent I5 = new TestEvent(50, 80);
    TestEvent I6 = new TestEvent(1000, 2000);

    @Test
    public void testOverlaps() {
        assertTrue(I1.overlaps(I1));
        assertTrue(I1.overlaps(I2));
        assertTrue(I1.overlaps(I3));
        assertTrue(I1.overlaps(I4));
        assertTrue(I1.overlaps(I5));
        assertTrue(!I1.overlaps(I6));

        assertTrue(I2.overlaps(I1));
        assertTrue(I2.overlaps(I2));
        assertTrue(I2.overlaps(I3));
        assertTrue(I2.overlaps(I4));
        assertTrue(!I2.overlaps(I5));
        assertTrue(!I2.overlaps(I6));

        assertTrue(I3.overlaps(I1));
        assertTrue(I3.overlaps(I2));
        assertTrue(I3.overlaps(I3));
        assertTrue(I3.overlaps(I4));
        assertTrue(I3.overlaps(I5));
        assertTrue(!I3.overlaps(I6));

        assertTrue(I4.overlaps(I1));
        assertTrue(I4.overlaps(I2));
        assertTrue(I4.overlaps(I3));
        assertTrue(I4.overlaps(I4));
        assertTrue(I4.overlaps(I5));
        assertTrue(!I4.overlaps(I6));

        assertTrue(I5.overlaps(I1));
        assertTrue(!I5.overlaps(I2));
        assertTrue(I5.overlaps(I3));
        assertTrue(I5.overlaps(I4));
        assertTrue(I5.overlaps(I5));
        assertTrue(!I5.overlaps(I6));

    }

    @Test
    public void testContainsInterval() {
        assertTrue(I1.contains(I2));
        assertTrue(I1.contains(I3));
        assertTrue(I1.contains(I4));
        assertTrue(I1.contains(I5));

        assertTrue(!I2.contains(I1));
        assertTrue(!I2.contains(I3));
        assertTrue(!I2.contains(I4));
        assertTrue(!I2.contains(I5));

        assertTrue(!I3.contains(I1));
        assertTrue(!I3.contains(I2));
        assertTrue(I3.contains(I4));
        assertTrue(!I3.contains(I5));

        assertTrue(!I4.contains(I1));
        assertTrue(!I4.contains(I2));
        assertTrue(!I4.contains(I3));
        assertTrue(!I4.contains(I5));

        assertTrue(!I5.contains(I1));
        assertTrue(!I5.contains(I2));
        assertTrue(!I5.contains(I3));
        assertTrue(!I5.contains(I4));
    }

    @Test
    public void testContainsLong() {
        assertTrue(I1.containsPoint(0)); // start inclusive
        assertTrue(!I1.containsPoint(90)); // end exclusive
        assertTrue(I1.containsPoint(5));
        assertTrue(!I1.containsPoint(-1));
        assertTrue(!I1.containsPoint(1000000000));

    }

    @Test
    public void testExclude() {
        TestEvent A = new TestEvent(0, 100);
        TestEvent B = new TestEvent(10, 100);
        TestEvent C = new TestEvent(110, 120);
        TestEvent D = new TestEvent(0, 110);
        TestEvent E = new TestEvent(10, 20);

        Vector<Interval<Integer>> resultAB = new Vector<Interval<Integer>>();
        resultAB.add(new Interval<Integer>(0, 10));
        assertTrue(A.exclude(B).equals(resultAB));

        Vector<Interval<Integer>> resultAC = new Vector<Interval<Integer>>();
        resultAC.add(new Interval<Integer>(0, 100));
        assertTrue(A.exclude(C).equals(resultAC));

        Vector<Interval<Integer>> resultCB = new Vector<Interval<Integer>>();
        resultCB.add(new Interval<Integer>(110, 120));
        assertTrue(C.exclude(B).equals(resultCB));

        Vector<Interval<Integer>> resultDA = new Vector<Interval<Integer>>();
        resultDA.add(new Interval<Integer>(100, 110));
        assertTrue(D.exclude(A).equals(resultDA));

        Vector<Interval<Integer>> resultAE = new Vector<Interval<Integer>>();
        resultAE.add(new Interval<Integer>(0, 10));
        resultAE.add(new Interval<Integer>(20, 100));
        assertTrue(A.exclude(E).equals(resultAE));

        assertTrue(A.exclude(A).size() == 0);

    }

    @Test
    public void testExpand() {
        Interval<Integer> A = new Interval<Integer>(0, 100);
        System.out.println(A.expand(new Interval<Integer>(10, 20)));
        assertTrue(A.expand(new Interval<Integer>(0, 100)).equals(new Interval<Integer>(0, 100)));
        assertTrue(A.expand(new Interval<Integer>(10, 20)).equals(new Interval<Integer>(0, 100)));
        assertTrue(A.expand(new Interval<Integer>(10, 10)).equals(new Interval<Integer>(0, 100)));
        assertTrue(A.expand(new Interval<Integer>(10, 100)).equals(new Interval<Integer>(0, 100)));
        assertTrue(A.expand(new Interval<Integer>(0, 90)).equals(new Interval<Integer>(0, 100)));
        assertTrue(A.expand(new Interval<Integer>(-10, 0)).equals(new Interval<Integer>(-10, 100)));
        assertTrue(A.expand(new Interval<Integer>(-10, 110)).equals(new Interval<Integer>(-10, 110)));
        assertTrue(A.expand(new Interval<Integer>(100, 110)).equals(new Interval<Integer>(0, 110)));
        assertTrue(A.expand(new Interval<Integer>(90, 110)).equals(new Interval<Integer>(0, 110)));
    }
    /*
     * Test public void testStuff() { assertTrue((new
     * IntervalBucket<Integer,IntegerEvent>(0,100)).equals(new
     * IntervalBucket<Integer,IntegerEvent>(0,100))); assertTrue(!(new
     * IntervalBucket<Integer,IntegerEvent>(0,100)).equals(new
     * IntervalBucket<Integer,IntegerEvent>(0,10)));
     * 
     * }
     * 
     * @Test public void testAddItems() {
     * IntervalBucket<Integer,SimpleInterval<Integer>> I = new
     * IntervalBucket<Integer,SimpleInterval<Integer>>(10,20); I.addItem(new
     * SimpleInterval<Integer>(10,10)); I.addItem(new
     * SimpleInterval<Integer>(11,11)); I.addItem(new
     * SimpleInterval<Integer>(12,12)); I.addItem(new
     * SimpleInterval<Integer>(20,20));
     * 
     * IntervalBucket<Integer,SimpleInterval<Integer>> resA = new
     * IntervalBucket<Integer,SimpleInterval<Integer>>(10,20); resA.addItem(new
     * SimpleInterval<Integer>(10,10)); resA.addItem(new
     * SimpleInterval<Integer>(11,11)); resA.addItem(new
     * SimpleInterval<Integer>(12,12)); resA.addItem(new
     * SimpleInterval<Integer>(20,20));
     * 
     * IntervalBucket<Integer,SimpleInterval<Integer>> resB = new
     * IntervalBucket<Integer,SimpleInterval<Integer>>(10,20); resB.addItem(new
     * SimpleInterval<Integer>(10,10)); resB.addItem(new
     * SimpleInterval<Integer>(11,11)); resB.addItem(new
     * SimpleInterval<Integer>(12,12));
     * 
     * IntervalBucket<Integer,SimpleInterval<Integer>> resC = new
     * IntervalBucket<Integer,SimpleInterval<Integer>>(10,20); resC.addItem(new
     * SimpleInterval<Integer>(11,11)); resC.addItem(new
     * SimpleInterval<Integer>(12,12)); resC.addItem(new
     * SimpleInterval<Integer>(20,20));
     * 
     * assertTrue(I.equals(resA)); assertTrue(I.equals(resB));
     * assertTrue(!I.equals(resC));
     * 
     * }
     */
}
