package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.AwtInputAdapter;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.layers.selector.LayerOptions;
import org.helioviewer.jhv.plugins.swek.EventTimelineLayer;
import org.helioviewer.jhv.plugins.swek.SWEKLayer;

// Swing side of SWEK: tree pane, filter and information windows, canvas cursor, layer options.
public final class SWEKSwingUI implements SWEKLayer.CanvasUi, EventTimelineLayer.Ui {

    private static final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    final SWEKSession session;
    private final SWEKTreePane treePane;
    private final HashMap<SWEKSupplier, FilterDialog> filters = new HashMap<>();
    private final Set<SWEKEventInformationDialog> information = new HashSet<>();
    private Cursor lastCursor;
    private boolean closed;

    public SWEKSwingUI(SWEKSession _session) {
        session = _session;
        treePane = new SWEKTreePane(session, this);
    }

    public void install() {
        MainFrame.getLayersPanel().setEventsPanel(treePane);
        LayerOptions.register(SWEKLayer.class, SWEKSwingUI::createLayerOptions);
    }

    private static JPanel createLayerOptions(SWEKLayer layer) {
        JCheckBox check = new JCheckBox("Icons", layer.isIcons());
        check.setHorizontalTextPosition(SwingConstants.LEFT);
        check.addActionListener(e -> layer.setIcons(check.isSelected()));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        panel.add(check);
        return panel;
    }

    public void close() {
        closed = true;
        LayerOptions.unregister(SWEKLayer.class);
        filters.values().forEach(FilterDialog::dispose);
        filters.clear();
        for (SWEKEventInformationDialog dialog : List.copyOf(information))
            dialog.dispose();
        information.clear();

        MainFrame.getLayersPanel().setEventsPanel(null);
    }

    void showFilter(SWEKSupplier supplier, Point location) {
        if (closed) return;
        FilterDialog dialog = filters.computeIfAbsent(supplier, value -> new FilterDialog(session, value));
        dialog.setLocation(location.x, location.y - dialog.getHeight());
        dialog.setVisible(true);
    }

    private boolean showInformation(RelatedEvents related, SolarEvent event, Consumer<SWEKEventInformationDialog> position) {
        if (closed) return false;
        SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(this, related, event);
        information.add(dialog);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                information.remove(dialog);
            }
        });
        dialog.pack();
        position.accept(dialog);
        dialog.setVisible(true);
        return true;
    }

    void open(RelatedEvents related, SolarEvent event) {
        showInformation(related, event, _ -> {});
    }

    @Override
    public void open(RelatedEvents related, SolarEvent event, Point locationOnScreen) {
        showInformation(related, event, dialog -> dialog.setLocation(locationOnScreen));
    }

    @Override
    public void open(RelatedEvents related, SolarEvent event, PointerEvent e) {
        boolean opened = showInformation(related, event, dialog -> {
            Component canvas = MainFrame.getRenderComponent();
            dialog.setLocation(calcWindowPosition(canvas, AwtInputAdapter.toAwtPoint(e), dialog.getWidth(), dialog.getHeight()));
        });
        if (opened)
            MainFrame.getRenderComponent().setCursor(handCursor);
    }

    @Override
    public void setHand(boolean hand) {
        Component canvas = MainFrame.getRenderComponent();
        Cursor cursor = canvas.getCursor();
        if (handCursor != cursor)
            lastCursor = cursor;
        canvas.setCursor(hand ? handCursor : lastCursor != null ? lastCursor : Cursor.getDefaultCursor());
    }

    private static Point calcWindowPosition(Component component, Point p, int hekWidth, int hekHeight) {
        int compWidth = component.getWidth();
        int compHeight = component.getHeight();
        Point compLoc = component.getLocationOnScreen();
        int compLocX = compLoc.x;
        int compLocY = compLoc.y;

        boolean yCoordInMiddle = false;
        int yCoord;
        if (p.y + hekHeight + yOffset < compHeight) {
            yCoord = p.y + compLocY + yOffset;
        } else {
            yCoord = p.y + compLocY - hekHeight - yOffset;
            if (yCoord < compLocY) {
                yCoord = compLocY + compHeight - hekHeight;
                if (yCoord < compLocY) {
                    yCoord = compLocY;
                }
                yCoordInMiddle = true;
            }
        }

        int xCoord;
        if (p.x + hekWidth + xOffset < compWidth) {
            xCoord = p.x + compLocX + xOffset;
        } else {
            xCoord = p.x + compLocX - hekWidth - xOffset;
            if (xCoord < compLocX && !yCoordInMiddle) {
                xCoord = compLocX + compWidth - hekWidth;
            }
        }

        return new Point(xCoord, yCoord);
    }

}
