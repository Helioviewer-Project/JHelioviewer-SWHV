package org.helioviewer.jhv.plugins.swek;

import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;

final class SWEKDialogs implements AutoCloseable {

    final SWEKSession session;
    private final HashMap<SWEKSupplier, FilterDialog> filters = new HashMap<>();
    private final Set<SWEKEventInformationDialog> information = new HashSet<>();
    private boolean closed;

    SWEKDialogs(SWEKSession _session) {
        session = _session;
    }

    void showFilter(SWEKSupplier supplier, Point location) {
        if (closed) return;
        FilterDialog dialog = filters.computeIfAbsent(supplier, value -> new FilterDialog(session, value));
        dialog.setLocation(location.x, location.y - dialog.getHeight());
        dialog.setVisible(true);
    }

    @Nullable
    SWEKEventInformationDialog createInformation(RelatedEvents related, SolarEvent event) {
        if (closed) return null;
        SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(this, related, event);
        information.add(dialog);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                information.remove(dialog);
            }
        });
        return dialog;
    }

    @Override
    public void close() {
        closed = true;
        filters.values().forEach(FilterDialog::dispose);
        filters.clear();
        for (SWEKEventInformationDialog dialog : List.copyOf(information))
            dialog.dispose();
        information.clear();
    }
}
