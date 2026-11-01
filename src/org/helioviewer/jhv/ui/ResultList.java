package org.helioviewer.jhv.ui;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

// The results of an archive search as the Swing dialogs show them: the found count over a list of multiple selection.
final class ResultList<T> {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 350;
    private static final int ROW_HEIGHT = 17;
    private static final int FIRST_ROW = 1000;
    private static final String NONE = "0 found";

    private final Function<T, String> label;
    private final Function<T, String> tip;
    private final BitSet selected = new BitSet();
    private List<T> items = List.of();
    private String found = NONE;
    private boolean searching;

    ResultList(Function<T, String> _label, Function<T, String> _tip) {
        label = _label;
        tip = _tip;
    }

    void clear() {
        items = List.of();
        selected.clear();
        found = NONE;
        searching = false;
    }

    void searching() {
        found = "Searching...";
        searching = true;
    }

    boolean isSearching() {
        return searching;
    }

    void show(List<T> _items, String _found) {
        items = List.copyOf(_items);
        selected.clear();
        found = _found;
        searching = false;
    }

    // The selection in listing order.
    List<T> selection() {
        List<T> selection = new ArrayList<>(selected.cardinality());
        for (int row = selected.nextSetBit(0); row >= 0; row = selected.nextSetBit(row + 1))
            selection.add(items.get(row));
        return selection;
    }

    int selectedCount() {
        return selected.cardinality();
    }

    // A right-aligned line, as the FlowLayout panels around the Swing list.
    static Panel line(Panel p, String text) {
        return p.row().right().gap(5).margin(0, 5, 0, 5).label(text).rowEnd();
    }

    void build(Panel p, String selection) {
        line(p, found);
        p.list(WIDTH, HEIGHT, ROW_HEIGHT).multiple(true).searchable();
        for (int row = 0; row < items.size(); row++) {
            T item = items.get(row);
            p.item(FIRST_ROW + row, label.apply(item), selected.get(row)).tip(tip.apply(item));
        }
        line(p.listEnd(), selection);
    }

    // Whether the command belongs to a row.
    boolean command(int item, Panel.Command command) {
        int row = item - FIRST_ROW;
        if (row < 0 || row >= items.size())
            return false;
        switch (command) {
            case SELECT_ONLY, CLICK -> {
                selected.clear();
                selected.set(row);
            }
            case SELECT_ADD -> selected.set(row);
            case SELECT_REMOVE -> selected.clear(row);
            default -> {}
        }
        return true;
    }

}
