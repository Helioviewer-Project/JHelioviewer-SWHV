package org.helioviewer.jhv.plugins.swek;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.event.EventInformationFixture;
import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.ui.Panel;

// The shared information screen against the real event database: one summary, its details arriving, and the input of either toolkit.
public final class EventInformationScreenTest {

    private static final int STANDARD = 1;
    private static final int ALL = 2;
    private static final int RELATED = 3;
    private static final int ROW = 100;
    private static final int SECTION_ALL = 2;
    private static final int HOVER = 1;
    private static final int DETAILS = 2;
    private static final String NOAA_URL = "https://ui.adsabs.harvard.edu/#search/q=%22NOAA%20" + EventInformationFixture.REGION + "%22&sort=date%20desc";

    private record Row(int item, String name, String value, String url, boolean selected) {
        // The value as the Swing table cell holds it.
        String cell() {
            return url == null ? value : "<a href=\"" + url + "\">" + value + "</a>";
        }
    }

    private record Opened(RelatedEvents related, SolarEvent event) {}

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        summaryThenDetails();
        sortMatchesSwing();
        linksFollowTheirRows();
        hoverHighlights();
        detailsOpenEachEvent();
        lateResultsAfterClose();
        failedDetailsKeepTheSummary();
        System.out.println("EventInformationScreenTest passed");
    }

    private static EventInformationScreen loaded(EventInformationFixture.Open open, EventInformationScreen.Details details) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        EventInformationScreen[] screen = new EventInformationScreen[1];
        EventQueue.invokeAndWait(() -> {
            screen[0] = new EventInformationScreen(open.session(), open.related(), open.event(), details);
            screen[0].listen(done::countDown);
        });
        check(done.await(10, TimeUnit.SECONDS), "Details never arrived");
        return screen[0];
    }

    private static void summaryThenDetails() throws Exception {
        EventInformationFixture.Open open = EventInformationFixture.open();
        CountDownLatch done = new CountDownLatch(1);
        EventInformationScreen[] held = new EventInformationScreen[1];
        int[] sortItem = new int[1];
        EventQueue.invokeAndWait(() -> {
            // The result cannot land while this task holds the event queue.
            EventInformationScreen screen = new EventInformationScreen(open.session(), open.related(), open.event(), (_, _) -> {});
            held[0] = screen;
            screen.listen(done::countDown);
            Panel summary = screen.build();
            check(screen.title().equals("Solar Flare"), "Title: " + screen.title());
            check(summary.value(0, Panel.IntField.FIT) == 1, "A new window fits once");
            check(sectionOpen(summary, "Standard Parameters") == Boolean.TRUE && sectionOpen(summary, "All Parameters") == Boolean.FALSE
                    && sectionOpen(summary, "Related Events") == Boolean.FALSE, "Initial sections");
            check(sectionOpen(summary, "Other Related Events") == null, "Other related events before their query");
            screen.checked(0, SECTION_ALL, true);
            check(screen.build().value(0, Panel.IntField.FIT) == 2, "A toggled section fits again");
            sortItem[0] = header(screen.build(), ALL, "Value");
            screen.command(ALL, sortItem[0], Panel.Command.CLICK);
            screen.command(STANDARD, ROW, Panel.Command.SELECT_ONLY);
            check(rows(screen.build(), STANDARD).stream().anyMatch(Row::selected), "A selection was lost before the details");
        });
        check(done.await(10, TimeUnit.SECONDS), "Details never arrived");
        Panel details = held[0].build();
        check(details.value(0, Panel.IntField.FIT) == 3, "Details fit again");
        check(sectionOpen(details, "All Parameters") == Boolean.TRUE, "Details closed a section the user opened");
        check(sectionOpen(details, "Other Related Events") == Boolean.FALSE, "Other related events are missing");
        check(rows(details, ALL).stream().anyMatch(row -> row.name.equals("obs instrument")), "The stored event's parameters are missing");
        check(rows(details, STANDARD).stream().noneMatch(Row::selected), "Replaced rows kept a selection");
        check(sorted(details, ALL, 1) == 1, "Details dropped the user's sort");
        held[0].close();
        open.session().close();
    }

    // The order of DefaultRowSorter over the same cells, through a run of header clicks.
    private static void sortMatchesSwing() throws Exception {
        EventInformationFixture.Open open = EventInformationFixture.open();
        EventInformationScreen screen = loaded(open, (_, _) -> {});
        List<Row> stored = new ArrayList<>(rows(screen.build(), ALL));
        stored.sort((a, b) -> a.item - b.item);
        Object[][] cells = new Object[stored.size()][];
        for (int i = 0; i < cells.length; i++)
            cells[i] = new Object[]{stored.get(i).name, stored.get(i).cell()};
        TableModel model = new DefaultTableModel(cells, new Object[]{"Parameter Name", "Value"});
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        sorter.toggleSortOrder(0);
        int[] columns = {0, 1, 1, 0, 0, 1, 0, 1, 1, 1};
        for (int click = -1; click < columns.length; click++) {
            if (click >= 0) {
                sorter.toggleSortOrder(columns[click]);
                screen.command(ALL, header(screen.build(), ALL, columns[click] == 0 ? "Parameter Name" : "Value"), Panel.Command.CLICK);
            }
            List<Row> shown = rows(screen.build(), ALL);
            for (int row = 0; row < shown.size(); row++) {
                check(shown.get(row).item - ROW == sorter.convertRowIndexToModel(row), "Sort differs from Swing after click " + click + " at row " + row);
            }
        }
        screen.close();
        open.session().close();
    }

    // Each link and each selection stays with its parameter as the order changes.
    private static void linksFollowTheirRows() throws Exception {
        EventInformationFixture.Open open = EventInformationFixture.open();
        EventInformationScreen screen = loaded(open, (_, _) -> {});
        Row method = row(screen.build(), STANDARD, "Method URL");
        screen.command(STANDARD, method.item, Panel.Command.SELECT_ONLY);
        for (int click = 0; click < 3; click++) {
            Panel p = screen.build();
            Row url = row(p, STANDARD, "Method URL");
            Row region = row(p, STANDARD, "NOAA Region");
            check(url.value.equals("Open URL") && EventInformationFixture.REFERENCE_URL.equals(url.url) && url.selected, "Reference link row: " + url);
            check(region.value.equals(String.valueOf(EventInformationFixture.REGION)) && NOAA_URL.equals(region.url) && !region.selected, "Region link row: " + region);
            check(row(p, STANDARD, "Description").value.length() <= 259, "Long values are cut");
            screen.command(STANDARD, header(p, STANDARD, "Value"), Panel.Command.CLICK);
        }
        screen.command(STANDARD, method.item, Panel.Command.SELECT_ADD);
        Row description = row(screen.build(), STANDARD, "Description");
        screen.command(STANDARD, description.item, Panel.Command.SELECT_ADD);
        check(rows(screen.build(), STANDARD).stream().filter(Row::selected).count() == 2, "Adding to a selection");
        screen.command(STANDARD, method.item, Panel.Command.SELECT_REMOVE);
        check(rows(screen.build(), STANDARD).stream().filter(Row::selected).toList().equals(List.of(row(screen.build(), STANDARD, "Description"))), "Removing from a selection");
        screen.close();
        open.session().close();
    }

    private static void hoverHighlights() throws Exception {
        EventInformationFixture.Open open = EventInformationFixture.open();
        List<Opened> opened = new ArrayList<>();
        EventInformationScreen screen = loaded(open, (related, event) -> opened.add(new Opened(related, event)));
        screen.command(0, HOVER, Panel.Command.ENTER);
        check(open.session().isHighlighted(open.related()), "Hover did not highlight the event");
        screen.command(0, HOVER, Panel.Command.EXIT);
        check(!open.session().isHighlighted(open.related()), "Leaving kept the highlight");
        // A host may report the next row before the last.
        screen.command(RELATED, HOVER, Panel.Command.ENTER);
        screen.command(0, HOVER, Panel.Command.ENTER);
        screen.command(RELATED, HOVER, Panel.Command.EXIT);
        check(open.session().isHighlighted(open.related()), "Leaving one row cleared the next");
        screen.checked(0, 3, false);
        check(open.session().isHighlighted(open.related()), "Collapsing related events cleared the main summary's highlight");
        screen.checked(0, 3, true);
        screen.command(RELATED, HOVER, Panel.Command.ENTER);
        screen.checked(0, 3, false);
        check(!open.session().isHighlighted(open.related()), "A collapsed section kept its highlight without a mouse exit");
        screen.command(0, HOVER, Panel.Command.ENTER);
        screen.command(RELATED, HOVER, Panel.Command.EXIT);
        check(open.session().isHighlighted(open.related()), "A late exit from a collapsed section cleared the next highlight");
        int other = RELATED + open.related().getAssociatedEvents(open.event()).size();
        screen.command(other, DETAILS, Panel.Command.CLICK);
        RelatedEvents otherGroup = opened.getFirst().related;
        screen.checked(0, 4, true);
        screen.command(other, HOVER, Panel.Command.ENTER);
        check(open.session().isHighlighted(otherGroup), "Other related event did not highlight");
        screen.checked(0, 3, false);
        check(open.session().isHighlighted(otherGroup), "Collapsing related events cleared another section's highlight");
        screen.checked(0, 4, false);
        check(!open.session().isHighlighted(otherGroup), "Other related events kept a highlight after collapsing");
        screen.command(0, HOVER, Panel.Command.ENTER);
        screen.close();
        check(!open.session().isHighlighted(open.related()), "A closed window kept its highlight");
        open.session().close();
    }

    private static void detailsOpenEachEvent() throws Exception {
        EventInformationFixture.Open open = EventInformationFixture.open();
        List<Opened> opened = new ArrayList<>();
        EventInformationScreen screen = loaded(open, (related, event) -> opened.add(new Opened(related, event)));
        List<SolarEvent> associated = open.related().getAssociatedEvents(open.event());
        Panel p = screen.build();
        List<Integer> owners = new ArrayList<>();
        List<Integer> bars = new ArrayList<>();
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.BUTTON && "Details".equals(p.string(i, Panel.IntField.LABEL)))
                owners.add(p.value(i, Panel.IntField.OWNER));
            if (p.kind(i) == Panel.Kind.SPACE && p.value(i, Panel.IntField.FOREGROUND) != 0)
                bars.add(p.value(i, Panel.IntField.FOREGROUND));
        }
        check(owners.size() == associated.size() + 2, "Listed events: " + owners);
        for (int owner : owners)
            screen.command(owner, DETAILS, Panel.Command.CLICK);
        check(opened.getFirst().related == open.related() && opened.getFirst().event == associated.getFirst(), "Details of a related event");
        Opened published = opened.get(associated.size());
        Opened unpublished = opened.get(associated.size() + 1);
        check(published.related == open.session().getRelatedEvents(published.event.getUniqueID()), "A shown event keeps its group");
        check(unpublished.related != published.related && open.session().getRelatedEvents(unpublished.event.getUniqueID()) == null, "An unshown event gets a group of its own");
        check(bars.size() == owners.size() + 1 && bars.getLast() == (0xFF000000 | unpublished.related.getColor().getRGB()), "Color bars: " + bars);
        screen.close();
        open.session().close();
    }

    // Hold the actual success or failure callback until the request's initial state is ready.
    private static void completeRequest(Runnable request) throws Exception {
        BlockingQueue<Runnable> callbacks = new LinkedBlockingQueue<>();
        UiThread.install(new UiThread.Impl() {
            @Override
            public void post(Runnable task) {
                callbacks.add(task);
            }

            @Override
            public boolean isUiThread() {
                return EventQueue.isDispatchThread();
            }
        });
        try {
            EventQueue.invokeAndWait(request);
            Runnable callback = callbacks.poll(10, TimeUnit.SECONDS);
            check(callback != null, "Details request never completed");
            EventQueue.invokeAndWait(callback);
        } finally {
            UiThread.install(new UiThread.Impl() {
                @Override
                public void post(Runnable task) {
                    EventQueue.invokeLater(task);
                }

                @Override
                public boolean isUiThread() {
                    return EventQueue.isDispatchThread();
                }
            });
        }
    }

    private static void lateResultsAfterClose() throws Exception {
        EventInformationFixture.Open open = EventInformationFixture.open();
        EventInformationScreen[] closed = new EventInformationScreen[1];
        int[] notified = {0};
        completeRequest(() -> {
            closed[0] = new EventInformationScreen(open.session(), open.related(), open.event(), (_, _) -> {});
            closed[0].listen(() -> notified[0]++);
            closed[0].close();
        });
        Panel p = closed[0].build();
        check(p.value(0, Panel.IntField.FIT) == 1 && sectionOpen(p, "Other Related Events") == null, "A closed window took late details");
        check(notified[0] == 0, "A closed window notified its view");
        open.session().close();
    }

    private static void failedDetailsKeepTheSummary() throws Exception {
        EventInformationFixture.Open open = EventInformationFixture.open();
        // Not in the database: the query fails.
        SolarEvent unknown = new SolarEvent(open.event().getSupplier(), 999_999, 1, 2);
        EventInformationScreen[] failing = new EventInformationScreen[1];
        boolean[] notified = {false};
        completeRequest(() -> {
            failing[0] = new EventInformationScreen(open.session(), new RelatedEvents(unknown), unknown, (_, _) -> {});
            failing[0].listen(() -> notified[0] = true);
        });
        check(!notified[0] && failing[0].build().value(0, Panel.IntField.FIT) == 1, "A failed query changed the window");
        check(sectionOpen(failing[0].build(), "Standard Parameters") == Boolean.TRUE, "A failed query lost the summary");
        failing[0].close();
        open.session().close();
        // A closed session cancels the query.
        EventInformationScreen[] canceled = new EventInformationScreen[1];
        completeRequest(() -> {
            canceled[0] = new EventInformationScreen(open.session(), open.related(), open.event(), (_, _) -> {});
            canceled[0].listen(() -> notified[0] = true);
        });
        check(!notified[0] && canceled[0].build().value(0, Panel.IntField.FIT) == 1, "A canceled query changed the window");
        canceled[0].close();
    }

    private static Boolean sectionOpen(Panel p, String title) {
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.SECTION && title.equals(p.string(i, Panel.IntField.LABEL)))
                return p.has(i, Panel.OPEN);
        }
        return null;
    }

    private static int header(Panel p, int owner, String name) {
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.BUTTON && p.value(i, Panel.IntField.OWNER) == owner && name.equals(p.string(i, Panel.IntField.LABEL)))
                return p.value(i, Panel.IntField.ID);
        }
        throw new AssertionError("No header " + name);
    }

    // The sort shown on a column's header button, zero if none.
    private static int sorted(Panel p, int owner, int column) {
        int seen = 0;
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.BUTTON && p.value(i, Panel.IntField.OWNER) == owner && seen++ == column)
                return p.value(i, Panel.IntField.INDEX);
        }
        throw new AssertionError("No column " + column);
    }

    private static List<Row> rows(Panel p, int owner) {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.ROW && p.value(i, Panel.IntField.OWNER) == owner && p.value(i, Panel.IntField.ID) >= ROW) {
                boolean link = p.kind(i + 2) == Panel.Kind.LINK;
                rows.add(new Row(p.value(i, Panel.IntField.ID), p.string(i + 1, Panel.IntField.LABEL), p.string(i + 2, Panel.IntField.LABEL),
                        link ? p.string(i + 2, Panel.IntField.TEXT) : null, p.has(i, Panel.SELECTED)));
            }
        }
        return rows;
    }

    private static Row row(Panel p, int owner, String name) {
        return rows(p, owner).stream().filter(row -> row.name.equals(name)).findFirst().orElseThrow(() -> new AssertionError("No row " + name));
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
