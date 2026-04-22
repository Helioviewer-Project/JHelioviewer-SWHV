package org.helioviewer.jhv.gui.components.base;

import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;

import org.helioviewer.jhv.threads.JHVThread;

public final class HoldRepeat {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new JHVThread.NamedThreadFactory("JHV-HoldRepeat"));

    private HoldRepeat() {}

    public static void install(AbstractButton button, int repeatMs) {
        final ScheduledFuture<?>[] future = {null};
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!button.isEnabled() || future[0] != null)
                    return;

                future[0] = executor.scheduleAtFixedRate(() -> EventQueue.invokeLater(() -> {
                    if (button.isEnabled())
                        button.doClick(0);
                }), repeatMs, repeatMs, TimeUnit.MILLISECONDS);
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
                if (future[0] != null) {
                    future[0].cancel(false);
                    future[0] = null;
                }
            }
        });
    }

}
