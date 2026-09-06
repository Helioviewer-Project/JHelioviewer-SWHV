package org.helioviewer.jhv.event.filter;

import java.awt.EventQueue;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.EventCache;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKDownloader;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.plugins.swek.sources.HEKHandler;

import org.json.JSONObject;

public final class EventFilterRequestsTest {

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(Directories.CACHE.getPath()));
        SWEKGroup group = new SWEKGroup("Filter requests", "");
        SWEK.Parameter parameter = new SWEK.Parameter("value", "Value", new SWEK.ParameterFilter("numeric", 0, 10, 0, 1, ""), true);
        SWEKSupplier supplier = new SWEKSupplier(group, "test", "Test",
                new SWEK.Source("test", List.of(), new HEKHandler(), Map.of("value", SWEK.NumericType.INTEGER)), "filter-requests", List.of(parameter));
        SWEKCatalog.add(supplier);
        SWEKCatalog.setRelations(List.of());
        long start = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3), end = start + 1;
        check(EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false,
                List.of(event("low", 1, start, end), event("high", 2, start, end)), List.of()), supplier), "store observations");
        check(EventDatabase.addStoredInterval(start - TimeUnit.DAYS.toMillis(2), end + TimeUnit.DAYS.toMillis(2), supplier), "cover requests without network access");
        EventQueue.invokeAndWait(() -> {
            SWEKDownloader.installFilterListener();
            setFilter(supplier, 2);
            SWEKDownloader.setSupplierActive(supplier, true);
        });
        try {
            load(group, start, end);
            EventQueue.invokeAndWait(() -> {
                check(EventCache.getEvents(start, end).size() == 1, "activation uses existing filter");
                setFilter(supplier, 1);
                check(SWEKDownloader.isSupplierActive(supplier), "filter change retains activation");
                check(EventCache.getEvents(start, end).isEmpty(), "filter change clears previous observations");
            });
            load(group, start, end);
            EventQueue.invokeAndWait(() -> {
                check(EventCache.getEvents(start, end).size() == 2, "replacement state uses new filter");
                setFilter(supplier, 3);
            });
            load(group, start, end);
            EventQueue.invokeAndWait(() -> check(EventCache.getEvents(start, end).isEmpty(), "later replacement does not retain older filter"));
        } finally {
            EventQueue.invokeAndWait(() -> {
                SWEKDownloader.setSupplierActive(supplier, false);
                SWEKDownloader.removeFilterListener();
                SWEKDownloader.clearGroupChangedCallback();
                FilterManager.removeFilters(supplier);
            });
        }
        System.out.println("EventFilterRequestsTest passed");
    }

    private static void setFilter(SWEKSupplier supplier, double threshold) {
        FilterManager.removeFilters(supplier);
        FilterManager.addFilter(supplier, new SWEK.Param("value", threshold, SWEK.Operand.BIGGER_OR_EQUAL));
        FilterManager.fireFilters(supplier);
    }

    private static void load(SWEKGroup group, long start, long end) throws Exception {
        CountDownLatch idle = new CountDownLatch(1);
        EventQueue.invokeAndWait(() -> {
            SWEKDownloader.setGroupChangedCallback(changed -> {
                if (changed == group && !SWEKDownloader.isGroupBusy(group)) idle.countDown();
            });
            SWEKDownloader.requestForInterval(start, end);
        });
        check(idle.await(5, TimeUnit.SECONDS), "request finishes");
    }

    private static SWEKHandler.RemoteEvent event(String uid, int value, long start, long end) throws Exception {
        try (ByteArrayOutputStream output = JSONUtils.compressJSON(new JSONObject().put("event_title", uid).put("value", value))) {
            return new SWEKHandler.RemoteEvent(output.toByteArray(), start, end, start, uid, Map.of("value", value));
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
