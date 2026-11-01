package org.helioviewer.jhv.ui;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

// The Swing time selector: an interval, its two fields, the shift buttons with their units and the Carrington rotation list.
public final class TimeSelection {

    // The control ids of a time selector.
    public record Ids(int start, int end, int startDate, int endDate, int back, int backUnit, int fore, int foreUnit, int carrington) {}

    public static final long[] SHIFTS = {TimeUtils.DAY_IN_MILLIS, 7 * TimeUtils.DAY_IN_MILLIS, Math.round(Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS)};

    // The width of a date field, as "2025-01-15T00:00:00" plus its margins.
    private static final int DATE_COLUMNS = 21;
    private static final int INDENT_POINTS = 20;
    private static final String ICON_SKIP_BACKWARD = "";
    private static final String ICON_SKIP_FORWARD = "";
    private static final String[] SHIFT_UNITS = {"Day", "Week", "Rotation"};
    public static final String[] ROTATIONS = rotations();
    public static final String[] ROTATION_SPANS = rotationSpans();

    private static final Ids DEFAULT_IDS = new Ids(1, 2, 3, 4, 5, 6, 7, 8, 9);
    // Screens keep their own ids from here.
    static final int NEXT_ID = 10;

    private static String[] rotations() {
        String[] names = new String[Carrington.CR_start.length];
        for (int i = 0; i < names.length; i++)
            names[i] = Integer.toString(Carrington.CR_MINIMAL + i);
        return names;
    }

    private static String[] rotationSpans() {
        String[] spans = new String[Carrington.CR_start.length];
        for (int i = 0; i < spans.length; i++)
            spans[i] = TimeUtils.formatShort(Carrington.CR_start[i]) + " - " + TimeUtils.formatShort(rotationEnd(i));
        return spans;
    }

    public static long rotationEnd(int rotation) {
        return rotation + 1 < Carrington.CR_start.length ? Carrington.CR_start[rotation + 1] : TimeUtils.MAXIMAL_TIME.milli;
    }

    // The rotation of a time, as an index of ROTATIONS.
    public static int rotation(long time) {
        return Math.clamp(Math.round(Carrington.time2CR(new JHVTime(time)) - Carrington.CR_MINIMAL), 0, ROTATIONS.length - 1);
    }

    // The start field beside the back split button, the end field beside the forward split button and the Carrington picker, indented.
    // The caller closes the grid and the row.
    public static Panel fields(Panel p, Ids ids, long start, long end, int backUnit, int foreUnit, boolean enabled) {
        return fields(p, ids, start, end, backUnit, foreUnit, enabled, DATE_COLUMNS);
    }

    private static Panel fields(Panel p, Ids ids, long start, long end, int backUnit, int foreUnit, boolean enabled, int columns) {
        return p.row().enabled(enabled).space(0, 0).sizePoints(INDENT_POINTS, 0).grid(3).identity(1).stretch(0)
                .timeField(ids.start, ids.startDate, TimeUtils.formatShort(start), columns, start,
                        TimeUtils.MINIMAL_TIME.milli, TimeUtils.MAXIMAL_TIME.milli).tip("Select start date")
                .splitButton(ids.back, ids.backUnit, ICON_SKIP_BACKWARD, SHIFT_UNITS, backUnit).flat().tip("Move time interval backward")
                .space(0, 0).endRow()
                .timeField(ids.end, ids.endDate, TimeUtils.formatShort(end), columns, end,
                        TimeUtils.MINIMAL_TIME.milli, TimeUtils.MAXIMAL_TIME.milli).tip("Select end date")
                .splitButton(ids.fore, ids.foreUnit, ICON_SKIP_FORWARD, SHIFT_UNITS, foreUnit).flat().tip("Move time interval forward")
                .comboButton(ids.carrington, "CR", ROTATIONS, rotation(start)).choiceTips(ROTATION_SPANS).tip("Select Carrington rotation").endRow();
    }

    // The picked day with the time of day of the current value.
    public static long withDate(long current, long day) {
        return day + Math.floorMod(current, TimeUtils.DAY_IN_MILLIS);
    }

    private final Ids ids;
    private final Runnable changed;
    private long start;
    private long end;
    private int backUnit;
    private int foreUnit;

    // The action runs after each change of the interval, as a listener of the Swing panel.
    TimeSelection(Runnable _changed) {
        this(DEFAULT_IDS, _changed);
    }

    TimeSelection(Ids _ids, Runnable _changed) {
        ids = _ids;
        changed = _changed;
        long milli = TimeUtils.START.milli;
        start = TimeUtils.floorSec(milli - 2 * TimeUtils.DAY_IN_MILLIS);
        end = TimeUtils.floorSec(milli);
    }

    long start() {
        return start;
    }

    long end() {
        return end;
    }

    private static long accepted(long time, long current) {
        return time >= TimeUtils.MINIMAL_TIME.milli && time <= TimeUtils.MAXIMAL_TIME.milli ? TimeUtils.floorSec(time) : current;
    }

    // A field keeps its time for a value out of range.
    void set(long _start, long _end) {
        start = accepted(_start, start);
        end = accepted(Math.max(_start, _end), end);
        changed.run();
    }

    private void commitStart(long time) {
        if (accepted(time, start) != start)
            set(time, end);
    }

    private void commitEnd(long time) {
        if (accepted(time, end) != end)
            set(start, time);
    }

    void build(Panel p, boolean enabled) {
        fields(p, ids, start, end, backUnit, foreUnit, enabled).gridEnd().rowEnd();
    }

    Panel fields(Panel p, int columns) {
        return fields(p, ids, start, end, backUnit, foreUnit, true, columns);
    }

    void build(Panel p, boolean enabled, int columns) {
        fields(p, ids, start, end, backUnit, foreUnit, enabled, columns).gridEnd().rowEnd();
    }

    boolean command(int item) {
        if (item == ids.back)
            set(start - SHIFTS[backUnit], end - SHIFTS[backUnit]);
        else if (item == ids.fore)
            set(start + SHIFTS[foreUnit], end + SHIFTS[foreUnit]);
        else
            return false;
        return true;
    }

    boolean choice(int item, int value) {
        if (item == ids.backUnit)
            backUnit = Panel.choiceIndex(value, SHIFTS.length);
        else if (item == ids.foreUnit)
            foreUnit = Panel.choiceIndex(value, SHIFTS.length);
        else if (item == ids.carrington)
            set(Carrington.CR_start[Panel.choiceIndex(value, Carrington.CR_start.length)], rotationEnd(value));
        else
            return false;
        return true;
    }

    // Swing commits a whole time, the native host a picked day.
    boolean number(int item, double value) {
        if (item == ids.start)
            commitStart((long) value);
        else if (item == ids.end)
            commitEnd((long) value);
        else if (item == ids.startDate)
            commitStart(withDate(start, (long) value));
        else if (item == ids.endDate)
            commitEnd(withDate(end, (long) value));
        else
            return false;
        return true;
    }

    // Unparsable text keeps the time.
    boolean setText(int item, String text) {
        if (item == ids.start)
            commitStart(TimeUtils.optParse(text, start));
        else if (item == ids.end)
            commitEnd(TimeUtils.optParse(text, end));
        else
            return false;
        return true;
    }

}
