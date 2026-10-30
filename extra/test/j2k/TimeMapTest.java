package org.helioviewer.jhv.time;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public final class TimeMapTest {

    private record Item(JHVTime time, String name) {}

    public static void main(String[] args) {
        List<Item> items = new ArrayList<>(List.of(item(100, "a"), item(200, "b"), item(400, "c")));
        TimeMap<Item> ordered = new TimeMap<>(items, Item::time);
        TimeMap<Item> collected = new TimeMap<>();
        collected.put(items.get(2).time(), items.get(2));
        collected.put(items.get(0).time(), items.get(0));
        collected.put(items.get(1).time(), item(200, "replaced"));
        collected.put(items.get(1).time(), items.get(1));
        collected.buildIndex();
        checkLookups(ordered, items);
        checkLookups(collected, items);
        items.clear();
        check(ordered.size() == 3 && ordered.valueAt(1).name().equals("b"), "Ordered input was not copied");

        Item replacement = item(200, "new");
        collected.put(replacement.time(), replacement);
        collected.buildIndex();
        check(collected.valueAt(1) == replacement && collected.nearestValue(new JHVTime(210)) == replacement, "Replacement was not indexed");
        collected.clear();
        check(collected.isEmpty() && collected.size() == 0, "Clear left entries");
        Item single = item(300, "single");
        collected.put(single.time(), single);
        collected.buildIndex();
        checkLookups(collected, List.of(single));

        reject(List.of());
        reject(List.of(item(200, "a"), item(100, "b")));
        reject(List.of(item(100, "a"), item(100, "b")));
        System.out.println("PASS: ordered and collected time indexes, clamping, ties, neighbors, replacement and clear/rebuild");
    }

    private static void checkLookups(TimeMap<Item> sequence, List<Item> items) {
        TreeMap<JHVTime, Item> reference = new TreeMap<>();
        for (Item item : items)
            reference.put(item.time(), item);
        check(sequence.size() == items.size() && sequence.maxIndex() == items.size() - 1, "Size changed");
        check(sequence.firstTime().equals(items.getFirst().time()) && sequence.lastTime().equals(items.getLast().time()), "Endpoints changed");
        for (int i = -2; i < items.size() + 2; i++) {
            Item expected = items.get(Math.clamp(i, 0, items.size() - 1));
            check(sequence.valueAt(i) == expected && sequence.timeAt(i).equals(expected.time()), "Indexed access changed at " + i);
        }
        for (long milli = 0; milli <= 500; milli++) {
            JHVTime time = new JHVTime(milli);
            JHVTime lower = reference.lowerKey(time);
            JHVTime higher = reference.higherKey(time);
            check(sequence.previousTime(time).equals(lower == null ? reference.firstKey() : lower), "Strict lower changed at " + milli);
            check(sequence.nextTime(time).equals(higher == null ? reference.lastKey() : higher), "Strict higher changed at " + milli);
            JHVTime floor = reference.floorKey(time);
            JHVTime ceiling = reference.ceilingKey(time);
            check(sequence.floorValue(time) == reference.get(floor == null ? reference.firstKey() : floor), "Inclusive floor changed at " + milli);
            check(sequence.ceilingValue(time) == reference.get(ceiling == null ? reference.lastKey() : ceiling), "Inclusive ceiling changed at " + milli);
            int nearest = 0;
            long distance = Long.MAX_VALUE;
            for (int i = 0; i < items.size(); i++) {
                long candidate = Math.abs(items.get(i).time().milli - milli);
                if (candidate <= distance) {
                    nearest = i;
                    distance = candidate;
                }
            }
            check(sequence.nearestIndex(time) == nearest && sequence.nearestValue(time) == items.get(nearest)
                    && sequence.nearestTime(time).equals(items.get(nearest).time()), "Nearest changed at " + milli);
        }
    }

    private static Item item(long milli, String name) {
        return new Item(new JHVTime(milli), name);
    }

    private static void reject(List<Item> items) {
        try {
            new TimeMap<>(items, Item::time);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid ordered input was accepted");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
