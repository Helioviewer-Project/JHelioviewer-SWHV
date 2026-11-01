package org.helioviewer.jhv.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.component.ChoicePicker;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.gui.time.TimeField;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.PunchClient;
import org.helioviewer.jhv.io.SoarClient;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.band.BandDataset;
import org.helioviewer.jhv.timelines.band.BandType;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jidesoft.swing.Searchable;
import com.jidesoft.swing.TreeSearchable;

// The five dataset dialogs through their shared definitions and real Swing widgets: requests, late replies,
// selection, time, confirmation and load, and every control's bounds against the original dialogs in swing-reference.tsv.
public final class DialogScreenTest {

    private static final int START = 1;
    private static final int END = 2;
    private static final int BACK = 5;
    private static final int BACK_UNIT = 6;
    private static final int FORE = 7;
    private static final int FORE_UNIT = 8;
    private static final int CARRINGTON = 9;
    private static final int FIRST_ROW = 1000;
    private static final long DAY = TimeUtils.DAY_IN_MILLIS;
    private static final long JAN_1 = 1735689600000L;

    static void require(boolean ok, String message) {
        if (!ok)
            throw new AssertionError(message);
    }

    private record Notice(String title, String text, Runnable action) {}

    private static final class Messages implements Message.Handler {
        final List<Notice> shown = new ArrayList<>();

        @Override
        public void err(String title, Object msg) {
            shown.add(new Notice(title, msg.toString(), null));
        }

        @Override
        public void warn(String title, Object msg) {
            shown.add(new Notice(title, msg.toString(), null));
        }

        @Override
        public void confirm(String title, Object msg, Runnable action) {
            shown.add(new Notice(title, msg.toString(), action));
        }

        @Override
        public void fatalErr(String msg) {
            throw new AssertionError(msg);
        }
    }

    private static final Messages messages = new Messages();

    private static final class Soar implements SoarScreen.Archive {
        final List<Consumer<List<String>>> soops = new ArrayList<>();
        final List<Consumer<List<SoarClient.DataItem>>> searches = new ArrayList<>();
        final List<Object[]> queries = new ArrayList<>();
        final List<List<SoarClient.DataItem>> loads = new ArrayList<>();

        @Override
        public void soops(Consumer<List<String>> reply) {
            soops.add(reply);
        }

        @Override
        public void search(Consumer<List<SoarClient.DataItem>> reply, List<String> descriptors, String level, long start, long end) {
            searches.add(reply);
            queries.add(new Object[]{descriptors, level, start, end});
        }

        @Override
        public void search(Consumer<List<SoarClient.DataItem>> reply, List<String> descriptors, String level, String soop) {
            searches.add(reply);
            queries.add(new Object[]{descriptors, level, soop});
        }

        @Override
        public void load(List<SoarClient.DataItem> items) {
            loads.add(items);
        }
    }

    private static final class Synoptic implements SynopticScreen.Archive {
        final List<Consumer<List<URI>>> replies = new ArrayList<>();
        final List<Runnable> failures = new ArrayList<>();
        final List<String> paths = new ArrayList<>();
        final List<JSONObject> queries = new ArrayList<>();
        final List<List<URI>> loads = new ArrayList<>();

        @Override
        public void search(String path, JSONObject query, Consumer<List<URI>> reply, Runnable failure) {
            paths.add(path);
            queries.add(query);
            replies.add(reply);
            failures.add(failure);
        }

        @Override
        public void load(List<URI> maps) {
            loads.add(maps);
        }
    }

    private static final class Aspiics implements AspiicsScreen.Archive {
        final List<Consumer<List<AspiicsScreen.Orbit>>> replies = new ArrayList<>();
        final List<APIRequest> loads = new ArrayList<>();

        @Override
        public void orbits(Consumer<List<AspiicsScreen.Orbit>> reply) {
            replies.add(reply);
        }

        @Override
        public void load(APIRequest request) {
            loads.add(request);
        }
    }

    private static final class Punch implements PunchScreen.Archive {
        final List<Consumer<List<String>>> products = new ArrayList<>();
        final List<String> levels = new ArrayList<>();
        final List<LongConsumer> coverage = new ArrayList<>();
        final List<String> covered = new ArrayList<>();
        final List<Consumer<List<PunchClient.DataItem>>> searches = new ArrayList<>();
        final List<Object[]> queries = new ArrayList<>();
        final List<List<PunchClient.DataItem>> loads = new ArrayList<>();

        @Override
        public void products(Consumer<List<String>> reply, String level) {
            products.add(reply);
            levels.add(level);
        }

        @Override
        public void coverage(LongConsumer reply, String level, String product) {
            coverage.add(reply);
            covered.add(level + product);
        }

        @Override
        public void search(Consumer<List<PunchClient.DataItem>> reply, String level, String product, long start, long end, long cadence) {
            searches.add(reply);
            queries.add(new Object[]{level, product, start, end, cadence});
        }

        @Override
        public void load(List<PunchClient.DataItem> items) {
            loads.add(items);
        }
    }

    // Controls by what a user sees.

