package org.helioviewer.jhv.plugins.swek;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.event.EventParameter;
import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

// One event's summary, parameters and related events. The screen owns their state and the details request; its window owns placement and closing.
public final class EventInformationScreen implements Screen {

    @FunctionalInterface
    public interface Details {
        void open(RelatedEvents related, SolarEvent event);
    }

    private record Listed(RelatedEvents related, SolarEvent event) {}

    private record SortKey(int column, boolean descending) {}

    private static final int MAIN = 0;
    private static final int TABLES = 1;
    private static final int EVENTS = 3;

    private static final int SECTION = 1;
    private static final int RELATED = 2;
    private static final int OTHER = 3;

    private static final int TABLE = 1;
    private static final int HOVER = 1;
    private static final int DETAILS = 2;
    private static final int SORT = 10;
    private static final int ROW = 100;

    private static final int ICON_SIZE = 32;
    private static final int ICON_MARGIN = 5;
    private static final int ICON_GAP = 23;
    private static final int END_MARGIN = 20;
    private static final int BAR_WIDTH = 140;
    private static final int BAR_HEIGHT = 4;
    private static final int TABLE_HEIGHT = 150;
    private static final int NAME_WIDTH = 180;
    private static final int VALUE_WIDTH = 200;
    private static final int STRING_CUTOFF = 256;
    private static final String LABEL_WIDTH = "Start Time";
    private static final String COLON_WIDTH = ":   ";
    private static final String TIME_WIDTH = "0000-00-00T00:00:00";
    private static final String[] COLUMNS = {"Parameter Name", "Value"};
    private static final String[] TITLES = {"Standard Parameters", "All Parameters", "Related Events", "Other Related Events"};

    private final Panel buffer = new Panel();
    private final SWEKSession session;
    private final Details details;
    private final Listed summary;
    private final List<Listed> related = new ArrayList<>();
    private final ArrayList<Listed> other = new ArrayList<>();
    private final Parameters standard;
    private final Parameters all;
    private final boolean[] open = {true, false, false, false};

    private Listed hovered;
    private Runnable listener = () -> {};
    private boolean closed;
    private int fit = 1;

    public EventInformationScreen(SWEKSession _session, RelatedEvents _related, SolarEvent event, Details _details) {
        session = _session;
        details = _details;
        summary = new Listed(_related, event);
        for (SolarEvent associated : _related.getAssociatedEvents(event))
            related.add(new Listed(_related, associated));
        standard = new Parameters(event.getVisibleEventParameters());
        all = new Parameters(event.getAllEventParameters());

        int id = event.getUniqueID();
        Task.submitBackground("event-info", () -> session.getEventDetails(id), this::loaded, this::failed);
    }

    public String title() {
        return summary.event.getSupplier().group().getName();
    }

    private void loaded(SWEKSession.EventDetails loaded) {
        if (closed)
            return;
        Colors.Data colors = new Colors.Data();
        for (SolarEvent event : loaded.relatedEvents()) {
            RelatedEvents group = session.getRelatedEvents(event.getUniqueID());
            other.add(new Listed(group == null ? new RelatedEvents(event, colors.getNextColor()) : group, event));
        }
        // The stored event carries the full parameter set.
        standard.replace(loaded.event().getVisibleEventParameters());
        all.replace(loaded.event().getAllEventParameters());
        fit++;
        listener.run();
    }

    private void failed(String ignoredLogContext, Throwable t) {
        if (!closed && !AppThread.isInterrupted(t))
            Log.error(t);
    }

    // The window closed: a result still in flight has nowhere to go.
    public void close() {
        closed = true;
        listener = () -> {};
        if (hovered != null) {
            hovered = null;
            session.highlight(null);
        }
    }

    @Override
    public void listen(Runnable _listener) {
        listener = _listener;
    }

    @Override
    public Panel build() {
        Panel p = buffer.reset();
        description(p, summary);
        p.fit(fit);
        table(p, 0, standard);
        table(p, 1, all);
        int owner = EVENTS;
        if (!related.isEmpty())
            owner = events(p, RELATED, related, owner);
        if (!other.isEmpty())
            events(p, OTHER, other, owner);
        return p.owner(MAIN);
    }

    private void description(Panel p, Listed listed) {
        String group = listed.event.getSupplier().group().getIconKey();
        p.row().hover(HOVER).space(0, 0).sizePoints(ICON_MARGIN, ICON_SIZE).image(SWEKIconBank.png(group), ICON_SIZE).space(0, 0).sizePoints(ICON_GAP, ICON_SIZE).column();
        time(p, 1, "Start Time", listed.event.start);
        time(p, 2, "End Time", listed.event.end);
        p.row().label("").widthText(LABEL_WIDTH).label("").widthText(COLON_WIDTH)
                .space(0, 0).sizePoints(BAR_WIDTH, BAR_HEIGHT).foreground(listed.related.getColor().getRGB() & 0xFFFFFF).rowEnd();
        p.columnEnd().space(0, 0).sizePoints(END_MARGIN, ICON_SIZE).rowEnd();
    }

    private static void time(Panel p, int id, String name, long milli) {
        p.row().label(name).widthText(LABEL_WIDTH).label(":").widthText(COLON_WIDTH)
                .text(id, TimeUtils.formatShort(milli), 0).widthText(TIME_WIDTH).readOnly(true).flat().rowEnd();
    }

