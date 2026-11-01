package org.helioviewer.jhv.gui.time;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.ui.TimeSelectorScreen;

@SuppressWarnings("serial")
public final class TimeSelectorPanel extends JPanel {

    private final TimeSelectorScreen screen = new TimeSelectorScreen();
    private final SwingPanel content = new SwingPanel(screen);

    public TimeSelectorPanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(content, c);
    }

    public void setTime(long start, long end) {
        screen.setTime(start, end);
        content.refresh();
    }

    public long getStartTime() {
        return screen.getStartTime();
    }

    public long getEndTime() {
        return screen.getEndTime();
    }

    public void addListener(TimeListener.Selection listener) {
        screen.addListener(listener);
    }
}
