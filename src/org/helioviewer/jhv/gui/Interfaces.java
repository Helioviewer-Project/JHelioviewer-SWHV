package org.helioviewer.jhv.gui;

import java.util.List;

import javax.swing.JComponent;

public final class Interfaces {

    public interface LazyComponent {
        void lazyRepaint();
    }

    public interface MainContentPanelPlugin {
        String getTabName();

        List<JComponent> getVisualInterfaces();
    }

    public interface ShowableDialog {
        void showDialog();
    }

    public interface StatusReceiver {
        void setStatus(String status);
    }

    private Interfaces() {}
}
