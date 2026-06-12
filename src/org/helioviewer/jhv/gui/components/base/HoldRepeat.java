package org.helioviewer.jhv.gui.components.base;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Timer;

public final class HoldRepeat {

    public static void install(AbstractButton button, int repeatMs) {
        Timer timer = new Timer(repeatMs, e -> {
            if (button.isEnabled())
                button.doClick(0);
        });
        timer.setInitialDelay(repeatMs);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled() && !timer.isRunning())
                    timer.start();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                stop();
            }

            private void stop() {
                timer.stop();
            }
        });
    }

    private HoldRepeat() {}
}
