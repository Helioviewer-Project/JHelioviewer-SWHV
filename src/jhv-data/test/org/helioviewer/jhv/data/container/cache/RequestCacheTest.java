package org.helioviewer.jhv.data.container.cache;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.math.Interval;
import org.junit.Before;
import org.junit.Test;

public class RequestCacheTest {

    @Before
    public void clearJHVEventCache() {
        JHVEventCache.getSingletonInstance().getRequestCache().clear();
    }

    @Test
    public void RequestCacheMissingIntervalTest() {
        JHVEventCache cache = JHVEventCache.getSingletonInstance();
        HashMap<Date, Interval<Date>> expectedRequestCache = new HashMap<Date, Interval<Date>>();
        List<Interval<Date>> expectedMissingInterval = new ArrayList<Interval<Date>>();

        Calendar c1 = new GregorianCalendar(2014, 01, 10, 14, 15, 00);
        Calendar c2 = new GregorianCalendar(2014, 01, 15, 14, 15, 00);
        Date date1 = c1.getTime();
        Date date2 = c2.getTime();
        expectedRequestCache.put(date1, new Interval<Date>(date1, date2));
        expectedMissingInterval.add(new Interval<Date>(date1, date2));
        JHVEventCacheResult result = cache.get(date1, date2);
        System.out.println("test empty request cache");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        Calendar c3 = new GregorianCalendar(2014, 01, 7, 14, 15, 00);
        Calendar c4 = new GregorianCalendar(2014, 01, 12, 14, 15, 00);
        Date date3 = c3.getTime();
        Date date4 = c4.getTime();
        result = cache.get(date3, date4);
        expectedRequestCache.clear();
        expectedMissingInterval.clear();
        expectedRequestCache.put(date3, new Interval<Date>(date3, date2));
        expectedMissingInterval.add(new Interval<Date>(date3, date1));
        System.out.println("Test new interval overlaps with interval in request cache: start before, end in interval in request cache");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        Calendar c5 = new GregorianCalendar(2014, 01, 12, 14, 15, 00);
        Calendar c6 = new GregorianCalendar(2014, 01, 18, 14, 15, 00);
        Date date5 = c5.getTime();
        Date date6 = c6.getTime();
        result = cache.get(date5, date6);
        expectedRequestCache.clear();
        expectedMissingInterval.clear();
        expectedMissingInterval.add(new Interval<Date>(date2, date6));
        expectedRequestCache.put(date3, new Interval<Date>(date3, date6));
        System.out.println("Test new interval overlaps with interval in request cache: start in interval in request cache, end after interval in request cache");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        Calendar c7 = new GregorianCalendar(2014, 01, 6, 14, 15, 00);
        Calendar c8 = new GregorianCalendar(2014, 01, 21, 14, 15, 00);
        Date date7 = c7.getTime();
        Date date8 = c8.getTime();
        result = cache.get(date7, date8);
        expectedRequestCache.clear();
        expectedMissingInterval.clear();
        expectedMissingInterval.add(new Interval<Date>(date7, date3));
        expectedMissingInterval.add(new Interval<Date>(date6, date8));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        System.out.println("Test new interval overlaps completly with interval in request cache");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        Calendar c9 = new GregorianCalendar(2014, 01, 2, 14, 15, 00);
        Calendar c10 = new GregorianCalendar(2014, 01, 4, 14, 15, 00);
        Date date9 = c9.getTime();
        Date date10 = c10.getTime();
        result = cache.get(date9, date10);
        expectedMissingInterval.clear();
        expectedMissingInterval.add(new Interval<Date>(date9, date10));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        System.out.println("Add separate interval in front");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        Calendar c11 = new GregorianCalendar(2014, 01, 24, 14, 15, 00);
        Calendar c12 = new GregorianCalendar(2014, 01, 26, 14, 15, 00);
        Date date11 = c11.getTime();
        Date date12 = c12.getTime();
        result = cache.get(date11, date12);
        expectedMissingInterval.clear();
        expectedMissingInterval.add(new Interval<Date>(date11, date12));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Add separate interval in after");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before first interval, end in second interval
        Calendar c13 = new GregorianCalendar(2014, 01, 1, 14, 15, 00);
        Calendar c14 = new GregorianCalendar(2014, 01, 7, 14, 15, 00);
        Date date13 = c13.getTime();
        Date date14 = c14.getTime();
        result = cache.get(date13, date14);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date13, date9));
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedRequestCache.put(date13, new Interval<Date>(date13, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval start before first finishes in second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in first interval, end in second interval
        Calendar c15 = new GregorianCalendar(2014, 1, 3, 14, 15, 00);
        Calendar c16 = new GregorianCalendar(2014, 1, 8, 14, 15, 00);
        Date date15 = c15.getTime();
        Date date16 = c16.getTime();
        result = cache.get(date15, date16);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in first finishes in second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in first interval and ends between second and
        // third interval
        Calendar c17 = new GregorianCalendar(2014, 1, 3, 14, 15, 00);
        Calendar c18 = new GregorianCalendar(2014, 1, 23, 14, 15, 00);
        Date date17 = c17.getTime();
        Date date18 = c18.getTime();
        result = cache.get(date17, date18);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date18));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date18));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in first finishes between second and third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in first interval and ends in third interval
        Calendar c19 = new GregorianCalendar(2014, 1, 3, 14, 15, 00);
        Calendar c20 = new GregorianCalendar(2014, 1, 25, 14, 15, 00);
        Date date19 = c19.getTime();
        Date date20 = c20.getTime();
        result = cache.get(date19, date20);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date12));
        System.out.println("Interval starts in first interval and finishes in third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in first interval and ends after third interval
        Calendar c21 = new GregorianCalendar(2014, 1, 3, 14, 15, 00);
        Calendar c22 = new GregorianCalendar(2014, 1, 28, 14, 15, 00);
        Date date21 = c21.getTime();
        Date date22 = c22.getTime();
        result = cache.get(date21, date22);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedMissingInterval.add(new Interval<Date>(date12, date22));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date22));
        System.out.println("Interval starts in first interval and finishes after third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before first interval and ends in between first
        // and second interval
        Calendar c23 = new GregorianCalendar(2014, 1, 1, 14, 15, 00);
        Calendar c24 = new GregorianCalendar(2014, 1, 5, 14, 15, 00);
        Date date23 = c23.getTime();
        Date date24 = c24.getTime();
        result = cache.get(date23, date24);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date23, date9));
        expectedMissingInterval.add(new Interval<Date>(date10, date24));
        expectedRequestCache.put(date23, new Interval<Date>(date23, date24));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts before first interval and finishes between first and second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before first interval and ends in between second
        // and the third interval
        Calendar c25 = new GregorianCalendar(2014, 1, 1, 14, 15, 00);
        Calendar c26 = new GregorianCalendar(2014, 1, 23, 14, 15, 00);
        Date date25 = c25.getTime();
        Date date26 = c26.getTime();
        result = cache.get(date25, date26);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date25, date9));
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date26));
        expectedRequestCache.put(date25, new Interval<Date>(date25, date26));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts before first interval and finishes between second and third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before first interval and ends in the third
        // interval
        Calendar c27 = new GregorianCalendar(2014, 1, 1, 14, 15, 00);
        Calendar c28 = new GregorianCalendar(2014, 1, 25, 14, 15, 00);
        Date date27 = c27.getTime();
        Date date28 = c28.getTime();
        result = cache.get(date27, date28);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date27, date9));
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedRequestCache.put(date27, new Interval<Date>(date27, date12));
        System.out.println("Interval starts before first interval and finishes in third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before first interval and ends after the third
        // interval
        Calendar c29 = new GregorianCalendar(2014, 1, 1, 14, 15, 00);
        Calendar c30 = new GregorianCalendar(2014, 1, 28, 14, 15, 00);
        Date date29 = c29.getTime();
        Date date30 = c30.getTime();
        result = cache.get(date29, date30);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date29, date9));
        expectedMissingInterval.add(new Interval<Date>(date10, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedMissingInterval.add(new Interval<Date>(date12, date30));
        expectedRequestCache.put(date29, new Interval<Date>(date29, date30));
        System.out.println("Interval starts before first interval and finishes after the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before second interval and ends before the second
        // interval
        Calendar c31 = new GregorianCalendar(2014, 1, 5, 14, 15, 00);
        Calendar c32 = new GregorianCalendar(2014, 1, 5, 17, 30, 00);
        Date date31 = c31.getTime();
        Date date32 = c32.getTime();
        result = cache.get(date31, date32);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date31, date32));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date31, new Interval<Date>(date31, date32));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts between first and second interval and finishes between first and second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before second interval and ends in the second
        // interval
        Calendar c33 = new GregorianCalendar(2014, 1, 5, 14, 15, 00);
        Calendar c34 = new GregorianCalendar(2014, 1, 9, 17, 30, 00);
        Date date33 = c33.getTime();
        Date date34 = c34.getTime();
        result = cache.get(date33, date34);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date33, date7));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date33, new Interval<Date>(date33, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts between first and second interval and finishes in second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before second interval and ends between the
        // second and third
        // interval
        Calendar c35 = new GregorianCalendar(2014, 1, 5, 14, 15, 00);
        Calendar c36 = new GregorianCalendar(2014, 1, 23, 17, 30, 00);
        Date date35 = c35.getTime();
        Date date36 = c36.getTime();
        result = cache.get(date35, date36);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date35, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date36));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date35, new Interval<Date>(date35, date36));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts between first and second interval and finishes between second and third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before second interval and ends in the
        // third interval
        Calendar c37 = new GregorianCalendar(2014, 1, 5, 14, 15, 00);
        Calendar c38 = new GregorianCalendar(2014, 1, 25, 17, 30, 00);
        Date date37 = c37.getTime();
        Date date38 = c38.getTime();
        result = cache.get(date37, date38);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date37, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date37, new Interval<Date>(date37, date12));
        System.out.println("Interval starts between first and second interval and finishes between in the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before second interval and ends after the third
        // interval
        Calendar c39 = new GregorianCalendar(2014, 1, 5, 14, 15, 00);
        Calendar c40 = new GregorianCalendar(2014, 1, 27, 17, 30, 00);
        Date date39 = c39.getTime();
        Date date40 = c40.getTime();
        result = cache.get(date39, date40);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date39, date7));
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedMissingInterval.add(new Interval<Date>(date12, date40));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date39, new Interval<Date>(date39, date40));
        System.out.println("Interval starts between first and second interval and finishes after the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in second interval and ends in the second
        // interval
        Calendar c41 = new GregorianCalendar(2014, 1, 7, 14, 15, 00);
        Calendar c42 = new GregorianCalendar(2014, 1, 9, 17, 30, 00);
        Date date41 = c41.getTime();
        Date date42 = c42.getTime();
        result = cache.get(date41, date42);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in second interval and finishes in the second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in second interval and ends between the second
        // and third interval
        Calendar c43 = new GregorianCalendar(2014, 1, 7, 14, 15, 00);
        Calendar c44 = new GregorianCalendar(2014, 1, 23, 17, 30, 00);
        Date date43 = c43.getTime();
        Date date44 = c44.getTime();
        result = cache.get(date43, date44);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date8, date44));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date44));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in second interval and finishes in between the second and third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in second interval and ends in the third
        // interval
        Calendar c45 = new GregorianCalendar(2014, 1, 7, 14, 15, 00);
        Calendar c46 = new GregorianCalendar(2014, 1, 25, 17, 30, 00);
        Date date45 = c45.getTime();
        Date date46 = c46.getTime();
        result = cache.get(date45, date46);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date12));
        System.out.println("Interval starts in second interval and finishes in the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in second interval and ends after the third
        // interval
        Calendar c47 = new GregorianCalendar(2014, 1, 7, 14, 15, 00);
        Calendar c48 = new GregorianCalendar(2014, 1, 27, 17, 30, 00);
        Date date47 = c47.getTime();
        Date date48 = c48.getTime();
        result = cache.get(date47, date48);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date8, date11));
        expectedMissingInterval.add(new Interval<Date>(date12, date48));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date48));
        System.out.println("Interval starts in second interval and finishes after the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in between the second and third interval and ends
        // in between the second and third interval
        Calendar c49 = new GregorianCalendar(2014, 1, 22, 14, 15, 00);
        Calendar c50 = new GregorianCalendar(2014, 1, 23, 17, 30, 00);
        Date date49 = c49.getTime();
        Date date50 = c50.getTime();
        result = cache.get(date49, date50);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date49, date50));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date49, new Interval<Date>(date49, date50));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in between the second and thrid interval and finishes in between the second and third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in between the second and third interval and ends
        // in the third interval
        Calendar c51 = new GregorianCalendar(2014, 1, 22, 14, 15, 00);
        Calendar c52 = new GregorianCalendar(2014, 1, 25, 17, 30, 00);
        Date date51 = c51.getTime();
        Date date52 = c52.getTime();
        result = cache.get(date51, date52);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date51, date11));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date51, new Interval<Date>(date51, date12));
        System.out.println("Interval starts in between the second and thrid interval and finishes in the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in between the second and third interval and ends
        // after the third interval
        Calendar c53 = new GregorianCalendar(2014, 1, 22, 14, 15, 00);
        Calendar c54 = new GregorianCalendar(2014, 1, 27, 17, 30, 00);
        Date date53 = c53.getTime();
        Date date54 = c54.getTime();
        result = cache.get(date53, date54);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date53, date11));
        expectedMissingInterval.add(new Interval<Date>(date12, date54));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date53, new Interval<Date>(date53, date54));
        System.out.println("Interval starts in between the second and third interval and finishes after the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in the third interval and ends in the third
        // interval
        Calendar c55 = new GregorianCalendar(2014, 1, 25, 14, 15, 00);
        Calendar c56 = new GregorianCalendar(2014, 1, 25, 17, 30, 00);
        Date date55 = c55.getTime();
        Date date56 = c56.getTime();
        result = cache.get(date55, date56);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in the third interval and finishes in the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in the third interval and ends after the third
        // interval
        Calendar c57 = new GregorianCalendar(2014, 1, 25, 14, 15, 00);
        Calendar c58 = new GregorianCalendar(2014, 1, 27, 17, 30, 00);
        Date date57 = c57.getTime();
        Date date58 = c58.getTime();
        result = cache.get(date57, date58);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date12, date58));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date58));
        System.out.println("Interval starts in the third interval and finishes after the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts after the third interval and ends after the third
        // interval
        Calendar c59 = new GregorianCalendar(2014, 1, 27, 14, 15, 00);
        Calendar c60 = new GregorianCalendar(2014, 1, 28, 17, 30, 00);
        Date date59 = c59.getTime();
        Date date60 = c60.getTime();
        result = cache.get(date59, date60);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date59, date60));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        expectedRequestCache.put(date59, new Interval<Date>(date59, date60));
        System.out.println("Interval starts after the third interval and finishes after the third interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before the first interval and ends before the
        // first
        // interval
        Calendar c61 = new GregorianCalendar(2014, 1, 1, 14, 15, 00);
        Calendar c62 = new GregorianCalendar(2014, 1, 1, 17, 30, 00);
        Date date61 = c61.getTime();
        Date date62 = c62.getTime();
        result = cache.get(date61, date62);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date61, date62));
        expectedRequestCache.put(date61, new Interval<Date>(date61, date62));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts before the first interval and finishes before the first interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts before the first interval and ends in the first
        // interval
        Calendar c63 = new GregorianCalendar(2014, 1, 1, 14, 15, 00);
        Calendar c64 = new GregorianCalendar(2014, 1, 3, 17, 30, 00);
        Date date63 = c63.getTime();
        Date date64 = c64.getTime();
        result = cache.get(date63, date64);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date63, date9));
        expectedRequestCache.put(date63, new Interval<Date>(date63, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts before the first interval and finishes in the first interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in the first interval and ends in the first
        // interval
        Calendar c65 = new GregorianCalendar(2014, 1, 3, 14, 15, 00);
        Calendar c66 = new GregorianCalendar(2014, 1, 3, 17, 30, 00);
        Date date65 = c65.getTime();
        Date date66 = c66.getTime();
        result = cache.get(date65, date66);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedRequestCache.put(date9, new Interval<Date>(date9, date10));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in the first interval and finishes in the first interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

        prepareRequestCache(cache, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12);

        // new interval starts in the first interval and ends between the first
        // and the second interval
        Calendar c67 = new GregorianCalendar(2014, 1, 3, 14, 15, 00);
        Calendar c68 = new GregorianCalendar(2014, 1, 5, 17, 30, 00);
        Date date67 = c67.getTime();
        Date date68 = c68.getTime();
        result = cache.get(date67, date68);
        expectedMissingInterval.clear();
        expectedRequestCache.clear();
        expectedMissingInterval.add(new Interval<Date>(date10, date68));
        expectedRequestCache.put(date9, new Interval<Date>(date9, date68));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date8));
        expectedRequestCache.put(date11, new Interval<Date>(date11, date12));
        System.out.println("Interval starts in the first interval and finishes between the first and the second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());
        assertMissingInterval("Testing missing interval", expectedMissingInterval, result.getMissingIntervals());

    }

    private void prepareRequestCache(JHVEventCache cache, Date date1, Date date2, Date date3, Date date4, Date date5, Date date6, Date date7, Date date8, Date date9, Date date10, Date date11, Date date12) {
        cache.getRequestCache().clear();
        cache.get(date1, date2);
        cache.get(date3, date4);
        cache.get(date5, date6);
        cache.get(date7, date8);
        cache.get(date9, date10);
        cache.get(date11, date12);
    }

    @Test
    public void RequestCacheRemoveIntervalTest() {

        JHVEventCache cache = JHVEventCache.getSingletonInstance();
        HashMap<Date, Interval<Date>> expectedRequestCache = new HashMap<Date, Interval<Date>>();

        //
        // No interval in cache
        //

        // Remove from empty request cache
        Date date7 = new GregorianCalendar(2014, 1, 7, 14, 15, 00).getTime();
        Date date10 = new GregorianCalendar(2014, 1, 10, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date7, date10));
        System.out.println("remove interval from empty request cache");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        //
        // One interval in cache
        //

        // remove before the interval
        prepareForOneInterval(cache);
        Date date15 = new GregorianCalendar(2014, 1, 15, 14, 15, 00).getTime();
        Date date20 = new GregorianCalendar(2014, 1, 20, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date7, date10));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove interval before interval in request cache");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start before end in interval in request cache
        prepareForOneInterval(cache);
        Date date17 = new GregorianCalendar(2014, 1, 17, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date10, date17));
        expectedRequestCache.clear();
        expectedRequestCache.put(date17, new Interval<Date>(date17, date20));
        System.out.println("remove start before end in interval in request cache");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start equals request cache start and remove end equals request
        // cache end
        prepareForOneInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date15, date20));
        expectedRequestCache.clear();
        System.out.println("remove start equals request cache start and remove end equals request cache end");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in request cache interval and remove end in request
        // cache interval
        prepareForOneInterval(cache);
        Date date16 = new GregorianCalendar(2014, 1, 16, 14, 15, 00).getTime();
        Date date19 = new GregorianCalendar(2014, 1, 19, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date16, date19));
        expectedRequestCache.clear();
        expectedRequestCache.put(date15, new Interval<Date>(date15, date16));
        expectedRequestCache.put(date19, new Interval<Date>(date19, date20));
        System.out.println("remove start in request cache interval and remove end in request cache interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in request cache interval and remove end after request
        // cache interval
        prepareForOneInterval(cache);
        Date date22 = new GregorianCalendar(2014, 1, 22, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date16, date22));
        expectedRequestCache.clear();
        expectedRequestCache.put(date15, new Interval<Date>(date15, date16));
        System.out.println("remove start in request cache interval and remove end after request cache interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start after request cache interval and remove end after
        // request cache interval
        prepareForOneInterval(cache);
        Date date26 = new GregorianCalendar(2014, 1, 26, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date22, date26));
        expectedRequestCache.clear();
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start after request cache interval and remove end after request cache interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        //
        // Two intervals in cache
        //

        // remove start and end before first interval
        prepareForTwoInterval(cache);
        Date date2 = new GregorianCalendar(2014, 1, 2, 14, 15, 00).getTime();
        Date date3 = new GregorianCalendar(2014, 1, 2, 14, 15, 00).getTime();
        Date date5 = new GregorianCalendar(2014, 1, 5, 14, 15, 00).getTime();
        Date date9 = new GregorianCalendar(2014, 1, 9, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date2, date3));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start and end before first interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start and end between first and second interval
        prepareForTwoInterval(cache);
        Date date11 = new GregorianCalendar(2014, 1, 11, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date10, date11));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start and end between first and second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start and end after second interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date22, date26));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start and end after second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start before first interval and end in first interval
        prepareForTwoInterval(cache);
        Date date6 = new GregorianCalendar(2014, 1, 6, 14, 15, 00).getTime();
        cache.removeRequestedIntervals(new Interval<Date>(date2, date6));
        expectedRequestCache.clear();
        expectedRequestCache.put(date6, new Interval<Date>(date6, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start before first interval and end in first interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start before first interval and ends between first and second
        // interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date2, date10));
        expectedRequestCache.clear();
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start before first interval and ends between first and second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start before first interval and ends after second interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date2, date22));
        expectedRequestCache.clear();
        System.out.println("remove start before first interval and ends after second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in first interval and ends in first interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date6, date7));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date6));
        expectedRequestCache.put(date7, new Interval<Date>(date7, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start in first interval and ends in first interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in first interval and ends between first and second
        // interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date6, date11));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date6));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start in first interval and ends between first and second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in first interval and ends in second
        // interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date6, date17));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date6));
        expectedRequestCache.put(date17, new Interval<Date>(date17, date20));
        System.out.println("remove start in first interval and ends in second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in first interval and ends after second
        // interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date6, date22));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date6));
        System.out.println("remove start in first interval and ends after second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in between first and second interval and ends in between
        // first and second interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date10, date11));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start in between first and second interval and ends in between first and second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in between first and second interval and ends in second
        // interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date10, date17));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date17, new Interval<Date>(date17, date20));
        System.out.println("remove start in between first and second interval and ends in second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in between first and second interval and ends after
        // second interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date10, date22));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        System.out.println("remove start in between first and second interval and ends after second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in second interval and ends in second interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date16, date17));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date16));
        expectedRequestCache.put(date17, new Interval<Date>(date17, date20));
        System.out.println("remove start in second interval and ends in second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start in second interval and ends after second interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date16, date22));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date16));
        System.out.println("remove start in second interval and ends after second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

        // remove start after second interval and ends after second interval
        prepareForTwoInterval(cache);
        cache.removeRequestedIntervals(new Interval<Date>(date22, date26));
        expectedRequestCache.clear();
        expectedRequestCache.put(date5, new Interval<Date>(date5, date9));
        expectedRequestCache.put(date15, new Interval<Date>(date15, date20));
        System.out.println("remove start after second interval and ends after second interval");
        assertRequestCache("Testing an empty request cache", expectedRequestCache, cache.getRequestCache());

    }

    private void prepareForOneInterval(JHVEventCache cache) {
        clearJHVEventCache();
        Date date15 = new GregorianCalendar(2014, 1, 15, 14, 15, 00).getTime();
        Date date20 = new GregorianCalendar(2014, 1, 20, 14, 15, 00).getTime();
        cache.get(date15, date20);
    }

    private void prepareForTwoInterval(JHVEventCache cache) {
        clearJHVEventCache();
        Date date5 = new GregorianCalendar(2014, 1, 5, 14, 15, 00).getTime();
        Date date9 = new GregorianCalendar(2014, 1, 9, 14, 15, 00).getTime();

        Date date15 = new GregorianCalendar(2014, 1, 15, 14, 15, 00).getTime();
        Date date20 = new GregorianCalendar(2014, 1, 20, 14, 15, 00).getTime();
        cache.get(date5, date9);
        cache.get(date15, date20);
    }

    private void assertMissingInterval(String string, List<Interval<Date>> expected, List<Interval<Date>> result) {
        if (expected != null && result != null) {
            if (expected.size() != result.size()) {
                fail("Result missing intervals has not the same amount of elements as the expected missing intervals");
            } else {
                for (Interval<Date> expectedInterval : expected) {
                    boolean found = false;
                    for (Interval<Date> resultInterval : result) {
                        if (expectedInterval.equals(resultInterval)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        fail("No interval found in result for expected interval " + expectedInterval);
                    }
                }
            }
        } else {
            if (expected == null && result != null) {
                fail("Result was not null, expected was");
            }
            if (expected != null && result == null) {
                fail("Result was null, not expected");
            }

        }
    }

    public void assertRequestCache(String message, Map<Date, Interval<Date>> expected, Map<Date, Interval<Date>> result) {
        if (expected != null && result != null) {
            if (expected.size() != result.size()) {
                fail("Result request cache has not the same amount of elements as the expected cache");
            } else {
                for (Date date : expected.keySet()) {
                    if (result.containsKey(date)) {
                        if (!result.get(date).equals(expected.get(date))) {
                            fail("Interval for date " + date + " didn't have the expected interval in the result");
                        }
                    } else {
                        fail("Date " + date + " was expected in the result, but not found");
                    }
                }
            }
        } else {
            if (expected == null && result != null) {
                fail("Result was not null, expected was");
            }
            if (expected != null && result == null) {
                fail("Result was null, not expected");
            }

        }
    }
}
