package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.CadencePanel;
import org.helioviewer.jhv.gui.component.ImageSelectorPanel;
import org.helioviewer.jhv.gui.component.MoviePanel;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.timelines.draw.DrawController;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;

@SuppressWarnings("serial")
public final class LayersSectionPanel extends JPanel implements Interfaces.ObservationSelector {

    private final CadencePanel cadencePanel;
    private final ImageSelectorPanel imageSelectorPanel;
    private final JideSplitButton addLayerButton;

    public LayersSectionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // request cadence for the next layer, sourced against the master time range
        cadencePanel = new CadencePanel(MoviePanel.getInstance().getTimeSelectorPanel());
        imageSelectorPanel = new ImageSelectorPanel(this);

        addLayerButton = new JideSplitButton(Buttons.newLayer);
        addLayerButton.setAlwaysDropdown(true);
        addLayerButton.add(imageSelectorPanel);
        addLayerButton.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                EventQueue.invokeLater(() -> imageSelectorPanel.getFocused().grabFocus());
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        JPanel addLayerRow = new JPanel(new BorderLayout());
        addLayerRow.add(addLayerButton, BorderLayout.LINE_START);
        addLayerRow.add(cadencePanel, BorderLayout.CENTER);

        add(addLayerRow);
        add(MainFrame.getLayersPanel());
    }

    // The Sync button lives next to the time range (in ImageLayersPane) and calls this.
    public void syncLayers() {
        syncLayersSpan(getStartTime(), getEndTime());
    }

    // Entry point for external callers that need to move the master range to a given
    // span and resync all layers to it (e.g. a layer's own "sync" button, or the
    // timeline widget snapping the movie to its locked selection).
    public void syncLayersSpan(long start, long end) {
        setTime(start, end);
        if (checkSanity()) {
            DrawController.setSelectedInterval(getStartTime(), getEndTime());
            ImageLayers.syncLayersSpan(getStartTime(), getEndTime(), getCadence());
        }
    }

    private boolean checkSanity() {
        long start = getStartTime();
        long end = getEndTime();
        if (start > end) {
            setTime(end, end);
            JOptionPane.showMessageDialog(null, "End date is before start date", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public int getCadence() {
        return cadencePanel.getCadence();
    }

    @Override
    public void setTime(long start, long end) {
        MoviePanel.getInstance().setTime(start, end);
    }

    @Override
    public long getStartTime() {
        return MoviePanel.getInstance().getStartTime();
    }

    @Override
    public long getEndTime() {
        return MoviePanel.getInstance().getEndTime();
    }

    @Override
    public void load(String server, int sourceId) {
        addLayerButton.doClickOnMenu();
        if (checkSanity()) {
            imageSelectorPanel.load(null, server, sourceId, getStartTime(), getEndTime(), getCadence());
        }
    }

    @Override
    public void setAvailabilityEnabled(boolean enable) {}
}
