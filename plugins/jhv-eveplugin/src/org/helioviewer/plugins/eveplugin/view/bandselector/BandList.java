package org.helioviewer.plugins.eveplugin.view.bandselector;

import java.awt.Color;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.BandControllerListener;
import org.helioviewer.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.plugins.eveplugin.lines.data.DownloadControllerListener;

/**
 * @author Stephan Pagel
 * */
public class BandList extends JPanel implements BandControllerListener, DownloadControllerListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private static final Color[] bandColors = { new Color(0, 0, 0), new Color(31, 26, 178), new Color(242, 159, 5), new Color(19, 137, 0), new Color(140, 64, 47), new Color(199, 0, 125), new Color(197, 230, 231), new Color(242, 92, 5), new Color(217, 37, 37), new Color(136, 166, 27), new Color(129, 0, 81), new Color(217, 136, 75), new Color(48, 110, 115), new Color(178, 156, 133), new Color(255, 83, 53), new Color(242, 209, 110), new Color(201, 255, 237), new Color(14, 61, 89), new Color(89, 115, 88), new Color(178, 3, 33), new Color(206, 224, 200), new Color(59, 66, 76), new Color(219, 108, 124) };

    private final String identifier;

    private final Color selectionBackgroundColor = new JList().getSelectionBackground();
    private final Color selectionForegroundColor = new JList().getSelectionForeground();
    private final LinkedList<BandListEntry> entryList = new LinkedList<BandListEntry>();

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public BandList(final String identifier) {
        this.identifier = identifier;

        initVisualComponents();

        BandController.getSingletonInstance().addBandControllerListener(this);
        DownloadController.getSingletonInstance().addListener(this);
    }

    private void initVisualComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    private void refillListEntries() {
        for (final BandListEntry entry : entryList) {
            remove(entry);
        }
        entryList.clear();

        final Band[] bands = BandController.getSingletonInstance().getBands(identifier);

        for (final Band band : bands) {
            addEntry(band);
        }
    }

    private void removeEntry(final Band band) {
        if (band == null)
            return;

        for (int i = 0; i < entryList.size(); i++) {
            final BandListEntry entry = entryList.get(i);

            if (entry.getBand().equals(band)) {
                remove(entry);
                entryList.remove(entry);

                selectItem(i);
            }
        }
    }

    private void addEntry(final Band band) {
        if (band == null) {
            return;
        }

        band.setGraphColor(bandColors[entryList.size() % bandColors.length]);

        final BandListEntry entry = new BandListEntry(this, band, identifier);
        add(entry);
        entryList.add(entry);
        selectItem(entryList.size() - 1);
    }

    public void selectItem(final Band band) {
        if (band == null)
            return;

        for (int i = 0; i < entryList.size(); i++) {
            final BandListEntry entry = entryList.get(i);

            if (entry.getBand().equals(band)) {
                entry.setForeground(selectionForegroundColor);
                entry.setBackground(selectionBackgroundColor);
            } else {
                entry.setForeground(Color.BLACK);
                entry.setBackground(Color.WHITE);
            }
        }
    }

    public void selectItem(final int index) {
        final int itemCount = entryList.size();

        if (itemCount == 0)
            return;

        final int pos = Math.max(0, Math.min(itemCount - 1, index));

        int i = 0;
        for (final BandListEntry entry : entryList) {
            if (i == pos) {
                entry.setForeground(selectionForegroundColor);
                entry.setBackground(selectionBackgroundColor);
            } else {
                entry.setForeground(Color.BLACK);
                entry.setBackground(Color.WHITE);
            }

            i++;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void bandAdded(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            addEntry(band);
            selectItem(band);

            repaint();
        }
    }

    public void bandRemoved(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            removeEntry(band);

            repaint();
        }
    }

    public void bandUpdated(final Band band, final String identifier) {
        if (this.identifier.equals(identifier) && band != null) {
            for (final BandListEntry entry : entryList) {
                if (entry.getBand().equals(band)) {
                    entry.updateVisualComponentValues();
                }
            }
        }
    }

    public void bandGroupChanged(final String identifier) {
        if (this.identifier.equals(identifier)) {
            refillListEntries();
            selectItem(0);
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Download Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void downloadStarted(Band band, Interval<Date> interval) {
        for (final BandListEntry entry : entryList) {
            if (entry.getBand().equals(band)) {
                entry.setDownloadActive(true);
            }
        }
    }

    public void downloadFinished(Band band, Interval<Date> interval, int activeBandDownloads) {
        if (activeBandDownloads <= 0) {
            for (final BandListEntry entry : entryList) {
                if (entry.getBand().equals(band)) {
                    entry.setDownloadActive(false);
                }
            }
        }
    }
}
