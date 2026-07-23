package org.helioviewer.jhv.gui.time;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;

@SuppressWarnings("serial")
public final class TimeSelectorPanel extends JPanel {

    private enum ShiftUnit {
        Day(TimeUtils.DAY_IN_MILLIS), Week(7 * TimeUtils.DAY_IN_MILLIS), Rotation(Math.round(Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS));

        final long shift;

        ShiftUnit(long _shift) {
            shift = _shift;
        }
    }

    // Quick time-span presets, ordered by real duration so the −/+ stepper walks a sensible ladder.
    private enum SpanPreset {
        MIN_1("1 min", 60_000L),
        MIN_15("15 min", 900_000L),
        MIN_30("30 min", 1_800_000L),
        HOUR_1("1 hour", 3_600_000L),
        HOUR_3("3 hours", 10_800_000L),
        HOUR_6("6 hours", 21_600_000L),
        HOUR_12("12 hours", 43_200_000L),
        HOUR_24("24 hours", 86_400_000L),
        WEEK_1("1 week", 7 * TimeUtils.DAY_IN_MILLIS),
        CR_1("1 CR", Math.round(Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS)),
        MONTH_1("1 month", 30 * TimeUtils.DAY_IN_MILLIS),
        MONTH_3("3 months", 90 * TimeUtils.DAY_IN_MILLIS),
        MONTH_6("6 months", 180 * TimeUtils.DAY_IN_MILLIS),
        YEAR_1("1 year", 365 * TimeUtils.DAY_IN_MILLIS);

        final String label;
        final long millis;

        SpanPreset(String _label, long _millis) {
            label = _label;
            millis = _millis;
        }
    }

    private static ButtonGroup createShiftMenu(JideSplitButton menu) {
        ButtonGroup group = new ButtonGroup();
        for (ShiftUnit unit : ShiftUnit.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(unit.toString());
            item.setActionCommand(unit.toString());
            if (unit == ShiftUnit.Day)
                item.setSelected(true);
            group.add(item);
            menu.add(item);
        }
        return group;
    }

    private final ArrayList<TimeListener.Selection> listeners = new ArrayList<>();
    private final TimeField startField = new TimeField("Select start date");
    private final TimeField endField = new TimeField("Select end date");
    private final CarringtonPicker carringtonPicker = new CarringtonPicker();
    private final JideButton spanButton = new JideButton(); // current span; click for the preset ladder