    private void table(Panel p, int index, Parameters parameters) {
        p.owner(MAIN).section(SECTION + index, TITLES[index], open[index]).owner(TABLES + index);
        p.table(COLUMNS.length, 1, 0).identity(TABLE).sizePoints(0, TABLE_HEIGHT).fill().columnWidth(0, NAME_WIDTH).columnWidth(1, VALUE_WIDTH)
                .stretchColumns(1).activateColumns().doubleClickColumns().copyColumns(0, 1).multiple(true).headerRow().tableRow(0, false);
        SortKey primary = parameters.keys.getFirst();
        for (int column = 0; column < COLUMNS.length; column++) {
            p.button(SORT + column, COLUMNS[column]);
            if (primary.column == column)
                p.sorted(primary.descending);
        }
        p.rowEnd();
        for (int row : parameters.order()) {
            EventParameter parameter = parameters.rows[row];
            p.tableRow(ROW + row, parameters.selected.contains(row)).label(parameter.getParameterDisplayName());
            String link = parameter.getLink();
            if (link == null)
                p.label(cut(parameter.getSimpleDisplayParameterValue())).wrap();
            else
                p.link(linkLabel(parameter), link).wrap();
            p.rowEnd();
        }
        p.tableEnd().owner(MAIN).sectionEnd();
    }

    private int events(Panel p, int index, List<Listed> events, int firstOwner) {
        int owner = firstOwner;
        p.owner(MAIN).section(SECTION + index, TITLES[index], open[index]).scroll(0).fill();
        for (Listed listed : events) {
            p.owner(owner++);
            description(p, listed);
            p.row().right().button(DETAILS, "Details").rowEnd();
        }
        p.owner(MAIN).scrollEnd().sectionEnd();
        return owner;
    }

    private static String cut(String value) {
        return value.length() > STRING_CUTOFF ? value.substring(0, STRING_CUTOFF) + "..." : value;
    }

    private static String linkLabel(EventParameter parameter) {
        return parameter.isUrl() ? "Open URL" : parameter.getSimpleDisplayParameterValue();
    }

    // What the value column orders by.
    private static String sortValue(EventParameter parameter) {
        String link = parameter.getLink();
        return link == null ? cut(parameter.getSimpleDisplayParameterValue()) : "<a href=\"" + link + "\">" + linkLabel(parameter) + "</a>";
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (owner >= TABLES && owner < EVENTS) {
            Parameters parameters = owner == TABLES ? standard : all;
            if (item >= ROW)
                parameters.select(item - ROW, command);
            else
                parameters.toggleSort(item - SORT);
            return;
        }

        Listed listed = owner == MAIN ? summary : owner - EVENTS < related.size() ? related.get(owner - EVENTS) : other.get(owner - EVENTS - related.size());
        if (item == DETAILS)
            details.open(listed.related, listed.event);
        else if (command == Panel.Command.ENTER) {
            hovered = listed;
            session.highlight(listed.related);
        } else if (hovered == listed) {
            hovered = null;
            session.highlight(null);
        }
    }

    @Override
    public void checked(int owner, int item, boolean value) {
        int section = item - SECTION;
        open[section] = value;
        if (!value && hovered != null && ((section == RELATED && related.contains(hovered)) || (section == OTHER && other.contains(hovered)))) {
            hovered = null;
            session.highlight(null);
        }
        fit++;
    }

    // Parameters in the order of their sort keys, most significant first, ties in stored order.
    private static final class Parameters {

        private static final int MAX_SORT_KEYS = 2;

        private final ArrayList<SortKey> keys = new ArrayList<>(List.of(new SortKey(0, false)));
        private final HashSet<Integer> selected = new HashSet<>();
        private EventParameter[] rows;

        Parameters(EventParameter[] _rows) {
            rows = _rows;
        }

        void replace(EventParameter[] _rows) {
            rows = _rows;
            selected.clear();
        }

        List<Integer> order() {
            Comparator<Integer> comparator = null;
            Collator collator = Collator.getInstance();
            for (SortKey key : keys) {
                Comparator<Integer> next = Comparator.comparing(row -> cell(rows[row], key.column), collator);
                if (key.descending)
                    next = next.reversed();
                comparator = comparator == null ? next : comparator.thenComparing(next);
            }
            ArrayList<Integer> order = new ArrayList<>(rows.length);
            for (int row = 0; row < rows.length; row++)
                order.add(row);
            order.sort(comparator);
            return order;
        }

        private static String cell(EventParameter parameter, int column) {
            return column == 0 ? parameter.getParameterDisplayName() : sortValue(parameter);
        }

        void select(int row, Panel.Command command) {
            if (command == Panel.Command.SELECT_REMOVE)
                selected.remove(row);
            else {
                if (command != Panel.Command.SELECT_ADD)
                    selected.clear();
                selected.add(row);
            }
        }

        // A new column sorts first and ascending; the first column reverses; any other moves to the front ascending.
        void toggleSort(int column) {
            if (keys.getFirst().column == column) {
                keys.set(0, new SortKey(column, !keys.getFirst().descending));
                return;
            }
            keys.removeIf(key -> key.column == column);
            keys.addFirst(new SortKey(column, false));
            if (keys.size() > MAX_SORT_KEYS)
                keys.removeLast();
        }

    }

}
