package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;

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

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;

        c.weightx = 1;
        c.gridx = 0;
        add(startField, c);
        c.weightx = 0;
        c.gridx = 1;
        add(backButton, c);

        c.gridy = 1;

        c.weightx = 1;
        c.gridx = 0;
        add(endField, c);
        c.weightx = 0;
        c.gridx = 1;
        add(foreButton, c);
        c.weightx = 0;
        c.gridx = 2;
        add(carringtonPicker, c);
    }

    private void shiftSpan(long shift) {
        setTime(getStartTime() + shift, getEndTime() + shift);
    }

    private void timeChanged() {
        setTime(getStartTime(), getEndTime());
    }

    private void carringtonChanged() {
        long time = carringtonPicker.getTime();
        int cr = (int) Math.round(Carrington.time2CR(new JHVTime(time)) - Carrington.CR_MINIMAL) + 1;
        setTime(time, Carrington.CR_start[Math.min(cr, Carrington.CR_start.length - 1)]);
    }

    public void setTime(long start, long end) {
        long finalEnd = Math.max(start, end); // maybe popup error
        carringtonPicker.setTime(start);
        startField.setTime(start);
        endField.setTime(finalEnd);
        listeners.forEach(listener -> listener.timeSelectionChanged(start, finalEnd));
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
