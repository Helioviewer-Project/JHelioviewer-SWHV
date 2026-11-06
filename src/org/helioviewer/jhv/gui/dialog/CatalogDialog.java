package org.helioviewer.jhv.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.component.FormPanel;
import org.helioviewer.jhv.gui.time.TimeSelectorPanel;
import org.helioviewer.jhv.io.ImageRequestSettings;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.swing.SearchableUtils;

// Searches one archive shortly after its query changes and loads the selected results.
@SuppressWarnings("serial")
abstract class CatalogDialog<T> extends BaseDialog {

    private static final int SEARCH_DELAY = 500; // ms
    private static final Dimension resultSize = new Dimension(500, 250);

    protected final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final FormPanel form = new FormPanel();
    private final JList<T> listPane = new JList<>();
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton addButton = new JButton("Add");
    private final Timer searchTimer = new Timer(SEARCH_DELAY, e -> search());
    private int searchRequest;
    @Nullable
    private List<T> results; // null until a search answers

    CatalogDialog(String title) {
        super(title, true);
        searchTimer.setRepeats(false);
        ImageRequestSettings settings = ImageRequestSettings.instance();
        timeSelectorPanel.setTime(settings.start(), settings.end());
        timeSelectorPanel.addListener((start, end) -> timeChanged());
    }

    // Adds the labeled rows above the time.
    protected abstract void addQueryRows();

    // Starts the search; false when the query is not complete.
    protected abstract boolean startSearch(Consumer<List<T>> onResult, Consumer<Throwable> onFailure);

    // Loads the items; false when the user backed out.
    protected abstract boolean load(List<T> items);

    // Why the selection cannot be added, or null.
    @Nullable
    protected String refuse(List<T> selected) {
        return null;
    }

    // Appended to the selection count.
    protected String describe(List<T> selected) {
        return "";
    }

    protected String nothingFound() {
        return "Nothing found";
    }

    protected void timeChanged() {
        scheduleSearch();
    }

    protected final void addRow(String label, JComponent control) {
        form.addRow(label, control);
    }

    // Searches once the query has settled.
    protected final void scheduleSearch() {
        clearResults();
        setStatus("Searching...");
        searchTimer.restart();
    }

    // Drops the results and any search in flight.
    protected final void clearResults() {
        searchRequest++;
        searchTimer.stop();
        setResults(null);
    }

    protected final void setStatus(String status) {
        statusLabel.setText(status);
    }

    private void search() {
        int request = searchRequest;
        if (!startSearch(list -> {
            if (request == searchRequest)
                setResults(list);
        }, t -> {
            if (request == searchRequest)
                setStatus("Search failed: " + t.getMessage());
        }))
            setStatus(" ");
    }

    private void setResults(@Nullable List<T> list) {
        results = list;
        listPane.setListData(list == null ? new Vector<>() : new Vector<>(list));
        if (list != null && !list.isEmpty())
            listPane.setSelectionInterval(0, list.size() - 1);
        else
            selectionChanged();
    }

    // Without results the status belongs to whoever set it last.
    private void selectionChanged() {
        List<T> selected = listPane.getSelectedValuesList();
        String reason = selected.isEmpty() ? null : refuse(selected);
        addButton.setText(selected.isEmpty() ? "Add" : "Add " + selected.size());
        addButton.setEnabled(!selected.isEmpty() && reason == null);
        if (results == null)
            return;
        int found = results.size();
        if (reason != null)
            setStatus(reason);
        else if (found == 0)
            setStatus(nothingFound());
        else
            setStatus(found + " found, " + (selected.size() == found ? "all" : selected.size()) + " selected" + describe(selected));
    }

    @Override
    public JComponent createContentPanel() {
        addQueryRows();
        timeSelectorPanel.setBorder(null); // the sidebar indent
        addRow("Time", timeSelectorPanel);

        listPane.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                selectionChanged();
        });
        SearchableUtils.installSearchable(listPane);
        JScrollPane scrollPane = new JScrollPane(listPane);
        scrollPane.setPreferredSize(resultSize);

        JPanel content = new JPanel(new BorderLayout(0, GAP));
        content.add(form, BorderLayout.PAGE_START);
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.PAGE_END);
        return content;
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = ComponentUtils.hideAction(this);
        setDefaultCancelAction(close);

        addButton.setEnabled(false);
        addButton.addActionListener(e -> {
            List<T> items = listPane.getSelectedValuesList();
            if (!items.isEmpty() && refuse(items) == null && load(items))
                setVisible(false);
        });

        JButton cancelButton = new JButton(close);
        cancelButton.setText("Cancel");

        ButtonPanel panel = new ButtonPanel();
        panel.add(addButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelButton, ButtonPanel.CANCEL_BUTTON);
        return panel;
    }

}
