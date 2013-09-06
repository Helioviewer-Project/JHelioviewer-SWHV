package org.helioviewer.base.math;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Vector;

import org.helioviewer.base.logging.Log;
import org.junit.Test;

public class IntervalsTest {
    public class TestEvent extends Interval<Integer> {

        public TestEvent(Integer start, Integer end) {
            super(new Interval<Integer>(start, end));
        }

        public String toString() {
            return "<TestEvent " + super.toString() + ">";
        }

    }

    /**
     * This makes some basic tests on adding events to an IntervalStore, and
     * makes some plausibily checks when calling the "needed" method
     */
    @Test
    public void testAddAndNeeded() {

        IntervalStore<Integer, TestEvent> I = new IntervalStore<Integer, TestEvent>();

        HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>> A = new HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>>();
        HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>> B = new HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>>();
        HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>> C = new HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>>();

        // these emulate request intervals
        A.put(new Interval<Integer>(100, 120), new IntervalContainer<Integer, TestEvent>());
        B.put(new Interval<Integer>(20, 30), new IntervalContainer<Integer, TestEvent>());
        C.put(new Interval<Integer>(0, 10), new IntervalContainer<Integer, TestEvent>());

        // these emulate events stored in the specific interval
        A.get(new Interval<Integer>(100, 120)).getItems().add(new TestEvent(110, 115));
        B.get(new Interval<Integer>(20, 30)).getItems().add(new TestEvent(20, 25));
        C.get(new Interval<Integer>(0, 10)).getItems().add(new TestEvent(5, 8));

        // add this data to the intervalstore
        I.add(A);
        I.add(B);
        I.add(C);

        // request some intervals we might need
        Vector<Interval<Integer>> resA = I.needed(new Interval<Integer>(100, 120));
        Vector<Interval<Integer>> resB = I.needed(new Interval<Integer>(100, 130));
        Vector<Interval<Integer>> resC = I.needed(new Interval<Integer>(20, 130));
        Vector<Interval<Integer>> resD = I.needed(new Interval<Integer>(15, 130));
        Vector<Interval<Integer>> resE = I.needed(new Interval<Integer>(5, 5));
        Vector<Interval<Integer>> resF = I.needed(new Interval<Integer>(10, 10));
        Vector<Interval<Integer>> resG = I.needed(new Interval<Integer>(11, 11));

        assertTrue(resA.size() == 0);

        assertTrue(resB.size() == 1);
        assertTrue(resB.contains(new Interval<Integer>(120, 130)));

        assertTrue(resC.size() == 2);
        assertTrue(resC.contains(new Interval<Integer>(30, 100)));
        assertTrue(resC.contains(new Interval<Integer>(120, 130)));

        assertTrue(resD.size() == 3);
        assertTrue(resD.contains(new Interval<Integer>(15, 20)));
        assertTrue(resD.contains(new Interval<Integer>(30, 100)));
        assertTrue(resD.contains(new Interval<Integer>(120, 130)));

        assertTrue(resE.size() == 0);

        assertTrue(resF.size() == 1);
        resF.contains(new Interval<Integer>(10, 10));

        assertTrue(resG.size() == 1);
        resG.contains(new Interval<Integer>(11, 11));

        // ///////

    }

    @Test
    public void testAddMerge() {

        IntervalStore<Integer, TestEvent> I = new IntervalStore<Integer, TestEvent>();
        IntervalStore<Integer, TestEvent> I2 = new IntervalStore<Integer, TestEvent>();

        HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>> A = new HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>>();
        HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>> A2 = new HashMap<Interval<Integer>, IntervalContainer<Integer, TestEvent>>();

        A.put(new Interval<Integer>(0, 100), new IntervalContainer<Integer, TestEvent>());
        A.put(new Interval<Integer>(110, 120), new IntervalContainer<Integer, TestEvent>());
        A.put(new Interval<Integer>(130, 140), new IntervalContainer<Integer, TestEvent>());

        A2.put(new Interval<Integer>(0, 115), new IntervalContainer<Integer, TestEvent>());
        A2.put(new Interval<Integer>(110, 120), new IntervalContainer<Integer, TestEvent>());
        A2.put(new Interval<Integer>(130, 140), new IntervalContainer<Integer, TestEvent>());
        A2.put(new Interval<Integer>(100, 110), new IntervalContainer<Integer, TestEvent>());

        I.add(A);
        Log.info("part2");
        I2.add(A2);

        Log.info(I);
        Log.info(I2);
        Log.info(I2);

        // TODO: Malte Nuhn - Do we need any logic to avoid overlapping
        // intervals?

    }
}