    public TimeSelectorPanel() {
        long milli = TimeUtils.START.milli;
        setTime(milli - 2 * TimeUtils.DAY_IN_MILLIS, milli);

        startField.addListener(this::timeChanged);
        endField.addListener(this::timeChanged);
        carringtonPicker.addListener(this::carringtonChanged);

        JideSplitButton backButton = new JideSplitButton(Buttons.skipBack);
        backButton.setMargin(new Insets(0, 0, 0, 0));
        backButton.setToolTipText("Move time interval backward");
        ButtonGroup backGroup = createShiftMenu(backButton);
        backButton.addActionListener(e -> shiftSpan(-ShiftUnit.valueOf(backGroup.getSelection().getActionCommand()).shift));

        JideSplitButton foreButton = new JideSplitButton(Buttons.skipFore);
        foreButton.setMargin(new Insets(0, 0, 0, 0));
        foreButton.setToolTipText("Move time interval forward");
        ButtonGroup foreGroup = createShiftMenu(foreButton);
        foreButton.addActionListener(e -> shiftSpan(ShiftUnit.valueOf(foreGroup.getSelection().getActionCommand()).shift));

        // Span control: −/+ step the preset ladder, the middle button opens the full ladder.
        JideButton spanDec = new JideButton("−");
        spanDec.setMargin(new Insets(0, 3, 0, 3));
        spanDec.setToolTipText("Shorter time span");
        spanDec.addActionListener(e -> stepSpan(false));

        spanButton.setMargin(new Insets(0, 3, 0, 3));
        spanButton.setToolTipText("Time span — click to pick a preset (keeps the start date fixed)");
        JPopupMenu spanMenu = new JPopupMenu();
        for (SpanPreset p : SpanPreset.values()) {
            JMenuItem item = new JMenuItem(p.label);
            item.addActionListener(e -> setTime(getStartTime(), getStartTime() + p.millis));
            spanMenu.add(item);
        }
        spanButton.addActionListener(e -> spanMenu.show(spanButton, 0, spanButton.getHeight()));

        JideButton spanInc = new JideButton("+");
        spanInc.setMargin(new Insets(0, 3, 0, 3));
        spanInc.setToolTipText("Longer time span");
        spanInc.addActionListener(e -> stepSpan(true));

        JPanel spanPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        spanPanel.add(spanDec);
        spanPanel.add(spanButton);
        spanPanel.add(spanInc);
        updateSpanLabel();

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;

        // Carrington-rotation picker as the top-left icon, just left of the start date.
        c.weightx = 0;
        c.gridx = 0;
        add(carringtonPicker, c);
        c.weightx = 1;
        c.gridx = 1;
        add(startField, c);
        c.weightx = 0;
        c.gridx = 2;
        add(backButton, c);

        c.gridy = 1;

        // Span control under the CR button, inline with the end date.
        c.weightx = 0;
        c.gridx = 0;
        add(spanPanel, c);
        c.weightx = 1;
        c.gridx = 1;
        add(endField, c);
        c.weightx = 0;
        c.gridx = 2;
        add(foreButton, c);
    }

    private void shiftSpan(long shift) {
        setTime(getStartTime() + shift, getEndTime() + shift);
    }

    // Move to the adjacent preset relative to the actual span (works even if the span is custom).
    private void stepSpan(boolean longer) {
        long start = getStartTime();
        long current = getEndTime() - start;
        SpanPreset[] all = SpanPreset.values();
        if (longer) {
            for (SpanPreset p : all)
                if (p.millis > current) {
                    setTime(start, start + p.millis);
                    return;
                }
            setTime(start, start + all[all.length - 1].millis);
        } else {
            for (int i = all.length - 1; i >= 0; i--)
                if (all[i].millis < current) {
                    setTime(start, start + all[i].millis);
                    return;
                }
            setTime(start, start + all[0].millis);
        }
    }

    private void updateSpanLabel() {
        long current = getEndTime() - getStartTime();
        SpanPreset nearest = SpanPreset.MIN_1;
        long best = Long.MAX_VALUE;
        for (SpanPreset p : SpanPreset.values()) {
            long d = Math.abs(p.millis - current);
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        // Show the preset name when the span is essentially one, otherwise the raw duration.
        spanButton.setText(best <= nearest.millis / 100 ? nearest.label : TimeUtils.formatDurationSig(current));
    }

    private void timeChanged() {
        setTime(getStartTime(), getEndTime());
    }

    private void carringtonChanged() {
        long time = carringtonPicker.getTime();
        int cr = (int) Math.round(Carrington.time2CR(new JHVTime(time)) - Carrington.CR_MINIMAL) + 1;
        long end = cr >= 0 && cr < Carrington.CR_start.length ? Carrington.CR_start[cr] : TimeUtils.MAXIMAL_TIME.milli;
        setTime(time, end);
    }

    public void setTime(long start, long end) {
        long finalEnd = Math.max(start, end); // maybe popup error
        carringtonPicker.setTime(start);
        startField.setTime(start);
        endField.setTime(finalEnd);

        long realStart = startField.getTime();
        long realEnd = endField.getTime();
        updateSpanLabel();
        listeners.forEach(listener -> listener.timeSelectionChanged(realStart, realEnd));
    }

    public long getStartTime() {
        return startField.getTime();
    }

    public long getEndTime() {
        return endField.getTime();
    }

    public void addListener(TimeListener.Selection listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
