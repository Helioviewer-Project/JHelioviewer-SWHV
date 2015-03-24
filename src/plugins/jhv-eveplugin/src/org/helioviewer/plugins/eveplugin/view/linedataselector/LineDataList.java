package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.BandColors;

/**
 * @author Stephan Pagel
 * */
public class LineDataList extends JPanel implements LineDataSelectorModelListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final Color selectionBackgroundColor = new JList().getSelectionBackground();
    private final Color selectionForegroundColor = new JList().getSelectionForeground();
    private final LinkedList<LineDataListEntry> entryList = new LinkedList<LineDataListEntry>();

    private final LineDataSelectorModel model;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public LineDataList() {
        model = LineDataSelectorModel.getSingletonInstance();
        initVisualComponents();

        model.addLineDataSelectorModelListener(this);
    }

    private void initVisualComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
    }

    private void refillListEntries() {
        for (final LineDataListEntry entry : entryList) {
            remove(entry);
        }
        entryList.clear();

        final List<LineDataSelectorElement> elements = model.getAllLineDataSelectorElements();

        for (final LineDataSelectorElement el : elements) {
            addEntry(el);
        }
    }

    private void removeEntry(final Band band) {
        if (band == null) {
            return;
        }

        for (int i = 0; i < entryList.size(); i++) {
            final LineDataListEntry entry = entryList.get(i);

            if (entry.getLineDataSelectorElement().equals(band)) {
                remove(entry);
                entryList.remove(entry);
                BandColors.resetColor(entry.getLineDataSelectorElement().getDataColor());
                selectItem(i);
            }
        }
        repaint();
    }

    private void addEntry(final LineDataSelectorElement element) {
        if (element == null) {
            return;
        }

        element.setDataColor(BandColors.getNextColor());
        final LineDataListEntry entry = new LineDataListEntry(this, element);
        add(entry);
        entryList.add(entry);
        selectItem(entryList.size() - 1);
    }

    public void selectItem(final LineDataSelectorElement element) {
        if (element == null) {
            return;
        }

        for (int i = 0; i < entryList.size(); i++) {
            final LineDataListEntry entry = entryList.get(i);

            if (entry.getLineDataSelectorElement().equals(element)) {
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

        if (itemCount == 0) {
            return;
        }

        final int pos = Math.max(0, Math.min(itemCount - 1, index));

        int i = 0;
        for (final LineDataListEntry entry : entryList) {
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

    public void bandAdded(final Band band) {

    }

    public void bandRemoved(final Band band) {
        removeEntry(band);

        repaint();

    }

    public void bandUpdated(final Band band) {
        if (band != null) {
            for (final LineDataListEntry entry : entryList) {
                if (entry.getLineDataSelectorElement().equals(band)) {
                    entry.updateVisualComponentValues();
                }
            }
        }
    }

    public void bandGroupChanged() {

        refillListEntries();
        selectItem(0);

    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
        for (final LineDataListEntry entry : entryList) {
            if (entry.getLineDataSelectorElement().equals(element)) {
                entry.setDownloadActive(true);
            }
        }
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
        for (final LineDataListEntry entry : entryList) {
            if (entry.getLineDataSelectorElement().equals(element)) {
                entry.setDownloadActive(false);
            }
        }
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {

        addEntry(element);
        selectItem(element);

        repaint();

    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        LineDataListEntry toRemove = null;
        if (element != null) {
            for (final LineDataListEntry entry : entryList) {
                if (entry.getLineDataSelectorElement().equals(element)) {
                    entry.updateVisualComponentValues();
                    toRemove = entry;
                    break;
                }
            }
        }
        if (toRemove != null) {
            entryList.remove(toRemove);
            // Log.debug("Remove entry from compnent.");
            remove(toRemove);
            repaint();
        }
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        if (element != null) {
            for (final LineDataListEntry entry : entryList) {
                if (entry.getLineDataSelectorElement().equals(element)) {
                    entry.updateVisualComponentValues();
                }
            }
        }
    }
}