    private static int control(Panel p, Panel.Kind kind, String label) {
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == kind && p.string(i, Panel.IntField.LABEL).equals(label))
                return i;
        }
        throw new AssertionError("No " + kind + " " + label);
    }

    private static int nthCombo(Panel p, int n) {
        for (int i = 0, seen = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.COMBO && p.string(i, Panel.IntField.LABEL).isEmpty() && seen++ == n)
                return i;
        }
        throw new AssertionError("No combo " + n);
    }

    private static void press(Screen screen, String label) {
        Panel p = screen.build();
        screen.command(0, p.value(control(p, Panel.Kind.BUTTON, label), Panel.IntField.ID), Panel.Command.CLICK);
    }

    private static void radio(Screen screen, String label) {
        Panel p = screen.build();
        int i = control(p, Panel.Kind.RADIO, label);
        screen.choice(0, p.value(i, Panel.IntField.ID), p.value(i, Panel.IntField.CHOICE));
    }

    private static void combo(Screen screen, int n, int value) {
        Panel p = screen.build();
        screen.choice(0, p.value(nthCombo(p, n), Panel.IntField.ID), value);
    }

    static boolean shows(Screen screen, String label) {
        Panel p = screen.build();
        for (int i = 0; i < p.size(); i++) {
            if (p.string(i, Panel.IntField.LABEL).equals(label))
                return true;
        }
        return false;
    }

    private static boolean enabled(Screen screen, Panel.Kind kind, String label) {
        Panel p = screen.build();
        return p.has(control(p, kind, label), Panel.ENABLED);
    }

    private static long startOf(Screen screen) {
        Panel p = screen.build();
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.TIME_FIELD)
                return (long) p.value(i, Panel.DoubleField.VALUE);
        }
        throw new AssertionError("No time field");
    }

    private static long endOf(Screen screen) {
        Panel p = screen.build();
        long end = 0;
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.TIME_FIELD)
                end = (long) p.value(i, Panel.DoubleField.VALUE);
        }
        return end;
    }

    private static void interval(Screen screen, long start, long end) {
        screen.number(0, START, start);
        screen.number(0, END, end);
    }

    private static final class Dismissed {
        int count;

        Dismissed(DialogScreen screen) {
            screen.onDismiss(() -> count++);
        }
    }

    // Data.

    private static SoarClient.DataItem soarItem(int i, long size) throws Exception {
        Constructor<?> constructor = SoarClient.DataItem.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object fits = constructor.getParameterTypes()[1].getEnumConstants()[0];
        return (SoarClient.DataItem) constructor.newInstance(String.format("solo_L2_eui-fsi174-image_2022030%dT00%02d00", i % 9 + 1, i), fits, size);
    }

    private static List<SoarClient.DataItem> soarItems(int n) throws Exception {
        List<SoarClient.DataItem> items = new ArrayList<>();
        for (int i = 0; i < n; i++)
            items.add(soarItem(i, 220_000_000L));
        return items;
    }

    private static List<URI> maps(int n) {
        List<URI> uris = new ArrayList<>();
        for (int i = 0; i < n; i++)
            uris.add(URI.create("https://idoc-ssa-prod.ias.u-psud.fr/data/synoptic/hmi_m_720s_2022030" + i + "_00.fits"));
        return uris;
    }

    private static List<AspiicsScreen.Orbit> orbits() {
        List<AspiicsScreen.Orbit> list = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            list.add(new AspiicsScreen.Orbit(120 - i, 1700000000000L - i * DAY, 1700020000000L - i * DAY));
        return list;
    }

    private static List<PunchClient.DataItem> punchItems(int n) {
        List<PunchClient.DataItem> items = new ArrayList<>();
        for (int i = 0; i < n; i++)
            items.add(new PunchClient.DataItem(String.format("PUNCH_L3_CAM_2025112000%02d00_v0c.fits", i), URI.create("https://example.invalid/" + i), 1763596800000L + i * 60000L));
        return items;
    }

    private static BandType band(String name, String group) throws Exception {
        Constructor<BandType> constructor = BandType.class.getDeclaredConstructor(JSONObject.class);
        constructor.setAccessible(true);
        JSONArray groups = group.isEmpty() ? new JSONArray() : new JSONArray().put(group);
        return constructor.newInstance(new JSONObject().put("name", name).put("label", name).put("groups", groups));
    }

    private static Map<String, BandDataset[]> catalogs() throws Exception {
        LinkedHashMap<String, BandDataset[]> catalogs = new LinkedHashMap<>();
        catalogs.put("GOES", new BandDataset[]{
                new BandDataset("XRS", List.of(band("XRS long", "GOES-16"), band("XRS short", "GOES-16"), band("XRS 2", "GOES-17"))),
                new BandDataset("Magnetometer", List.of(band("Hp", "")))});
        catalogs.put("OMNI", new BandDataset[]{new BandDataset("OMNI2", List.of(band("Bz", "")))});
        catalogs.put("Solar Orbiter", new BandDataset[]{new BandDataset("MAG", List.of(band("Br", "")))});
        return catalogs;
    }

    // The control streams every host draws; intervals are pinned through the same callbacks as the fields.
    public static Map<String, DialogScreen> fixtures() throws Exception {
        LinkedHashMap<String, DialogScreen> screens = new LinkedHashMap<>();
        SoarScreen soarEmpty = new SoarScreen(new Soar());
        interval(soarEmpty, JAN_1, JAN_1 + DAY);
        screens.put("soar-dialog", soarEmpty);
        SynopticScreen synopticEmpty = new SynopticScreen(new Synoptic());
        interval(synopticEmpty, JAN_1, JAN_1 + DAY);
        screens.put("synoptic-dialog", synopticEmpty);
        screens.put("aspiics-dialog", new AspiicsScreen(new Aspiics()));
        PunchScreen punchEmpty = new PunchScreen(new Punch());
        interval(punchEmpty, JAN_1, JAN_1 + DAY);
        screens.put("punch-dialog", punchEmpty);
        screens.put("timeline-dialog", new TimelineScreen("Timeline datasets are not loaded yet."));

        Soar soarArchive = new Soar();
        SoarScreen soar = new SoarScreen(soarArchive);
        interval(soar, JAN_1, JAN_1 + DAY);
        press(soar, "Search");
        soarArchive.searches.getLast().accept(soarItems(40));
        soar.command(0, FIRST_ROW + 1, Panel.Command.SELECT_ADD);
        screens.put("soar-loaded", soar);

        Synoptic synopticArchive = new Synoptic();
        SynopticScreen synoptic = new SynopticScreen(synopticArchive);
        interval(synoptic, JAN_1, JAN_1 + DAY);
        press(synoptic, "Search");
        synopticArchive.replies.getLast().accept(maps(12));
        synoptic.command(0, FIRST_ROW, Panel.Command.SELECT_ADD);
        screens.put("synoptic-loaded", synoptic);

        Aspiics aspiicsArchive = new Aspiics();
        AspiicsScreen aspiics = new AspiicsScreen(aspiicsArchive);
        aspiics.show();
        aspiicsArchive.replies.getLast().accept(orbits());
        screens.put("aspiics-loaded", aspiics);

        Punch punchArchive = new Punch();
        PunchScreen punch = new PunchScreen(punchArchive);
        punch.show();
        punchArchive.products.getLast().accept(List.of("CAM", "CTM", "PAM"));
        punchArchive.coverage.getLast().accept(1763596800000L);
        press(punch, "Search");
        punchArchive.searches.getLast().accept(punchItems(30));
        punch.command(0, FIRST_ROW + 2, Panel.Command.SELECT_ADD);
        screens.put("punch-loaded", punch);

        TimelineScreen timeline = new TimelineScreen(null, (types, full) -> {});
        timeline.setCatalogs(catalogs());
        screens.put("timeline-expanded", timeline);
        return screens;
    }

    // The shared definitions.

    private static void soarRequests() throws Exception {
        Soar archive = new Soar();
        SoarScreen screen = new SoarScreen(archive);
        Dismissed dismissed = new Dismissed(screen);
        screen.show();
        require(archive.soops.isEmpty(), "SOOP names load on the first switch, not at opening");
        require(enabled(screen, Panel.Kind.RADIO, "Time"), "Time radio enabled");
        Panel p = screen.build();
        require(!p.has(nthCombo(p, 0), Panel.ENABLED) && p.has(nthCombo(p, 1), Panel.ENABLED), "SOOP combo disabled in Time mode");
        require(p.value(nthCombo(p, 2), Panel.IntField.INDEX) == 1, "Level defaults to L2");

        interval(screen, JAN_1, JAN_1 + DAY);
        press(screen, "Search");
        Object[] query = archive.queries.getLast();
        require(query[0].equals(List.of("eui-fsi174-image", "eui-fsi174-image-occulter", "eui-fsi174-image-short")) && query[1].equals("L2")
                && query[2].equals(JAN_1) && query[3].equals(JAN_1 + DAY), "Time query arguments " + Arrays.toString(query));
        require(shows(screen, "Searching..."), "Search shows its state");
        require(enabled(screen, Panel.Kind.BUTTON, "Add"), "Add stays enabled and ignores an empty selection");
        press(screen, "Add");
        require(dismissed.count == 0 && archive.loads.isEmpty(), "Add without a selection does nothing");

        Consumer<List<SoarClient.DataItem>> stale = archive.searches.getLast();
        combo(screen, 2, 2);
        stale.accept(soarItems(3));
        require(shows(screen, "0 found"), "A changed level drops the running search");

        radio(screen, "SOOP");
        require(archive.soops.size() == 1, "The first switch to SOOP loads the names");
        p = screen.build();
        require(p.has(nthCombo(p, 0), Panel.ENABLED), "SOOP combo enabled in SOOP mode");
        for (int i = 0; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.ROW && i + 1 < p.size() && p.kind(i + 1) == Panel.Kind.SPACE)
                require(!p.has(i, Panel.ENABLED), "SOOP mode disables the time selector");
        }
        press(screen, "Search");
        require(archive.queries.size() == 1, "SOOP search needs the names");
        // A SOOP list arriving during a search does not touch it.
        archive.soops.getLast().accept(List.of("L_FULL", "R_SMALL"));
        combo(screen, 0, 1);
        press(screen, "Search");
        require(archive.queries.getLast()[2].equals("R_SMALL") && archive.queries.getLast()[1].equals("L3"), "SOOP query arguments");
        radio(screen, "SOOP");
        require(shows(screen, "Searching..."), "Choosing the current radio keeps the search");
        archive.searches.getLast().accept(soarItems(2));
        require(shows(screen, "2 found"), "SOOP result shown");

        press(screen, "Search");
        Consumer<List<SoarClient.DataItem>> closed = archive.searches.getLast();
        press(screen, "Cancel");
        require(dismissed.count == 1, "Cancel dismisses");
        screen.show();
        closed.accept(soarItems(5));
        require(!shows(screen, "5 found") && shows(screen, "0 found"), "A closed lifetime ignores its replies after reopening");
    }

    private static void soarSelection() throws Exception {
        Soar archive = new Soar();
        SoarScreen screen = new SoarScreen(archive);
        Dismissed dismissed = new Dismissed(screen);
        interval(screen, JAN_1, JAN_1 + DAY);
        press(screen, "Search");
        List<SoarClient.DataItem> items = soarItems(4);
        archive.searches.getLast().accept(items);
        screen.command(0, FIRST_ROW + 2, Panel.Command.SELECT_ADD);
        screen.command(0, FIRST_ROW, Panel.Command.SELECT_ADD);
        require(shows(screen, "2 selected (0.4GiB)"), "Selection label with size");

        screen.choice(0, BACK_UNIT, 1);
        screen.choice(0, FORE_UNIT, 2);
        require(shows(screen, "2 selected (0.4GiB)"), "Choosing a shift unit alone keeps the results");
        screen.setText(0, START, TimeUtils.formatShort(JAN_1));
        screen.number(0, START, JAN_1);
        require(shows(screen, "2 selected (0.4GiB)"), "An unchanged time commit keeps the results");
        screen.setText(0, START, "not a time");
        require(shows(screen, "2 selected (0.4GiB)") && startOf(screen) == JAN_1, "An invalid time commit keeps time and results");

        press(screen, "Add");
        require(archive.loads.getLast().equals(List.of(items.get(0), items.get(2))), "Load in listing order, not click order");
        require(dismissed.count == 1, "Add dismisses");

        press(screen, "Search");
        archive.searches.getLast().accept(items);
        screen.command(0, FIRST_ROW + 1, Panel.Command.SELECT_ONLY);
        screen.number(0, START, JAN_1 + 3600000);
        require(shows(screen, "0 found"), "A changed time clears the results");

        long size = 3L * 1024 * 1024 * 1024 / 2;
        press(screen, "Search");
        archive.searches.getLast().accept(List.of(soarItem(0, size), soarItem(1, size)));
        screen.command(0, FIRST_ROW, Panel.Command.SELECT_ADD);
        press(screen, "Add");
        require(archive.loads.getLast().size() == 1 && archive.loads.size() == 2, "A selection under 2GiB loads");
        press(screen, "Search");
        archive.searches.getLast().accept(List.of(soarItem(0, size), soarItem(1, size)));
        screen.command(0, FIRST_ROW, Panel.Command.SELECT_ADD);
        screen.command(0, FIRST_ROW + 1, Panel.Command.SELECT_ADD);
        messages.shown.clear();
        press(screen, "Add");
        require(archive.loads.size() == 2 && messages.shown.size() == 1, "Over 2GiB is refused");
        Notice notice = messages.shown.getFirst();
        require(notice.title.equals("SOAR error") && notice.text.equals(String.format("Too much data selected for download: 3.0GiB.%nPlease reduce the selection to less than 2.0GiB.")), "The original message: " + notice.text);
        require(dismissed.count == 2, "The window stays after a refusal");
    }

    private static void timeSelection() {
        Soar archive = new Soar();
        SoarScreen screen = new SoarScreen(archive);
        interval(screen, JAN_1, JAN_1 + DAY);
        screen.command(0, BACK, Panel.Command.CLICK);
        require(startOf(screen) == JAN_1 - DAY && endOf(screen) == JAN_1, "Back shifts by the default day");
        screen.choice(0, FORE_UNIT, 1);
        screen.command(0, FORE, Panel.Command.CLICK);
        require(startOf(screen) == JAN_1 + 6 * DAY && endOf(screen) == JAN_1 + 7 * DAY, "Forward shifts by its own unit");
        screen.choice(0, BACK_UNIT, 2);
        screen.command(0, BACK, Panel.Command.CLICK);
        long rotation = Math.round(Carrington.CR_SYNODIC_MEAN * DAY);
        require(startOf(screen) == TimeUtils.floorSec(JAN_1 + 6 * DAY - rotation), "Back shifts by a rotation");
        screen.choice(0, CARRINGTON, 300);
        long rotationStart = TimeUtils.floorSec(Carrington.CR_start[300]);
        require(startOf(screen) == rotationStart && endOf(screen) == TimeUtils.floorSec(Carrington.CR_start[301]), "A rotation selects its span");
        screen.number(0, END, rotationStart - DAY);
        require(endOf(screen) == rotationStart, "The end follows a later start");
        screen.number(0, START, TimeUtils.MAXIMAL_TIME.milli + DAY);
        require(startOf(screen) == rotationStart, "A start out of range keeps the field");
        Panel p = screen.build();
        int picker = control(p, Panel.Kind.COMBO, "CR");
        require(p.choiceTips(picker)[300].equals(TimeUtils.formatShort(Carrington.CR_start[300]) + " - " + TimeUtils.formatShort(Carrington.CR_start[301])), "Rotation tooltips");
        require(p.value(picker, Panel.IntField.INDEX) == 300, "The picker opens at the current rotation");
    }

    private static void synopticRequests() {
        Synoptic archive = new Synoptic();
        SynopticScreen screen = new SynopticScreen(archive);
        Dismissed dismissed = new Dismissed(screen);
        interval(screen, JAN_1, JAN_1 + DAY);
        Panel p = screen.build();
        require(p.has(nthCombo(p, 0), Panel.ENABLED) && !p.has(nthCombo(p, 1), Panel.ENABLED), "AIA channels disabled for HMI");
        require(p.value(nthCombo(p, 1), Panel.IntField.INDEX) == 2, "AIA defaults to 171");
        require(!enabled(screen, Panel.Kind.BUTTON, "Add"), "Add needs a selection");

        press(screen, "Search");
        JSONObject query = archive.queries.getLast();
        require(archive.paths.getLast().equals("/api/s005a/map/hmi"), "HMI endpoint");
        require(query.getString("start_date").equals(TimeUtils.formatDate(JAN_1)) && query.getString("end_date").equals(TimeUtils.formatDate(JAN_1 + DAY))
                && query.getJSONArray("channels").getString(0).equals("hmi.m_720s") && query.getJSONArray("map_type").getString(0).equals("forecast") && query.getBoolean("latest"), "HMI query " + query);
        require(!enabled(screen, Panel.Kind.BUTTON, "Search"), "Search disabled while searching");
        archive.replies.getLast().accept(maps(3));
        require(shows(screen, "3 found") && enabled(screen, Panel.Kind.BUTTON, "Search"), "Result shown, Search available");
        require(shows(screen, "hmi_m_720s_20220300_00.fits"), "File names in the list");

        screen.command(0, FIRST_ROW + 2, Panel.Command.SELECT_ADD);
        screen.command(0, FIRST_ROW, Panel.Command.SELECT_ADD);
        require(shows(screen, "2 selected") && enabled(screen, Panel.Kind.BUTTON, "Add"), "Selection enables Add");
        // Only Search invalidates the list.
        radio(screen, "AIA");
        combo(screen, 1, 5);
        radio(screen, "archived");
        screen.number(0, START, JAN_1 - DAY);
        require(shows(screen, "2 selected"), "Options keep the results until the next Search");
        press(screen, "Add");
        require(archive.loads.getLast().equals(List.of(maps(3).get(0), maps(3).get(2))) && dismissed.count == 1, "Maps load in listing order");

        press(screen, "Search");
        query = archive.queries.getLast();
        require(archive.paths.getLast().equals("/api/s050a/map/") && query.getJSONArray("channels").getString(0).equals("304") && query.getJSONArray("map_type").getString(0).equals("archived"), "AIA query " + query);
        require(!shows(screen, "3 found") && !shows(screen, "hmi_m_720s_20220300_00.fits"), "Search clears the list");
        archive.failures.getLast().run();
        require(shows(screen, "0 found") && enabled(screen, Panel.Kind.BUTTON, "Search"), "A failed search is an empty one");

        press(screen, "Search");
        Consumer<List<URI>> closed = archive.replies.getLast();
        press(screen, "Cancel");
        closed.accept(maps(3));
        require(shows(screen, "0 found"), "A closed window ignores its reply");
        radio(screen, "HMI");
        combo(screen, 0, 1);
        press(screen, "Search");
        require(archive.queries.getLast().getJSONArray("channels").getString(0).equals("hmi.ic_720s"), "HMI continuum channel");
    }

    private static void aspiicsRequests() {
        Aspiics archive = new Aspiics();
        AspiicsScreen screen = new AspiicsScreen(archive);
        Dismissed dismissed = new Dismissed(screen);
        screen.show();
        require(archive.replies.size() == 1, "Opening loads the orbits");
        press(screen, "Add");
        require(dismissed.count == 0 && archive.loads.isEmpty(), "Add without an orbit does nothing");
        require(!shows(screen, "Reload orbits") && !shows(screen, "Status"), "Only the original controls");
        Consumer<List<AspiicsScreen.Orbit>> reply = archive.replies.getLast();
        press(screen, "Close");
        require(dismissed.count == 1, "Close dismisses");
        reply.accept(orbits());
        require(screen.build().choices(nthCombo(screen.build(), 0)).length == 0, "A closed window ignores its orbits");
        screen.show();
        require(archive.replies.size() == 2, "Reopening without orbits loads them");
        archive.replies.getLast().accept(orbits());
        screen.show();
        require(archive.replies.size() == 2, "Orbits load once");

        Panel p = screen.build();
        int orbit = nthCombo(p, 0);
        require(Arrays.equals(p.choices(orbit), new String[]{"120", "119", "118", "117"}) && p.string(orbit, Panel.IntField.WIDTH_TEXT).equals("9999"), "Orbit choices and width");
        require(p.choiceTips(orbit)[1].equals(TimeUtils.format(1700000000000L - DAY) + " - " + TimeUtils.format(1700020000000L - DAY)), "Orbit tooltips");
        require(p.has(control(p, Panel.Kind.CHECK, "Total brightness"), Panel.SELECTED) && !p.has(control(p, Panel.Kind.CHECK, "Fe XIV"), Panel.SELECTED), "The first product is the default");

        combo(screen, 0, 2);
        p = screen.build();
        screen.checked(0, p.value(control(p, Panel.Kind.CHECK, "Fe XIV"), Panel.IntField.ID), true);
        screen.checked(0, p.value(control(p, Panel.Kind.CHECK, "Total brightness"), Panel.IntField.ID), false);
        screen.checked(0, p.value(control(p, Panel.Kind.CHECK, "He I D3"), Panel.IntField.ID), true);
        press(screen, "Add");
        require(archive.loads.size() == 2 && dismissed.count == 2, "One layer per product, then dismissal");
        APIRequest helium = archive.loads.getFirst();
        require(helium.server().equals("ROB") && helium.sourceId() == 509 && helium.startTime() == 1700000000000L - 2 * DAY && helium.endTime() == 1700020000000L - 2 * DAY
                && helium.cadence() == APIRequest.CADENCE_ALL && archive.loads.getLast().sourceId() == 510, "Request arguments " + helium);
    }

    private static void punchRequests() {
        Punch archive = new Punch();
        PunchScreen screen = new PunchScreen(archive);
        screen.show();
        require(archive.levels.equals(List.of("3")), "Opening lists the default level");
        require(shows(screen, "Checking products..."), "Products state");
        press(screen, "Search");
        require(archive.queries.isEmpty(), "Search needs a product");

        Consumer<List<String>> stale = archive.products.getLast();
        combo(screen, 0, 2);
        require(archive.levels.getLast().equals("2"), "A level lists its products");
        stale.accept(List.of("OLD"));
        require(archive.coverage.isEmpty(), "A stale product list is ignored");

        archive.products.getLast().accept(List.of("PTM", "CAM"));
        Panel p = screen.build();
        require(p.value(nthCombo(p, 1), Panel.IntField.INDEX) == 1 && archive.covered.getLast().equals("2CAM"), "CAM is the default product");
        require(p.string(nthCombo(p, 1), Panel.IntField.TIP).equals("clear average / low-noise mosaic"), "Product tooltip");
        require(shows(screen, "Checking archive coverage..."), "Coverage state");

        LongConsumer staleCoverage = archive.coverage.getLast();
        combo(screen, 1, 0);
        staleCoverage.accept(JAN_1);
        require(shows(screen, "Checking archive coverage..."), "A stale coverage is ignored");
        archive.coverage.getLast().accept(0);
        require(shows(screen, "Archive: no data for this product"), "No coverage");
        combo(screen, 1, 1);
        archive.coverage.getLast().accept(JAN_1);
        require(shows(screen, "Archive: latest day available is " + TimeUtils.formatDate(JAN_1)), "Latest day");
        require(startOf(screen) == JAN_1 && endOf(screen) == JAN_1 + DAY - 1000, "The interval follows the latest day");

        screen.number(0, END, JAN_1 + DAY);
        combo(screen, 1, 0);
        archive.coverage.getLast().accept(JAN_1 - 10 * DAY);
        require(startOf(screen) == JAN_1, "A user's interval stays");

        combo(screen, 2, 1);
        press(screen, "Search");
        Object[] query = archive.queries.getLast();
        require(query[0].equals("2") && query[1].equals("PTM") && query[2].equals(JAN_1) && query[3].equals(JAN_1 + DAY) && query[4].equals(10 * TimeUtils.MINUTE_IN_MILLIS), "Query " + Arrays.toString(query));
        archive.searches.getLast().accept(List.of());
        require(shows(screen, "0 found — archive may not cover this period; see umbra.nascom.nasa.gov/punch"), "The empty result names the archive");

        combo(screen, 0, 0);
        archive.products.getLast().accept(List.of());
        require(shows(screen, "Archive: no products for this level"), "No products");
    }

    private static PunchScreen punchWith(Punch archive, int files) {
        PunchScreen screen = new PunchScreen(archive);
        screen.show();
        archive.products.getLast().accept(List.of("CAM"));
        archive.coverage.getLast().accept(JAN_1);
        press(screen, "Search");
        archive.searches.getLast().accept(punchItems(files));
        return screen;
    }

    private static void punchConfirmation() {
        Punch archive = new Punch();
        PunchScreen screen = punchWith(archive, 201);
        Dismissed dismissed = new Dismissed(screen);
        for (int i = 0; i < 200; i++)
            screen.command(0, FIRST_ROW + i, Panel.Command.SELECT_ADD);
        messages.shown.clear();
        press(screen, "Add");
        require(messages.shown.isEmpty() && archive.loads.getLast().size() == 200 && dismissed.count == 1, "200 files load without a question");

        press(screen, "Search");
        archive.searches.getLast().accept(punchItems(201));
        for (int i = 0; i < 201; i++)
            screen.command(0, FIRST_ROW + i, Panel.Command.SELECT_ADD);
        press(screen, "Add");
        Notice notice = messages.shown.getLast();
        require(notice.title.equals("Large PUNCH download") && notice.action != null, "Over 200 files asks");
        require(notice.text.equals(String.format("This will download 201 native-resolution FITS files (typically a few MB each).%nSeveral files may be fetched in parallel.%nProceed?")), "The original question: " + notice.text);
        require(archive.loads.size() == 1 && dismissed.count == 1, "Nothing loads before the answer");
        // The question stands for the files it named, whatever the list shows after.
        screen.command(0, FIRST_ROW, Panel.Command.SELECT_REMOVE);
        notice.action.run();
        require(archive.loads.getLast().size() == 201 && dismissed.count == 2, "OK loads the files asked about");

        press(screen, "Search");
        archive.searches.getLast().accept(punchItems(5001));
        for (int i = 0; i < 5001; i++)
            screen.command(0, FIRST_ROW + i, Panel.Command.SELECT_ADD);
        messages.shown.clear();
        press(screen, "Add");
        notice = messages.shown.getLast();
        require(notice.title.equals("PUNCH error") && notice.action == null && archive.loads.size() == 2 && dismissed.count == 2, "Over 5000 files is refused");
        require(notice.text.equals(String.format("Too many files selected for download: 5001.%nPlease reduce the selection to less than 5000 files or choose a sparser cadence.")), "The original limit: " + notice.text);
    }

    private static int node(Panel p, String label) {
        for (int i = 0; i < p.size(); i++) {
            if ((p.kind(i) == Panel.Kind.TREE || p.kind(i) == Panel.Kind.ITEM) && p.string(i, Panel.IntField.LABEL).equals(label))
                return p.value(i, Panel.IntField.ID);
        }
        throw new AssertionError("No node " + label);
    }

    private static void timelineRequests() throws Exception {
        List<List<BandType>> loads = new ArrayList<>();
        List<Boolean> full = new ArrayList<>();
        TimelineScreen screen = new TimelineScreen(null, (types, resolution) -> {
            loads.add(types);
            full.add(resolution);
        });
        Dismissed dismissed = new Dismissed(screen);
        Map<String, BandDataset[]> catalogs = catalogs();
        screen.setCatalogs(catalogs);
        Panel p = screen.build();
        require(p.has(control(p, Panel.Kind.TREE, "GOES"), Panel.OPEN) && !p.has(control(p, Panel.Kind.TREE, "OMNI"), Panel.OPEN), "Only the first catalog opens");
        require(p.has(control(p, Panel.Kind.BUTTON, "Add"), Panel.DEFAULT), "Add is the default button");

        screen.command(0, node(p, "GOES"), Panel.Command.SELECT_ONLY);
        press(screen, "Add");
        require(loads.isEmpty() && dismissed.count == 0, "A catalog is a heading and loads nothing");

        // Two groups of one dataset, then a dataset already inside the first: paths in selection order, each type once.
        screen.command(0, node(p, "GOES"), Panel.Command.SELECT_REMOVE);
        screen.command(0, node(p, "GOES-17"), Panel.Command.SELECT_ADD);
        screen.command(0, node(p, "GOES-16"), Panel.Command.SELECT_ADD);
        p = screen.build();
        int group16 = control(p, Panel.Kind.TREE, "GOES-16");
        int child16 = group16 + 1;
        require(p.kind(child16) == Panel.Kind.ITEM && p.string(child16, Panel.IntField.LABEL).equals("XRS"), "Grouped datasets sit under their group");
        screen.command(0, p.value(child16, Panel.IntField.ID), Panel.Command.SELECT_ADD);
        screen.checked(0, p.value(control(p, Panel.Kind.CHECK, "Full time resolution"), Panel.IntField.ID), true);
        press(screen, "Add");
        List<String> names = new ArrayList<>();
        for (BandType type : loads.getLast())
            names.add(type.getName());
        require(names.equals(List.of("XRS 2", "XRS long", "XRS short")), "Selection order, deduplicated: " + names);
        require(full.getLast() && dismissed.count == 1, "Full resolution passes, Add dismisses");

        screen.setCatalogs(catalogs());
        p = screen.build();
        require(!p.has(control(p, Panel.Kind.TREE, "GOES-16"), Panel.SELECTED), "New catalogs clear the selection");
        screen.command(0, node(p, "OMNI2"), Panel.Command.DOUBLE_CLICK);
        require(loads.size() == 1 && dismissed.count == 1, "A double click without a selection loads nothing");
        screen.command(0, node(p, "OMNI2"), Panel.Command.SELECT_ONLY);
        screen.command(0, node(p, "OMNI2"), Panel.Command.DOUBLE_CLICK);
        require(loads.size() == 2 && dismissed.count == 2, "A double click on a selected dataset loads it");

        TimelineScreen hostless = new TimelineScreen("Timeline datasets are not loaded yet.", (types, resolution) -> loads.add(types));
        require(shows(hostless, "Timeline datasets are not loaded yet.") && !shows(new TimelineScreen(null, (types, resolution) -> {}), "Timeline datasets are not loaded yet."), "Only a host without a chart explains the empty tree");
    }

    // Real Swing widgets, without a window.

    // UIGlobals.setLaf without its cursors, which need a display.
    static void theme() throws Exception {
        com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme.setup();
        com.jidesoft.plaf.LookAndFeelFactory.installJideExtension();
        UIManager.getLookAndFeelDefaults().put("defaultFont", UIManager.getFont(Platform.isLinux() ? "small.font" : "medium.font"));
        UIGlobals.backColor = UIManager.getColor("Label.background");
        UIGlobals.foreColor = UIManager.getColor("Label.foreground");
        UIGlobals.uiFont = UIManager.getFont("defaultFont");
        float size = UIGlobals.uiFont.getSize();
        UIGlobals.uiFontBold = UIGlobals.uiFont.deriveFont(java.awt.Font.BOLD);
        UIGlobals.uiFontSmall = UIGlobals.uiFont.deriveFont(size - 2);
        UIGlobals.uiFontSmallBold = UIGlobals.uiFontSmall.deriveFont(java.awt.Font.BOLD);
        UIGlobals.uiFontMono = UIManager.getFont("monospaced.font");
        UIGlobals.uiFontMonoSmall = UIGlobals.uiFontMono.deriveFont(size - 2);
        UIGlobals.sansFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, (int) size);
        for (String key : List.of("Button.arc", "CheckBox.arc", "Component.arc", "ProgressBar.arc", "TextComponent.arc"))
            UIManager.put(key, 6);
        UIManager.put("ButtonPanel.order", "OCA");
        try (java.io.InputStream is = org.helioviewer.jhv.io.FileUtils.getResource("/fonts/materialdesignicons-webfont.ttf")) {
            UIGlobals.uiFontMDI = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(UIGlobals.uiFontMDI);
        }
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested)
                layout(nested);
        }
    }

    static SwingPanel swing(Screen screen) {
        SwingPanel panel = new SwingPanel(screen);
        JRootPane root = new JRootPane();
        root.setContentPane(panel);
        panel.refresh();
        size(panel);
        return panel;
    }

    private static void size(SwingPanel panel) {
        panel.refresh();
        panel.setSize(panel.getPreferredSize());
        layout(panel);
        layout(panel);
    }

    static <T> List<T> all(Container container, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (type.isInstance(child) && child.isVisible())
                found.add(type.cast(child));
            if (child instanceof Container nested && child.isVisible() && !(child instanceof JComboBox) && !(child instanceof AbstractButton))
                found.addAll(all(nested, type));
        }
        return found;
    }

    static AbstractButton button(Container container, String text) {
        for (AbstractButton button : all(container, AbstractButton.class)) {
            if (text.equals(button.getText()))
                return button;
        }
        throw new AssertionError("No button " + text);
    }

    static void key(JComponent component, String stroke) {
        Object name = component.getInputMap().get(KeyStroke.getKeyStroke(stroke));
        require(name != null, "No binding for " + stroke);
        component.getActionMap().get(name).actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, stroke));
    }

    private static void click(Component component, Point at, int clicks) {
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_CLICKED, 0, 0, at.x, at.y, clicks, false, MouseEvent.BUTTON1));
    }

    // The focus manager drops a focus event of a component without a window.
    private static void focusLost(Component component) {
        for (java.awt.event.FocusListener listener : component.getFocusListeners())
            listener.focusLost(new FocusEvent(component, FocusEvent.FOCUS_LOST));
    }

    private static Point rowPoint(JList<?> list, int row) {
        java.awt.Rectangle bounds = list.getCellBounds(row, row);
        return new Point(bounds.x + 10, bounds.y + bounds.height / 2);
    }

    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }

    private record Bounds(String state, String kind, String text, int x, int y, int width, int height, boolean enabled) {}

    private static String kindOf(Component component) {
        return component instanceof ChoicePicker ? "CarringtonPicker" : component instanceof javax.swing.JCheckBox ? "JCheckBox" : component.getClass().getSimpleName();
    }

    private static String textOf(Component component) {
        return switch (component) {
            case javax.swing.JLabel label -> label.getText();
            case AbstractButton button when !(button instanceof com.jidesoft.swing.JideSplitButton) && !(button instanceof ChoicePicker) -> button.getText();
            case JComboBox<?> combo -> combo.getSelectedItem() == null ? "" : combo.getSelectedItem().toString();
            default -> "";
        };
    }

    // Each control of the original at its bounds. A radio centers its text in the original's taller cell,
    // and the coverage text of the original's HTML pane sits inside its margin.
    private static void matchReference(String state, SwingPanel panel, List<Bounds> reference) {
        Dimension size = panel.getPreferredSize();
        List<Component> controls = new ArrayList<>();
        for (Class<?> type : List.of(javax.swing.JLabel.class, AbstractButton.class, JComboBox.class, TimeField.class, JScrollPane.class))
            controls.addAll(all(panel, type).stream().map(Component.class::cast).toList());
        for (Bounds expected : reference) {
            if (!expected.state.equals(state))
                continue;
            if (expected.kind.equals("pane")) {
                require(size.width == expected.width && size.height == expected.height, state + " window " + size + " against the original " + expected.width + "x" + expected.height);
                continue;
            }
            Component match = null;
            int best = Integer.MAX_VALUE;
            for (Component control : controls) {
                boolean same = expected.kind.equals("HTMLPane") ? control instanceof javax.swing.JLabel && textOf(control).startsWith(expected.y < 100 ? "" : "?") && SwingUtilities.convertPoint(control, 0, 0, panel).x == expected.x + 6
                        : kindOf(control).equals(expected.kind) && textOf(control).equals(expected.text);
                if (same) {
                    Point at = SwingUtilities.convertPoint(control, 0, 0, panel);
                    int distance = Math.abs(at.x - expected.x) + Math.abs(at.y - expected.y);
                    if (distance < best) {
                        best = distance;
                        match = control;
                    }
                }
            }
            require(match != null, state + " has no " + expected);
            Point at = SwingUtilities.convertPoint(match, 0, 0, panel);
            String where = state + " " + expected + " at " + at.x + "," + at.y + " " + match.getWidth() + "x" + match.getHeight();
            int center = 2 * at.y + match.getHeight();
            int original = 2 * expected.y + expected.height;
            switch (expected.kind) {
                case "JRadioButton" -> require(at.x == expected.x && Math.abs(center - original) <= 1, where);
                case "HTMLPane" -> require(Math.abs(center - original) <= 1, where);
                default -> require(at.x == expected.x && at.y == expected.y && match.getWidth() == expected.width && match.getHeight() == expected.height, where);
            }
            if (!expected.kind.equals("HTMLPane"))
                require(match.isEnabled() == expected.enabled, where + " enabled " + match.isEnabled());
            controls.remove(match);
        }
    }

    private static List<Bounds> reference() throws Exception {
        List<Bounds> reference = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(System.getProperty("jhv.reference")))) {
            if (line.startsWith("#"))
                continue;
            String[] f = line.split("\t", -1);
            reference.add(new Bounds(f[0], f[1], f[2], Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]), Boolean.parseBoolean(f[7])));
        }
        return reference;
    }

    private static void swingGeometry() throws Exception {
        // Text widths come from the platform font; the reference was measured on macOS.
        if (!Platform.isMacOS())
            return;
        List<Bounds> reference = reference();

        Soar soarArchive = new Soar();
        SoarScreen soar = new SoarScreen(soarArchive);
        SwingPanel panel = swing(soar);
        matchReference("soar-default", panel, reference);
        press(soar, "Search");
        soarArchive.searches.getLast().accept(soarItems(40));
        size(panel);
        all(panel, JList.class).getFirst().setSelectedIndices(new int[]{1, 2, 5});
        size(panel);
        matchReference("soar-loaded", panel, reference);

        Synoptic synopticArchive = new Synoptic();
        SynopticScreen synoptic = new SynopticScreen(synopticArchive);
        panel = swing(synoptic);
        matchReference("synoptic-default", panel, reference);
        press(synoptic, "Search");
        synopticArchive.replies.getLast().accept(maps(12));
        size(panel);
        all(panel, JList.class).getFirst().setSelectedIndices(new int[]{0, 3});
        size(panel);
        matchReference("synoptic-loaded", panel, reference);

        Aspiics aspiicsArchive = new Aspiics();
        AspiicsScreen aspiics = new AspiicsScreen(aspiicsArchive);
        aspiics.show();
        panel = swing(aspiics);
        matchReference("aspiics-empty", panel, reference);
        aspiicsArchive.replies.getLast().accept(orbits());
        size(panel);
        matchReference("aspiics-loaded", panel, reference);

        Punch punchArchive = new Punch();
        PunchScreen punch = new PunchScreen(punchArchive);
        punch.show();
        panel = swing(punch);
        matchReference("punch-default", panel, reference);
        punchArchive.products.getLast().accept(List.of("CAM", "CTM", "PAM"));
        size(panel);
        matchReference("punch-checking", panel, reference);
        punchArchive.coverage.getLast().accept(1763596800000L);
        press(punch, "Search");
        punchArchive.searches.getLast().accept(punchItems(30));
        size(panel);
        all(panel, JList.class).getFirst().setSelectedIndices(new int[]{0, 1, 2, 8});
        size(panel);
        matchReference("punch-loaded", panel, reference);
        press(punch, "Search");
        punchArchive.searches.getLast().accept(List.of());
        size(panel);
        matchReference("punch-empty", panel, reference);

        TimelineScreen timeline = new TimelineScreen(null, (types, full) -> {});
        panel = swing(timeline);
        matchReference("timeline-empty", panel, reference);
        timeline.setCatalogs(catalogs());
        size(panel);
        matchReference("timeline-loaded", panel, reference);
    }

    @SuppressWarnings("unchecked")
    private static void swingList() throws Exception {
        Soar archive = new Soar();
        SoarScreen screen = new SoarScreen(archive);
        SwingPanel panel = swing(screen);
        require(all(panel, javax.swing.JTable.class).isEmpty(), "Results are a list, not a table");
        press(screen, "Search");
        archive.searches.getLast().accept(soarItems(40));
        size(panel);
        JList<String> list = all(panel, JList.class).getFirst();
        require(list.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION && list.getFixedCellHeight() == 17, "Multiple selection, original row height");
        require(list.getClientProperty(Searchable.CLIENT_PROPERTY_SEARCHABLE) instanceof com.jidesoft.swing.ListSearchable, "Typing searches the list");
        JScrollPane scroll = all(panel, JScrollPane.class).getFirst();
        require(scroll.getPreferredSize().equals(new Dimension(500, 350)), "List viewport");

        // BasicListUI asks the toolkit for the menu shortcut on a press, so a press does not run headless: these are its model calls.
        list.setSelectionInterval(3, 3);
        panel.refresh();
        require(shows(screen, "1 selected (0.2GiB)"), "A click selects one row");
        list.setSelectionInterval(3, 6);
        panel.refresh();
        require(Arrays.equals(list.getSelectedIndices(), new int[]{3, 4, 5, 6}) && shows(screen, "4 selected (0.8GiB)"), "Shift extends from the anchor");
        list.removeSelectionInterval(5, 5);
        panel.refresh();
        require(Arrays.equals(list.getSelectedIndices(), new int[]{3, 4, 6}), "The menu shortcut key toggles one row");
        key(list, "DOWN");
        panel.refresh();
        require(shows(screen, "1 selected (0.2GiB)"), "An arrow reaches the screen");
        key(list, "shift DOWN");
        panel.refresh();
        require(shows(screen, "2 selected (0.4GiB)"), "Shift arrow extends");
        list.clearSelection();
        panel.refresh();
        require(shows(screen, "0 selected (0.0GiB)"), "Clearing reaches the screen, in the original's words");
        list.setSelectionInterval(0, 39);
        panel.refresh();
        require(shows(screen, "40 selected (8.2GiB)"), "A whole range reaches the screen");
        require(Arrays.equals(list.getSelectedIndices(), java.util.stream.IntStream.range(0, 40).toArray()), "The list keeps it through a refresh");
        press(screen, "Search");
        size(panel);
        require(list.getModel().getSize() == 0 && list.isSelectionEmpty(), "A new search empties the list");
        require(shows(screen, "0 selected (0.0GiB)"), "A search keeps the original's words");
    }

    private static void swingTime() throws Exception {
        Soar archive = new Soar();
        SoarScreen screen = new SoarScreen(archive);
        interval(screen, JAN_1, JAN_1 + DAY);
        SwingPanel panel = swing(screen);
        List<com.jidesoft.swing.JideSplitButton> splits = all(panel, com.jidesoft.swing.JideSplitButton.class);
        require(splits.size() == 2, "Two shift buttons");
        require(splits.get(0).getToolTipText().equals("Move time interval backward") && splits.get(0).getItemCount() == 3, "Three units per shift button");
        JRadioButtonMenuItem week = (JRadioButtonMenuItem) splits.get(1).getItem(1);
        require(((JRadioButtonMenuItem) splits.get(1).getItem(0)).isSelected() && week.getText().equals("Week"), "Day is the default unit");
        press(screen, "Search");
        archive.searches.getLast().accept(soarItems(2));
        panel.refresh();
        week.doClick();
        panel.refresh();
        require(week.isSelected() && shows(screen, "2 found"), "A unit alone keeps the results");
        // A press on the button part, which needs a popup menu's mouse grab in a window.
        for (java.awt.event.ActionListener listener : splits.get(1).getActionListeners())
            listener.actionPerformed(new ActionEvent(splits.get(1), ActionEvent.ACTION_PERFORMED, ""));
        panel.refresh();
        require(startOf(screen) == JAN_1 + 7 * DAY && shows(screen, "0 found"), "The button shifts by its unit and clears the results");

        List<TimeField> fields = all(panel, TimeField.class);
        require(fields.get(0).getToolTipText().equals("Select start date") && fields.get(1).getToolTipText().equals("Select end date"), "Field tooltips");
        TimeField start = fields.getFirst();
        start.setText(TimeUtils.formatShort(JAN_1));
        focusLost(start);
        panel.refresh();
        require(startOf(screen) == JAN_1, "Focus loss commits the text");
        press(screen, "Search");
        archive.searches.getLast().accept(soarItems(2));
        panel.refresh();
        focusLost(start);
        start.setText("nonsense");
        focusLost(start);
        panel.refresh();
        require(shows(screen, "2 found") && startOf(screen) == JAN_1, "Unchanged and invalid commits keep the results");

        ChoicePicker picker = all(panel, ChoicePicker.class).getFirst();
        require(picker.getText().equals("CR") && picker.getToolTipText().equals("Select Carrington rotation"), "Rotation picker");
        JList<?> rotations = field(picker, "list", JList.class);
        rotations.setSize(rotations.getPreferredSize());
        Point row = rowPoint(rotations, 300);
        require(rotations.getToolTipText(new MouseEvent(rotations, MouseEvent.MOUSE_MOVED, 0, 0, row.x, row.y, 0, false)).equals(TimeUtils.formatShort(Carrington.CR_start[300]) + " - " + TimeUtils.formatShort(Carrington.CR_start[301])), "Rotation span tooltip");
        click(rotations, row, 1);
        panel.refresh();
        require(startOf(screen) == TimeUtils.floorSec(Carrington.CR_start[300]) && endOf(screen) == TimeUtils.floorSec(Carrington.CR_start[301]), "A picked rotation sets the interval");

        radio(screen, "SOOP");
        panel.refresh();
        require(!fields.getFirst().isEnabled() && !splits.getFirst().isEnabled() && !picker.isEnabled(), "SOOP mode disables the whole selector");
        JRadioButton time = (JRadioButton) button(panel, "Time");
        time.doClick();
        panel.refresh();
        require(fields.getFirst().isEnabled() && picker.isEnabled() && all(panel, JComboBox.class).getFirst().isEnabled() == false, "Time mode enables it again");
    }

    // The focus owner of a window that the headless toolkit cannot open.
    private static final class Focused extends java.awt.DefaultKeyboardFocusManager {
        Component owner;

        @Override
        public Component getFocusOwner() {
            return owner;
        }
    }

    // PUNCH and Synoptic open with the focus in the start field.
    private static void swingFocusedTime() {
        Punch archive = new Punch();
        PunchScreen screen = new PunchScreen(archive);
        screen.show();
        SwingPanel panel = swing(screen);
        archive.products.getLast().accept(List.of("CAM"));
        panel.refresh();
        List<TimeField> fields = all(panel, TimeField.class);
        TimeField start = fields.getFirst();
        Focused focus = new Focused();
        java.awt.KeyboardFocusManager previous = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager();
        java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager(focus);
        try {
            focus.owner = start;
            require(start.hasFocus(), "The field has the focus");
            archive.coverage.getLast().accept(JAN_1);
            panel.refresh();
            require(start.getTime() == JAN_1 && start.getText().equals(TimeUtils.formatShort(JAN_1)), "A focused field shows the time coverage set: " + start.getText());
            require(startOf(screen) == JAN_1, "The screen has it too");

            start.setText("2024-01-01T00:");
            panel.refresh();
            require(start.getText().equals("2024-01-01T00:"), "A refresh leaves an unchanged time's edit in progress");
            press(screen, "Search");
            panel.refresh();
            require(start.getText().equals("2024-01-01T00:"), "A search leaves it too");
        } finally {
            java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
        }
    }

    private static void swingCombo() {
        Aspiics archive = new Aspiics();
        AspiicsScreen screen = new AspiicsScreen(archive);
        screen.show();
        SwingPanel panel = swing(screen);
        JComboBox<?> combo = all(panel, JComboBox.class).getFirst();
        require(combo.getItemCount() == 0 && combo.getSelectedIndex() == -1, "An empty combo shows nothing");
        int empty = combo.getPreferredSize().width;
        archive.replies.getLast().accept(orbits());
        size(panel);
        require(combo.getItemCount() == 4 && combo.getSelectedIndex() == 0 && combo.getPreferredSize().width == empty, "Orbits leave the width alone");
        JList<String> list = new JList<>();
        @SuppressWarnings("unchecked")
        javax.swing.ListCellRenderer<Object> renderer = (javax.swing.ListCellRenderer<Object>) combo.getRenderer();
        JComponent cell = (JComponent) renderer.getListCellRendererComponent(list, "119", 1, false, false);
        require(cell.getToolTipText().equals(TimeUtils.format(1700000000000L - DAY) + " - " + TimeUtils.format(1700020000000L - DAY)), "Orbit tooltip in the list");
    }

    private static Screen panelScreen(Panel panel) {
        return () -> panel;
    }

    private static String rejection(Panel snapshot) {
        try {
            new SwingPanel(panelScreen(snapshot));
            return "";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    // The widget comes from the snapshot's kind, not from search, headers or the shape of its rows.
    private static void swingCollections() {
        SwingPanel plainList = new SwingPanel(panelScreen(new Panel().list(200, 100, 17).item(1, "One", true).tip("Tip").item(2, "Two", false).listEnd()));
        JList<?> list = all(plainList, JList.class).getFirst();
        require(list.getModel().getSize() == 2 && list.isSelectedIndex(0) && !list.isSelectedIndex(1), "A list shows its items and their selection");
        require(list.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION, "A list selects one row unless declared multiple");
        require(list.getClientProperty(Searchable.CLIENT_PROPERTY_SEARCHABLE) == null, "A list searches only when declared searchable");
        require(all(plainList, JScrollPane.class).getFirst().getPreferredSize().equals(new Dimension(200, 100)), "A list has its declared viewport");

        SwingPanel searchable = new SwingPanel(panelScreen(new Panel().list(200, 100, 17).multiple(true).searchable().item(1, "One", false).listEnd()));
        JList<?> found = all(searchable, JList.class).getFirst();
        require(found.getClientProperty(Searchable.CLIENT_PROPERTY_SEARCHABLE) != null, "A searchable list searches");
        require(found.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, "A multiple list selects many rows");
        require(rejection(new Panel().list(200, 100, 17).label("Text").listEnd()).equals("Swing list has no LABEL"), "A list takes items only");
        require(rejection(new Panel().button(1, "Save").save(null, "name", "")).equals("Swing button has no SAVE"), "A save button is refused, not left inert");

        SwingPanel tree = new SwingPanel(panelScreen(new Panel().treeView(150, 80).identity(1).tree(1, "Branch", true).item(2, "Leaf", false).treeEnd().treeViewEnd()));
        require(all(tree, JTree.class).size() == 1 && all(tree, JList.class).isEmpty(), "A tree view is a tree");
        SwingPanel scroll = new SwingPanel(panelScreen(new Panel().scroll(0).searchable().label("Text").scrollEnd()));
        require(all(scroll, JTree.class).isEmpty() && all(scroll, JLabel.class).size() == 1, "A scroll stays a scroll of its controls, search or not");

        Panel rich = new Panel().table(4, 1, 0).searchable()
                .tableRow(1, false).check(2, "", true).label("Layer").label("2020").button(3, "x").rowEnd().tableEnd();
        require(rejection(rich).equals("Swing table needs a header row"), "A headerless table is refused as a table, not read as a list");
    }

    // A gesture shows its result once, at once, without a queued refresh; a notification outside a gesture still reaches the view.
    private static void swingRefreshOwner() throws Exception {
        boolean[] value = {false};
        Runnable[] listener = new Runnable[1];
        int[] builds = {0};
        Panel buffer = new Panel();
        SwingPanel panel = new SwingPanel(new Screen() {
            @Override
            public Panel build() {
                builds[0]++;
                return buffer.reset().check(1, "On", value[0]);
            }

            @Override
            public void listen(Runnable _listener) {
                listener[0] = _listener;
            }

            @Override
            public void checked(int owner, int item, boolean on) {
                value[0] = !on; // The model decides; what the view shows comes from it.
                listener[0].run();
            }
        });
        java.util.concurrent.atomic.AtomicBoolean queued = field(panel, "refreshQueued", java.util.concurrent.atomic.AtomicBoolean.class);
        javax.swing.JCheckBox check = all(panel, javax.swing.JCheckBox.class).getFirst();
        builds[0] = 0;
        check.doClick();
        require(builds[0] == 1 && !queued.get(), "A gesture builds once and queues nothing: " + builds[0] + ", " + queued.get());
        require(!check.isSelected(), "The gesture shows what the model left");
        value[0] = true;
        listener[0].run();
        require(queued.get() && builds[0] == 1, "A notification waits for the queue");
        panel.lazyRepaint();
        require(builds[0] == 2 && check.isSelected(), "A notification reaches the view");
    }

    private static void swingTree() throws Exception {
        List<List<BandType>> loads = new ArrayList<>();
        TimelineScreen screen = new TimelineScreen(null, (types, full) -> loads.add(types));
        Dismissed dismissed = new Dismissed(screen);
        SwingPanel panel = swing(screen);
        JTree tree = all(panel, JTree.class).getFirst();
        require(panel.initialFocus() == tree, "The tree takes the focus");
        require(((JRootPane) SwingUtilities.getRootPane(panel)).getDefaultButton() == button(panel, "Add"), "Add is the default button");
        screen.setCatalogs(catalogs());
        size(panel);
        require(!tree.isRootVisible() && tree.getShowsRootHandles() && tree.getToggleClickCount() == 0, "Root hidden, handles, no click toggling");
        require(tree.getSelectionModel().getSelectionMode() == TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, "Discontiguous selection");
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        require(renderer.getLeafIcon() == null && renderer.getOpenIcon() == null && renderer.getClosedIcon() == null, "No icons");
        require(tree.isExpanded(0) && !tree.isExpanded(tree.getRowCount() - 1), "The first catalog opens");
        require(all(panel, JScrollPane.class).getFirst().getPreferredSize().equals(new Dimension(350, 350)), "Tree viewport");
        require(tree.getClientProperty(Searchable.CLIENT_PROPERTY_SEARCHABLE) instanceof TreeSearchable searchable && searchable.isRecursive(), "Typing searches the collapsed branches too");

        tree.expandRow(1);
        tree.setSelectionRow(0);
        panel.refresh();
        button(panel, "Add").doClick();
        require(loads.isEmpty() && dismissed.count == 0, "A catalog loads nothing");
        require(tree.isExpanded(1), "A refresh keeps what the user expanded");

        tree.setSize(tree.getPreferredSize());
        layout(tree);
        tree.setSelectionRows(new int[]{1});
        tree.addSelectionRow(2);
        panel.refresh();
        require(tree.getSelectionCount() == 2, "A group and its dataset stay selected together");
        button(panel, "Add").doClick();
        require(loads.getFirst().size() == 2 && dismissed.count == 1, "A selected group loads its datasets, overlap once");

        tree.clearSelection();
        java.awt.Rectangle row = tree.getRowBounds(2);
        Point at = new Point(row.x + 4, row.y + row.height / 2);
        tree.setSelectionRow(2);
        click(tree, at, 2);
        require(loads.size() == 2 && dismissed.count == 2, "A double click on a row loads the selection");

        int rows = tree.getRowCount();
        tree.setSelectionRow(1);
        panel.refresh();
        key(tree, "DOWN");
        panel.refresh();
        TreePath selected = tree.getSelectionPath();
        require(selected != null && tree.getRowForPath(selected) == 2 && tree.getRowCount() == rows, "An arrow moves the selection");
        tree.setSelectionRow(3);
        tree.setSelectionInterval(2, 3);
        button(panel, "Add").doClick();
        require(loads.getLast().stream().map(BandType::getName).toList().equals(List.of("XRS long", "XRS short", "XRS 2")), "An upward range keeps the tree's dataset order");
    }

    private static void swingButtons() {
        for (DialogScreen screen : List.of(new SoarScreen(new Soar()), new SynopticScreen(new Synoptic()), new PunchScreen(new Punch()), new TimelineScreen(null, (types, full) -> {}))) {
            SwingPanel panel = swing(screen);
            AbstractButton cancel = button(panel, "Cancel");
            AbstractButton add = button(panel, "Add");
            require(SwingUtilities.convertPoint(cancel, 0, 0, panel).x < SwingUtilities.convertPoint(add, 0, 0, panel).x, "Cancel stands left of Add, as the production order");
            require(SwingUtilities.convertPoint(add, 0, 0, panel).x + add.getWidth() == panel.getWidth(), "Add ends at the window's edge");
            JRootPane root = SwingUtilities.getRootPane(panel);
            require((root.getDefaultButton() != null) == (screen instanceof TimelineScreen), screen.title() + " default button as the original");
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.init();
        org.helioviewer.jhv.io.Directories.createPersistentDirs();
        org.helioviewer.jhv.app.Log.init();
        org.helioviewer.jhv.io.Directories.createCacheDirs();
        org.helioviewer.jhv.app.AppInit.loadSpice();
        Message.setHandler(messages);
        soarRequests();
        soarSelection();
        timeSelection();
        synopticRequests();
        aspiicsRequests();
        punchRequests();
        punchConfirmation();
        timelineRequests();
        SwingUtilities.invokeAndWait(() -> {
            try {
                theme();
                swingGeometry();
                swingList();
                swingTime();
                swingFocusedTime();
                swingCombo();
                swingCollections();
                swingRefreshOwner();
                swingTree();
                swingButtons();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        System.out.println("DialogScreenTest passed");
        System.exit(0);
    }

}
