package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;

import org.json.JSONObject;

public final class EventCacheTest {

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "groups" -> {
                checkIntervals();
                checkGroups();
            }
            case "downloads" -> checkDownloads();
            default -> throw new IllegalArgumentException("Unknown event test: " + args[0]);
        }
        System.out.println("EventCacheTest " + args[0] + " passed");
    }

    public static void checkLoadedEvents(List<SolarEvent> events) {
        EventCache.replaceEvents(new EventBatch(1, events, List.of()));
        for (SolarEvent event : events) {
            RelatedEvents group = EventCache.getRelatedEvents(event.getUniqueID());
            check(EventCache.getEvents(event.start, event.start).contains(group), "loaded event visible at start");
            check(EventCache.getEvents(event.end, event.end).contains(group), "loaded event visible at end");
            check(group.getClosestTo(event.start) == event, "canvas representative");
            check(group.getIntervals().equals(List.of(new Interval(event.start, event.end))), "singleton timeline interval");
        }
    }

    private static void checkGroups() throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        checkGroupOwnership();
        checkAssociationReplacement();
        checkPendingLinkRemoval();
        checkVersionsSurviveRegrouping();
        checkRebuiltGroupOrder();
        checkRemovalNotifications();
        checkRequestOwnership();
        EventCollection groups = new EventCollection();
        SolarEvent first = new SolarEvent(null, 1, 100, 200);
        SolarEvent second = new SolarEvent(null, 2, 400, 500);
        groups.addEvent(first);
        groups.addEvent(second);
        groups.addAssociation(new SolarEvent.Link(1, 2));
        RelatedEvents group = groups.getRelatedEvents(1);
        check(groups.getEvents(300, 300).isEmpty(), "no phantom event in gap");
        check(groups.getEvents(200, 200).size() == 1, "inclusive end");
        check(groups.getEvents(400, 400).size() == 1, "inclusive start");
        check(group.getClosestTo(390) == second, "nearest event outside intervals");
        check(group.getIntervals().equals(List.of(new Interval(100, 200), new Interval(400, 500))), "timeline gaps");
        groups.addEvent(new SolarEvent(null, 2, 190, 500));
        check(group.getIntervals().equals(List.of(new Interval(100, 500))), "overlapping interval union");
        check(groups.getEvents(300, 300).size() == 1, "updated interval visible");
        List<Interval> previousIntervals = group.getIntervals();
        groups.addEvent(new SolarEvent(null, 2, 150, 175));
        check(group.getStart() == 100 && group.getEnd() == 200, "replacement shrinks group bounds");
        check(group.getIntervals().equals(List.of(new Interval(100, 200))), "contained observation does not extend coverage");
        check(previousIntervals.equals(List.of(new Interval(100, 500))), "previous coverage snapshot stays unchanged");
        check(groups.getEvents(300, 300).isEmpty(), "old coverage removed from cache");

        groups.addEvent(new SolarEvent(null, 3, 200, 250));
        groups.addAssociation(new SolarEvent.Link(2, 3));
        check(group.getIntervals().equals(List.of(new Interval(100, 250))), "touching intervals merge");
        groups.addEvent(new SolarEvent(null, 4, 350, 350));
        groups.addAssociation(new SolarEvent.Link(3, 4));
        check(group.getIntervals().equals(List.of(new Interval(100, 250), new Interval(350, 350))), "point observation and gap survive merge");
        check(groups.getEvents(350, 350).contains(group), "point observation visible");
        check(groups.getEvents(251, 349).isEmpty(), "merged group preserves gap");
        groups.addEvent(new SolarEvent(null, 1, 400, 500));
        check(group.getIntervals().equals(List.of(new Interval(150, 175), new Interval(200, 250), new Interval(350, 350), new Interval(400, 500))), "replacement uncovers previously contained intervals");
        check(group.getStart() == 150 && group.getEnd() == 500, "replacement updates both bounds");
        check(groups.getEvents(100, 149).isEmpty(), "replacement reindexes start");
    }

    private static void checkRequestOwnership() throws Exception {
        EventQueue.invokeAndWait(() -> {
            SWEK.Source source = new SWEK.Source("test", List.of(), null, Map.of());
            SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Requests", ""), "requests", "requests", source, "requests", List.of());
            boolean[] expectedActive = {true};
            int[] notifications = new int[1];
            EventListener.Handle listener = () -> {
                check(SWEKDownloader.isSupplierActive(supplier) == expectedActive[0], "activation updated before cache notification");
                if (!expectedActive[0])
                    check(EventCache.getRelatedEvents(301) == null, "deactivation removes observations before notification");
                notifications[0]++;
            };
            EventCache.registerHandler(listener);
            try {
                check(!SWEKDownloader.isSupplierActive(supplier), "supplier initially inactive");
                SWEKDownloader.setSupplierActive(supplier, true);
                EventCache.replaceEvents(new EventBatch(1, List.of(new SolarEvent(supplier, 301, 100, 200)), List.of()));
                SWEKDownloader.requestForInterval(Long.MAX_VALUE - 1, Long.MAX_VALUE);
                check(!SWEKDownloader.isGroupBusy(supplier.group()), "future-only interval schedules no work");
                expectedActive[0] = false;
                SWEKDownloader.setSupplierActive(supplier, false);
                check(notifications[0] == 3, "activation, publication, and deactivation each notify once");
            } finally {
                EventCache.unregisterHandler(listener);
            }
        });
    }

    private static void checkRebuiltGroupOrder() {
        SWEK.Source source = new SWEK.Source("test", List.of(), null, Map.of());
        SWEKSupplier retained = new SWEKSupplier(null, "retained", "retained", source, "order-retained", List.of());
        SWEKSupplier removed = new SWEKSupplier(null, "removed", "removed", source, "order-removed", List.of());
        EventCollection groups = new EventCollection();
        for (int id = 1; id <= 7; id++)
            groups.addEvent(new SolarEvent(id == 7 ? removed : retained, id, 100, 200));
        for (SolarEvent.Link link : List.of(new SolarEvent.Link(1, 2), new SolarEvent.Link(2, 7), new SolarEvent.Link(7, 3),
                new SolarEvent.Link(7, 4), new SolarEvent.Link(4, 5), new SolarEvent.Link(5, 6)))
            groups.addAssociation(link);
        RelatedEvents original = groups.getRelatedEvents(1);
        groups.addEvent(new SolarEvent(retained, 9, 100, 200));
        groups.removeSupplier(removed);
        check(groups.getEvents(150, 150).equals(List.of(groups.getRelatedEvents(9), groups.getRelatedEvents(3),
                groups.getRelatedEvents(1), groups.getRelatedEvents(4))), "reconstruction preserves the order of groups sharing a start time");
        check(groups.getRelatedEvents(1).getClosestTo(150).getUniqueID() == 1 && groups.getRelatedEvents(4).getClosestTo(150).getUniqueID() == 4,
                "reconstruction preserves representative-event ties");
        check(groups.getRelatedEvents(3).getColor().equals(original.getColor()), "isolated survivor retains its color");
    }

    private static void checkVersionsSurviveRegrouping() {
        SWEK.Source source = new SWEK.Source("test", List.of(), null, Map.of());
        SWEKSupplier firstSupplier = new SWEKSupplier(null, "first", "first", source, "versions-first", List.of());
        SWEKSupplier secondSupplier = new SWEKSupplier(null, "second", "second", source, "versions-second", List.of());
        SolarEvent left = new SolarEvent(firstSupplier, 1, 100, 200), bridge = new SolarEvent(secondSupplier, 2, 300, 400);
        SolarEvent right = new SolarEvent(firstSupplier, 3, 500, 600);
        SolarEvent.Link firstLink = new SolarEvent.Link(1, 2), secondLink = new SolarEvent.Link(2, 3);
        EventCollection groups = new EventCollection();
        groups.replace(new EventBatch(10, List.of(left, bridge, right), List.of(firstLink, secondLink)));
        groups.replace(new EventBatch(20, List.of(right), List.of(secondLink)));
        groups.replace(new EventBatch(23, List.of(bridge), List.of(firstLink)));
        SolarEvent obsoleteRight = new SolarEvent(firstSupplier, 3, 0, 1000);
        groups.replace(new EventBatch(19, List.of(obsoleteRight), List.of(secondLink)));
        check(groups.getRelatedEvents(3).getClosestTo(500) == right, "split preserves the untouched observation version");
        check(groups.getRelatedEvents(2) != groups.getRelatedEvents(3), "obsolete link cannot reconnect split groups");
        groups.replace(new EventBatch(25, List.of(), List.of(secondLink)));
        groups.replace(new EventBatch(22, List.of(new SolarEvent(secondSupplier, 2, 0, 1000)), List.of(firstLink, secondLink)));
        check(groups.getRelatedEvents(2).getClosestTo(300) == bridge, "merge preserves the observation version");
        groups.removeSupplier(secondSupplier);
        groups.replace(new EventBatch(19, List.of(obsoleteRight), List.of()));
        check(groups.getRelatedEvents(3).getClosestTo(500) == right, "supplier removal preserves surviving versions");
        groups.replace(new EventBatch(1, List.of(bridge), List.of()));
        check(groups.getRelatedEvents(2).getClosestTo(300) == bridge, "supplier removal discards removed observation versions");
        groups.addEvent(right);
        groups.replace(new EventBatch(19, List.of(obsoleteRight), List.of()));
        check(groups.getRelatedEvents(3).getClosestTo(500) == right, "direct observation replacement preserves its published version");
    }

    private static void checkAssociationReplacement() {
        SolarEvent first = new SolarEvent(null, 1, 100, 200), bridge = new SolarEvent(null, 2, 300, 400);
        SolarEvent third = new SolarEvent(null, 3, 500, 600), fourth = new SolarEvent(null, 4, 700, 800);
        SolarEvent fifth = new SolarEvent(null, 5, 900, 1000);
        SolarEvent.Link firstLink = new SolarEvent.Link(1, 2), secondLink = new SolarEvent.Link(2, 3), separateLink = new SolarEvent.Link(4, 5);
        EventCollection groups = new EventCollection();
        groups.replace(new EventBatch(1, List.of(first, bridge, third, fourth, fifth), List.of(firstLink, secondLink, separateLink)));
        RelatedEvents original = groups.getRelatedEvents(1), separate = groups.getRelatedEvents(4);
        groups.replace(new EventBatch(3, List.of(bridge), List.of()));
        check(groups.getRelatedEvents(1) != groups.getRelatedEvents(2) && groups.getRelatedEvents(2) != groups.getRelatedEvents(3), "removed bridge links split the group");
        check(groups.getRelatedEvents(1).getColor().equals(original.getColor()), "split retains color");
        check(groups.getRelatedEvents(4) == separate && groups.getRelatedEvents(5) == separate, "other interval stays untouched");
        check(groups.getEvents(300, 400).equals(List.of(groups.getRelatedEvents(2))), "split observation remains visible");
        groups.replace(new EventBatch(2, List.of(first, new SolarEvent(null, 2, 0, 1000)), List.of(firstLink)));
        check(groups.getRelatedEvents(1) != groups.getRelatedEvents(2), "older batch cannot restore removed link");
        check(groups.getRelatedEvents(2).getClosestTo(300) == bridge, "older batch cannot overwrite observation");
        groups.replace(new EventBatch(4, List.of(first, bridge), List.of(firstLink)));
        check(groups.getRelatedEvents(1) == groups.getRelatedEvents(2), "newer batch can restore link");
        groups.replace(new EventBatch(6, List.of(bridge), List.of()));
        groups.replace(new EventBatch(5, List.of(fourth, fifth), List.of()));
        check(groups.getRelatedEvents(4) != groups.getRelatedEvents(5), "older batch still refreshes independent observations");

        EventCollection pending = new EventCollection();
        pending.replace(new EventBatch(5, List.of(), List.of(firstLink)));
        pending.replace(new EventBatch(4, List.of(first), List.of()));
        pending.replace(new EventBatch(6, List.of(bridge), List.of(firstLink)));
        check(pending.getRelatedEvents(1) == pending.getRelatedEvents(2), "older endpoint batch preserves newer pending link");
        EventCollection removedPending = new EventCollection();
        removedPending.replace(new EventBatch(1, List.of(first), List.of(firstLink)));
        removedPending.replace(new EventBatch(3, List.of(first), List.of()));
        removedPending.replace(new EventBatch(2, List.of(bridge), List.of(firstLink)));
        check(removedPending.getRelatedEvents(1) != removedPending.getRelatedEvents(2), "obsolete pending link stays removed when endpoint arrives late");

        SolarEvent left = new SolarEvent(null, 201, 100, 200), right = new SolarEvent(null, 202, 300, 400);
        EventCache.replaceEvents(new EventBatch(1, List.of(left, right), List.of(new SolarEvent.Link(201, 202))));
        RelatedEvents highlighted = EventCache.getRelatedEvents(201);
        EventCache.highlight(highlighted);
        int[] notifications = new int[1];
        EventListener.Handle listener = () -> {
            check(!highlighted.isHighlighted(), "split clears detached highlight before cache notification");
            check(EventCache.getRelatedEvents(201) != EventCache.getRelatedEvents(202), "notification sees finished split");
            notifications[0]++;
        };
        EventCache.registerHandler(listener);
        EventCache.replaceEvents(new EventBatch(2, List.of(left), List.of()));
        EventCache.unregisterHandler(listener);
        check(notifications[0] == 1, "batch publishes one cache notification");
        EventCache.removeSupplier(null);
    }

    private static void checkPendingLinkRemoval() {
        EventCollection groups = new EventCollection();
        SolarEvent first = new SolarEvent(null, 1, 100, 200);
        SolarEvent second = new SolarEvent(null, 2, 100, 200);
        SolarEvent third = new SolarEvent(null, 3, 100, 200);
        SolarEvent fourth = new SolarEvent(null, 4, 100, 200);
        SolarEvent fifth = new SolarEvent(null, 5, 100, 200);
        SolarEvent sixth = new SolarEvent(null, 6, 100, 200);
        groups.replace(new EventBatch(1, List.of(), List.of(new SolarEvent.Link(1, 2), new SolarEvent.Link(2, 1),
                new SolarEvent.Link(2, 3), new SolarEvent.Link(4, 5), new SolarEvent.Link(6, 6))));
        groups.replace(new EventBatch(2, List.of(first, sixth), List.of()));
        groups.addEvent(second);
        groups.addEvent(third);
        groups.addEvent(fourth);
        groups.addEvent(fifth);
        check(groups.getRelatedEvents(1) != groups.getRelatedEvents(2), "removed links in both orientations cannot reconnect arriving endpoints");
        check(groups.getRelatedEvents(2) == groups.getRelatedEvents(3), "retained pending link at a shared endpoint still resolves");
        check(groups.getRelatedEvents(4) == groups.getRelatedEvents(5), "unrelated pending link still resolves");
        check(groups.getRelatedEvents(6).getAssociatedEvents(sixth).isEmpty(), "removed pending self-link stays removed");
    }

    private static void checkRemovalNotifications() {
        DisplayController.setRenderRequestHandler(_ -> {});
        SWEK.Source source = new SWEK.Source("test", List.of(), null, Map.of());
        SWEKSupplier supplier = new SWEKSupplier(null, "notifications", "notifications", source, "notifications", List.of());
        SolarEvent event = new SolarEvent(supplier, 100, 100, 200);
        EventCache.replaceEvents(new EventBatch(1, List.of(event), List.of()));
        RelatedEvents group = EventCache.getRelatedEvents(100);
        EventCache.highlight(group);
        int[] notifications = new int[2];
        EventListener.Highlight highlightListener = () -> {
            check(!group.isHighlighted(), "highlight cleared before notification");
            check(EventCache.getRelatedEvents(100) == group, "highlight notification precedes group removal");
            notifications[0]++;
        };
        EventListener.Handle cacheListener = () -> {
            check(EventCache.getRelatedEvents(100) == null, "cache notification follows group removal");
            notifications[1]++;
        };
        EventCache.addHighlightListener(highlightListener);
        EventCache.registerHandler(cacheListener);
        EventCache.removeSupplier(supplier);
        EventCache.removeHighlightListener(highlightListener);
        EventCache.unregisterHandler(cacheListener);
        check(notifications[0] == 1 && notifications[1] == 1, "one notification of each kind");

        RelatedEvents detached = new RelatedEvents(event);
        EventCache.highlight(detached);
        EventCache.replaceEvents(new EventBatch(1, List.of(event), List.of()));
        EventCache.removeSupplier(supplier);
        check(detached.isHighlighted(), "detached detail group is not mistaken for cached group");
        EventCache.highlight(null);
    }

    private static void checkGroupOwnership() {
        SWEK.Source source = new SWEK.Source("test", List.of(), null, Map.of());
        SWEKSupplier firstSupplier = new SWEKSupplier(null, "first", "first", source, "first", List.of());
        SWEKSupplier secondSupplier = new SWEKSupplier(null, "second", "second", source, "second", List.of());
        SolarEvent first = new SolarEvent(firstSupplier, 1, 100, 200);
        SolarEvent bridge = new SolarEvent(secondSupplier, 2, 300, 400);
        SolarEvent third = new SolarEvent(firstSupplier, 3, 500, 600);
        SolarEvent fourth = new SolarEvent(firstSupplier, 4, 700, 800);
        EventCollection groups = new EventCollection();
        groups.addAssociation(new SolarEvent.Link(1, 2));
        groups.addAssociation(new SolarEvent.Link(2, 3));
        groups.addEvent(first);
        groups.addEvent(third);
        check(groups.getRelatedEvents(1) != groups.getRelatedEvents(3), "missing bridge does not merge endpoints");
        groups.addEvent(bridge);
        RelatedEvents merged = groups.getRelatedEvents(1);
        check(merged == groups.getRelatedEvents(3), "pending links resolve through bridge");
        groups.addAssociation(new SolarEvent.Link(1, 2));
        check(merged.getAssociatedEvents(first).equals(List.of(bridge)), "duplicate link stays unique and direct");
        groups.addEvent(fourth);
        groups.addAssociation(new SolarEvent.Link(3, 4));
        groups.addAssociation(new SolarEvent.Link(2, 5));

        EventCollection independent = new EventCollection();
        independent.addEvent(first);
        check(independent.getRelatedEvents(2) == null && independent.getRelatedEvents(1) != merged, "collections have independent membership");

        groups.removeSupplier(secondSupplier);
        check(groups.getRelatedEvents(2) == null, "supplier observation removed");
        check(groups.getRelatedEvents(1) != groups.getRelatedEvents(3), "removing bridge splits group");
        check(groups.getRelatedEvents(3) == groups.getRelatedEvents(4), "surviving association retained");
        check(groups.getRelatedEvents(1).getColor().equals(merged.getColor()), "split preserves color");
        check(groups.getEvents(300, 400).isEmpty(), "removed bridge absent from time index");
        groups.addEvent(new SolarEvent(firstSupplier, 5, 900, 1000));
        groups.addEvent(bridge);
        check(groups.getRelatedEvents(2) != groups.getRelatedEvents(5), "removed pending link cannot reconnect reloaded observation");
        groups.removeSupplier(firstSupplier);
        check(groups.getEvents(0, 1000).equals(List.of(groups.getRelatedEvents(2))), "removal leaves only the other supplier");
        check(independent.getRelatedEvents(1).getClosestTo(100) == first, "removal does not affect another collection");
    }

    private static void checkDownloads() throws Exception {
        Files.createDirectories(Path.of(Directories.CACHE.getPath()));
        SWEKGroup group = new SWEKGroup("Requests", "");
        ControlledHandler retryHandler = new ControlledHandler(false);
        SWEKSupplier retry = supplier(group, "retry", retryHandler);
        long start = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3), end = start + 1;
        EventQueue.invokeAndWait(() -> SWEKDownloader.setSupplierActive(retry, true));
        await(request(group, start, end), "failed request finishes");
        check(retryHandler.calls.get() == 1, "one padded request");
        await(request(group, start, end), "failed interval can be retried");
        check(retryHandler.calls.get() == 2, "retry fetched the failed interval");
        EventQueue.invokeAndWait(() -> {
            SWEKDownloader.requestForInterval(start, end);
            check(!SWEKDownloader.isGroupBusy(group), "completed interval is not scheduled again");
            SWEKDownloader.setSupplierActive(retry, false);
        });
        check(retryHandler.calls.get() == 2, "completed request remains covered");

        ControlledHandler delayedHandler = new ControlledHandler(true);
        SWEKSupplier delayed = supplier(group, "delayed", delayedHandler);
        EventQueue.invokeAndWait(() -> SWEKDownloader.setSupplierActive(delayed, true));
        request(group, start, end);
        await(delayedHandler.entered, "first worker is fetching");
        EventQueue.invokeAndWait(() -> {
            check(SWEKDownloader.isGroupBusy(group), "blocked worker marks group busy");
            SWEKDownloader.setSupplierActive(delayed, false);
            check(!SWEKDownloader.isGroupBusy(group), "cancelled supplier is no longer busy");
            SWEKDownloader.setSupplierActive(delayed, true);
        });
        delayedHandler.release.countDown();
        await(request(group, start, end), "reactivated supplier finishes new request");
        EventQueue.invokeAndWait(() -> {
            check(SWEKDownloader.isSupplierActive(delayed), "old worker does not deactivate new state");
            SWEKDownloader.requestForInterval(start, end);
            check(!SWEKDownloader.isGroupBusy(group), "new state retains completed coverage");
            SWEKDownloader.setSupplierActive(delayed, false);
            SWEKDownloader.clearGroupChangedCallback();
        });
        check(delayedHandler.calls.get() == 2, "reactivation creates exactly one replacement request");
        checkPublicationFailure(group, start, end);
        checkSupplierCancellation(group);
        checkPagination(group, start, end);
    }

    private static void checkPagination(SWEKGroup group, long start, long end) throws Exception {
        PaginatedHandler handler = new PaginatedHandler();
        SWEKSupplier supplier = supplier(group, "paginated", handler);
        EventQueue.invokeAndWait(() -> SWEKDownloader.setSupplierActive(supplier, true));
        try {
            await(request(group, start, end), "second page failure finishes request");
            check(handler.pages.equals(List.of(0, 1)), "downloader advances to the second page");
            check(!EventDatabase.isStored(start, end, supplier), "failed second page must not complete interval coverage");
            List<SolarEvent> partial = EventDatabase.loadEvents(start, end, supplier, List.of()).events();
            check(partial.size() == 1, "first page was stored before the second page failed");
            int firstId = partial.getFirst().getUniqueID();

            await(request(group, start, end), "retry completes both pages");
            check(handler.pages.equals(List.of(0, 1, 0, 1)), "retry restarts pagination and stops at the final page");
            check(EventDatabase.isStored(start, end, supplier), "successful final page completes interval coverage");
            List<SolarEvent> loaded = EventDatabase.loadEvents(start, end, supplier, List.of()).events();
            check(loaded.size() == 2, "retry retains both events without duplicating the first page");
            check(loaded.stream().filter(event -> event.getUniqueID() == firstId).count() == 1, "retried first page keeps its database identity");
            EventQueue.invokeAndWait(() -> {
                for (SolarEvent event : loaded)
                    check(EventCache.getRelatedEvents(event.getUniqueID()) != null, "completed download publishes every page");
                SWEKDownloader.requestForInterval(start, end);
                check(!SWEKDownloader.isGroupBusy(group), "completed pages are not scheduled again");
            });
            check(handler.pages.size() == 4, "completed interval requires no further fetches");
        } finally {
            EventQueue.invokeAndWait(() -> {
                SWEKDownloader.setSupplierActive(supplier, false);
                SWEKDownloader.clearGroupChangedCallback();
            });
        }
    }

    private static final class PaginatedHandler extends SWEKHandler {
        private final List<Integer> pages = new ArrayList<>();
        private final byte[] json = JSONUtils.compressJSON(new JSONObject()).toByteArray();

        PaginatedHandler() throws Exception {}

        @Override
        RemotePage fetchPage(SWEKSupplier supplier, long start, long end, int page) {
            pages.add(page);
            check(page == 0 || page == 1, "only two pages exist");
            if (pages.size() == 2)
                throw new IllegalStateException("deliberate second page failure");
            RemoteEvent event = new RemoteEvent(json, start, end, start, "page-" + page, Map.of());
            return new RemotePage(page == 0, List.of(event), List.of());
        }

        @Override
        protected URI createURI(SWEKSupplier supplier, long start, long end, int page) {
            throw new AssertionError("controlled pagination does not use the network");
        }

        @Override
        protected RemotePage parseRemotePage(JSONObject json, SWEKSupplier supplier) {
            throw new AssertionError("controlled pagination supplies pages directly");
        }

        @Override
        public SolarEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) {
            return new SolarEvent(supplier, id, start, end);
        }
    }

    private static void checkSupplierCancellation(SWEKGroup group) throws Exception {
        ControlledHandler handler = new ControlledHandler(2);
        SWEKSupplier supplier = supplier(group, "multiple", handler);
        long end = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), start = end - TimeUnit.DAYS.toMillis(1);
        EventQueue.invokeAndWait(() -> SWEKDownloader.setSupplierActive(supplier, true));
        request(group, start, end);
        await(handler.entered, "both interval workers are fetching");
        EventQueue.invokeAndWait(() -> {
            SWEKDownloader.setSupplierActive(supplier, false);
            check(!SWEKDownloader.isGroupBusy(group), "cancellation clears all workers from busy state");
            SWEKDownloader.setSupplierActive(supplier, true);
        });
        handler.release.countDown();
        await(request(group, start, end), "replacement interval workers finish");
        check(handler.calls.get() == 4, "both cancelled intervals are fetched by the new state");
        EventQueue.invokeAndWait(() -> {
            SWEKDownloader.requestForInterval(start, end);
            check(!SWEKDownloader.isGroupBusy(group), "replacement workers retain interval coverage");
            SWEKDownloader.setSupplierActive(supplier, false);
            SWEKDownloader.clearGroupChangedCallback();
        });
    }

    private static void checkPublicationFailure(SWEKGroup group, long start, long end) throws Exception {
        ControlledHandler handler = new ControlledHandler(false, false);
        SWEKSupplier supplier = supplier(group, "publication", handler);
        IllegalStateException failure = new IllegalStateException("deliberate cache listener failure");
        CountDownLatch reported = new CountDownLatch(1);
        Thread.UncaughtExceptionHandler[] previous = new Thread.UncaughtExceptionHandler[1];
        EventListener.Handle listener = () -> { throw failure; };
        EventQueue.invokeAndWait(() -> {
            SWEKDownloader.setSupplierActive(supplier, true);
            previous[0] = Thread.currentThread().getUncaughtExceptionHandler();
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
                if (error == failure) reported.countDown();
                else previous[0].uncaughtException(thread, error);
            });
            EventCache.registerHandler(listener);
        });
        try {
            CountDownLatch idle = request(group, start, end);
            await(reported, "publication error remains observable");
            await(idle, "publication failure still clears busy state");
            EventQueue.invokeAndWait(() -> {
                SWEKDownloader.requestForInterval(start, end);
                check(!SWEKDownloader.isGroupBusy(group), "listener failure does not invalidate successfully loaded interval");
            });
        } finally {
            EventQueue.invokeAndWait(() -> {
                EventCache.unregisterHandler(listener);
                Thread.currentThread().setUncaughtExceptionHandler(previous[0]);
                SWEKDownloader.setSupplierActive(supplier, false);
                SWEKDownloader.clearGroupChangedCallback();
            });
        }
    }

    private static SWEKSupplier supplier(SWEKGroup group, String name, SWEKHandler handler) {
        SWEKSupplier supplier = new SWEKSupplier(group, name, name, new SWEK.Source("test", List.of(), handler, Map.of()), name, List.of());
        SWEKCatalog.add(supplier);
        SWEKCatalog.setRelations(List.of());
        return supplier;
    }

    private static CountDownLatch request(SWEKGroup group, long start, long end) throws Exception {
        CountDownLatch idle = new CountDownLatch(1);
        EventQueue.invokeAndWait(() -> {
            SWEKDownloader.setGroupChangedCallback(changed -> {
                if (changed == group && !SWEKDownloader.isGroupBusy(group)) idle.countDown();
            });
            SWEKDownloader.requestForInterval(start, end);
        });
        return idle;
    }

    private static final class ControlledHandler extends SWEKHandler {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch entered, release;
        private final boolean failFirst;
        private final int blockedCalls;

        ControlledHandler(boolean pause) {
            this(pause, true);
        }

        ControlledHandler(boolean pause, boolean _failFirst) {
            entered = new CountDownLatch(1);
            release = new CountDownLatch(pause ? 1 : 0);
            failFirst = _failFirst;
            blockedCalls = 1;
        }

        ControlledHandler(int _blockedCalls) {
            entered = new CountDownLatch(_blockedCalls);
            release = new CountDownLatch(1);
            failFirst = false;
            blockedCalls = _blockedCalls;
        }

        @Override
        RemotePage fetchPage(SWEKSupplier supplier, long start, long end, int page) throws Exception {
            int call = calls.incrementAndGet();
            if (call <= blockedCalls) {
                entered.countDown();
                await(release, "release controlled fetch");
                if (call == 1 && failFirst) throw new IllegalStateException("deliberate request failure");
            }
            return new RemotePage(false, List.of(), List.of());
        }

        @Override
        protected URI createURI(SWEKSupplier supplier, long start, long end, int page) {
            throw new AssertionError("controlled fetch does not use a network URI");
        }

        @Override
        protected RemotePage parseRemotePage(JSONObject json, SWEKSupplier supplier) {
            throw new AssertionError("controlled fetch supplies its page directly");
        }

        @Override
        public SolarEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) {
            throw new AssertionError("empty page has no observations to decode");
        }
    }

    private static void await(CountDownLatch latch, String message) throws Exception {
        check(latch.await(5, TimeUnit.SECONDS), message);
    }

    private static void checkIntervals() {
        check(merge().isEmpty(), "empty coverage");
        check(merge(new Interval(5, 5)).equals(List.of(new Interval(5, 5))), "point interval survives");
        check(merge(new Interval(20, 30), new Interval(0, 10), new Interval(10, 20), new Interval(5, 15), new Interval(0, 10))
                .equals(List.of(new Interval(0, 30))), "unsorted, touching, overlapping and duplicate intervals form one union");
        check(merge(new Interval(10, 20), new Interval(-10, -5), new Interval(0, 0), new Interval(11, 12))
                .equals(List.of(new Interval(-10, -5), new Interval(0, 0), new Interval(10, 20))), "gaps and isolated points survive nested intervals");
        check(merge(new Interval(0, Long.MAX_VALUE), new Interval(Long.MIN_VALUE, 0))
                .equals(List.of(new Interval(Long.MIN_VALUE, Long.MAX_VALUE))), "endpoint merging does not require arithmetic that can overflow");
        RequestCache cache = new RequestCache();
        cache.adaptRequestCache(10, 20);
        cache.adaptRequestCache(20, 30);
        check(cache.getAllRequestIntervals().equals(List.of(new Interval(10, 30))), "request cache merges touching coverage");
        cache.removeRequestedInterval(15, 25);
        check(cache.getAllRequestIntervals().equals(List.of(new Interval(10, 15), new Interval(25, 30))), "subtraction preserves the remaining coverage");
        check(cache.getMissingIntervals(10, 30).equals(List.of(new Interval(15, 25))), "removed coverage is requestable again");
    }

    private static List<Interval> merge(Interval... intervals) {
        return Interval.merge(new ArrayList<>(List.of(intervals)));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
