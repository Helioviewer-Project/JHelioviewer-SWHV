package org.helioviewer.jhv.gui.time;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.io.Proba3Orbits;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;

@SuppressWarnings("serial")
public final class TimeSelectorPanel extends JPanel {

    private enum ShiftUnit {
        Day(TimeUtils.DAY_IN_MILLIS), Week(7 * TimeUtils.DAY_IN_MILLIS), Rotation(Math.round(Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS));

        private final long shift;

        ShiftUnit(long _shift) {
            shift = _shift;
        }
    }

    private enum Mode {
        Interval("Interval"), Carrington("Carrington rotation"), Orbit("Proba-3 orbit");

        private final String label;

        Mode(String _label) {
            label = _label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record Range(String name, long start, long end) {
        @Override
        public String toString() {
            return name;
        }
    }

    // Shifts the interval by the step picked in its menu.
    @SuppressWarnings("serial")
    private final class ShiftButton extends JideSplitButton {

        private ShiftUnit unit = ShiftUnit.Day;

        ShiftButton(String icon, int sign, String direction) {
            super(icon);
            ButtonGroup group = new ButtonGroup();
            for (ShiftUnit step : ShiftUnit.values()) {
                String tip = "Shift " + direction + " one " + step.toString().toLowerCase();
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(step.toString(), step == unit);
                item.addActionListener(e -> {
                    unit = step;
                    setToolTipText(tip);
                });
                if (step == unit)
                    setToolTipText(tip);
                group.add(item);
                add(item);
            }
            addActionListener(e -> setTime(getStartTime() + sign * unit.shift, getEndTime() + sign * unit.shift));
        }

    }

    private static final String INTERVAL_CARD = "interval";
    private static final String RANGE_CARD = "range";

    // The last start only ends the rotation before it.
    private static DefaultComboBoxModel<Range> carringtonRanges() {
        long[] starts = Carrington.CR_start;
        Range[] ranges = new Range[starts.length - 1];
        for (int i = 0; i < ranges.length; i++)
            ranges[i] = new Range(Integer.toString(i + Carrington.CR_MINIMAL), starts[i], starts[i + 1]);
        return new DefaultComboBoxModel<>(ranges);
    }

    private final ArrayList<TimeListener.Selection> listeners = new ArrayList<>();
    private final TimeField startField = new TimeField("Select start date", this::timeChanged);
    private final TimeField endField = new TimeField("Select end date", this::timeChanged);
    private final JComboBox<Mode> modeCombo = new JComboBox<>(Mode.values());
    private final JComboBox<Range> rangeCombo = new JComboBox<>();
    private final JTextField rangeText = new JTextField();
    private final JideButton prevButton = new JideButton(Buttons.skipBack);
    private final JideButton nextButton = new JideButton(Buttons.skipFore);
    private final JPanel cards = new JPanel(new CardLayout());
    private final DefaultComboBoxModel<Range> carringtons = carringtonRanges();
    private DefaultComboBoxModel<Range> orbits = new DefaultComboBoxModel<>();
    private Future<List<Proba3Orbits.Orbit>> orbitsRequest;

    public TimeSelectorPanel() {
        long milli = TimeUtils.START.milli;
        setTime(milli - 2 * TimeUtils.DAY_IN_MILLIS, milli);

        ShiftButton backButton = new ShiftButton(Buttons.skipBack, -1, "back");
        ShiftButton foreButton = new ShiftButton(Buttons.skipFore, 1, "forward");

        JPanel interval = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridheight = 2;
        c.gridx = 0;
        c.gridy = 0;
        interval.add(backButton, c);
        c.gridx = 2;
        interval.add(foreButton, c);
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 1;
        interval.add(startField, c);
        c.gridy = 1;
        interval.add(endField, c);

        rangeCombo.setPrototypeDisplayValue(new Range("9999", 0, 0));
        rangeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Range(String ignored, long start, long end))
                    label.setToolTipText(TimeUtils.format(start) + " - " + TimeUtils.format(end));
                return label;
            }
        });
        rangeCombo.addActionListener(e -> rangeSelected());

        prevButton.addActionListener(e -> stepRange(-1));
        nextButton.addActionListener(e -> stepRange(1));

        JPanel stepper = new JPanel(new BorderLayout());
        stepper.add(prevButton, BorderLayout.LINE_START);
        stepper.add(rangeCombo, BorderLayout.CENTER);
        stepper.add(nextButton, BorderLayout.LINE_END);

        rangeText.setEditable(false);
        rangeText.setHorizontalAlignment(JTextField.CENTER);

        JPanel range = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        range.add(stepper, c);
        c.gridy = 1;
        range.add(rangeText, c);

        cards.add(interval, INTERVAL_CARD);
        cards.add(range, RANGE_CARD);
        modeCombo.setToolTipText("Select how the time interval is given");
        modeCombo.addActionListener(e -> modeChanged());

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        add(modeCombo, c);
        c.gridy = 1;
        add(cards, c);
    }

    // The mode menu's baseline, so that a form label lines up with it.
    @Override
    public int getBaseline(int width, int height) {
        Dimension size = modeCombo.getPreferredSize();
        int baseline = modeCombo.getBaseline(size.width, size.height);
        return baseline < 0 ? -1 : getInsets().top + baseline;
    }

    @Override
    public BaselineResizeBehavior getBaselineResizeBehavior() {
        return BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    private Mode mode() {
        return (Mode) modeCombo.getSelectedItem();
    }

    private void modeChanged() {
        Mode mode = mode();
        ((CardLayout) cards.getLayout()).show(cards, mode == Mode.Interval ? INTERVAL_CARD : RANGE_CARD);
        if (mode == Mode.Interval)
            return;
        rangeCombo.setToolTipText("Select " + mode);
        prevButton.setToolTipText("Previous " + mode);
        nextButton.setToolTipText("Next " + mode);
        if (mode == Mode.Carrington) {
            int index = 0;
            while (index + 1 < carringtons.getSize() && carringtons.getElementAt(index + 1).start() <= getStartTime())
                index++;
            showRange(carringtons, carringtons.getElementAt(index));
        } else {
            showRange(orbits, orbits.getSelectedItem());
            if (orbits.getSize() == 0 && (orbitsRequest == null || orbitsRequest.isDone()))
                orbitsRequest = Proba3Orbits.submitLoad(this::setOrbits);
        }
    }

    // JComboBox.setSelectedItem fires even when the item is already selected.
    private void showRange(DefaultComboBoxModel<Range> model, Object item) {
        rangeCombo.setModel(model);
        rangeCombo.setSelectedItem(item);
    }

    // Carrington rotations are listed oldest first, orbits newest first.
    private void stepRange(int later) {
        int count = rangeCombo.getItemCount();
        if (count != 0)
            rangeCombo.setSelectedIndex(Math.clamp(rangeCombo.getSelectedIndex() + (mode() == Mode.Carrington ? later : -later), 0, count - 1));
    }

    private void setOrbits(List<Proba3Orbits.Orbit> list) {
        Range[] ranges = new Range[list.size()];
        for (int i = 0; i < ranges.length; i++) {
            Proba3Orbits.Orbit orbit = list.get(i);
            ranges[i] = new Range(Integer.toString(orbit.id()), orbit.start(), orbit.end());
        }
        Arrays.sort(ranges, Comparator.comparingLong(Range::start).reversed());
        orbits = new DefaultComboBoxModel<>(ranges);
        if (mode() == Mode.Orbit)
            showRange(orbits, orbits.getSelectedItem());
    }

    private void rangeSelected() {
        if (rangeCombo.getSelectedItem() instanceof Range range) {
            rangeText.setText(TimeUtils.formatShort(TimeUtils.floorSec(range.start())) + " - " + TimeUtils.formatShort(TimeUtils.floorSec(range.end())));
            setTime(range.start(), range.end());
        } else
            rangeText.setText("");
    }

    private void timeChanged() {
        setTime(getStartTime(), getEndTime());
    }

    public void setTime(long start, long end) {
        long finalEnd = Math.max(start, end);
        startField.setTime(start);
        endField.setTime(finalEnd);

        long realStart = startField.getTime();
        long realEnd = endField.getTime();
        // An interval other than the selected range is given by its ends.
        if (mode() != Mode.Interval && !(rangeCombo.getSelectedItem() instanceof Range range
                && TimeUtils.floorSec(range.start()) == realStart && TimeUtils.floorSec(range.end()) == realEnd))
            modeCombo.setSelectedItem(Mode.Interval);
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
