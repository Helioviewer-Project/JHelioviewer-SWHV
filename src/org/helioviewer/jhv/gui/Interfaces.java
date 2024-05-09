package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.util.List;

import javax.swing.JComponent;

public class Interfaces {

    public interface JHVCell {
        Component getComponent();
    }

    public interface LazyComponent {
        void lazyRepaint();
    }

    public interface MainContentPanelPlugin {
        String getTabName();

        List<JComponent> getVisualInterfaces();
    }

    public interface ObservationSelector {
        int getCadence();

        void setTime(long start, long end);

        long getStartTime();

        long getEndTime();

        void load(String server, int sourceId);

        void setAvailabilityEnabled(boolean enable);
    }

    public interface ShowableDialog {
        void showDialog();
    }

    public interface StatusReceiver {
        void setStatus(String status);
    }

}
