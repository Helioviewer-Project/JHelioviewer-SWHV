package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVSplitButton;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class TimeSelectorPanel extends JPanel {

    private enum ShiftUnit {
        Day(TimeUtils.DAY_IN_MILLIS), Week(7 * TimeUtils.DAY_IN_MILLIS), Rotation(Math.round(Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS));

        final long shift;

        ShiftUnit(long _shift) {
            shift = _shift;
        }
    }

    private static ButtonGroup createShiftMenu(JHVSplitButton menu) {
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

    private final ArrayList<TimeSelectorListener> listeners = new ArrayList<>();
    private final TimePanel startTimePanel = new TimePanel("Select start date");
    private final TimePanel endTimePanel = new TimePanel("Select end date");
    private final CarringtonPicker carringtonPicker = new CarringtonPicker();

    public TimeSelectorPanel() {
        long milli = TimeUtils.START.milli;
        setTime(milli - 2 * TimeUtils.DAY_IN_MILLIS, milli);

        startTimePanel.addListener(this::timeChanged);
        endTimePanel.addListener(this::timeChanged);
        carringtonPicker.addListener(this::carringtonChanged);

        JHVSplitButton backButton = new JHVSplitButton(Buttons.skipBack);
        backButton.setMargin(new Insets(0, 0, 0, 0));
        backButton.setToolTipText("Move time interval backward");
        ButtonGroup backGroup = createShiftMenu(backButton);
        backButton.addActionListener(e -> shiftSpan(-ShiftUnit.valueOf(backGroup.getSelection().getActionCommand()).shift));

        JHVSplitButton foreButton = new JHVSplitButton(Buttons.skipFore);
        foreButton.setMargin(new Insets(0, 0, 0, 0));
        foreButton.setToolTipText("Move time interval forward");
        ButtonGroup foreGroup = createShiftMenu(foreButton);
        foreButton.addActionListener(e -> shiftSpan(ShiftUnit.valueOf(foreGroup.getSelection().getActionCommand()).shift));

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;

        c.weightx = 0;
        c.gridx = 0;
        add(backButton, c);
        c.weightx = 1;
        c.gridx = 1;
        add(startTimePanel, c);
        c.weightx = 0;
        c.gridx = 2;
        add(carringtonPicker, c);

        c.gridy = 1;

        c.weightx = 0;
        c.gridx = 0;
        add(foreButton, c);
        c.weightx = 1;
        c.gridx = 1;
        add(endTimePanel, c);
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
        carringtonPicker.setTime(start);
        startTimePanel.setTime(start);
        endTimePanel.setTime(end);

        listeners.forEach(listener -> listener.timeSelectionChanged(start, end));
    }

    public long getStartTime() {
        return startTimePanel.getTime();
    }

    public long getEndTime() {
        return endTimePanel.getTime();
    }

    public void addListener(TimeSelectorListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
