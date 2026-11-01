package org.helioviewer.jhv.gui.component;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Consumer;

import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

import com.jidesoft.swing.SearchableUtils;

// A list of items. The screen owns rows and selection.
@SuppressWarnings("serial")
final class PanelList extends JList<String> {

    private final Screen screen;
    private final Consumer<Runnable> act;
    private final int owner;

    private String[] labels = {};
    private String[] tips = {};
    private int[] items = {};
    private BitSet selected = new BitSet();

    PanelList(Panel p, int control, Screen _screen, Consumer<Runnable> _act) {
        screen = _screen;
        act = _act;
        owner = p.value(control, Panel.IntField.OWNER);
        setSelectionMode(p.has(control, Panel.MULTIPLE) ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        setFixedCellHeight(p.value(control, Panel.IntField.ROW_HEIGHT));
        if (p.has(control, Panel.SEARCHABLE))
            SearchableUtils.installSearchable(this);
        addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                act.accept(this::reportSelection);
        });
    }

    private void reportSelection() {
        for (int row = 0; row < items.length; row++) {
            boolean shown = isSelectedIndex(row);
            if (shown != selected.get(row))
                screen.command(owner, items[row], shown ? Panel.Command.SELECT_ADD : Panel.Command.SELECT_REMOVE);
        }
    }

    void update(Panel p, int control) {
        int rows = p.listEnd(control) - control - 1;
        String[] newLabels = new String[rows];
        String[] newTips = new String[rows];
        int[] newItems = new int[rows];
        BitSet newSelected = new BitSet(rows);
        for (int r = 0; r < rows; r++) {
            int row = control + 1 + r;
            if (p.kind(row) != Panel.Kind.ITEM)
                throw new IllegalArgumentException("Swing list has no " + p.kind(row));
            newItems[r] = p.value(row, Panel.IntField.ID);
            newSelected.set(r, p.has(row, Panel.SELECTED));
            newLabels[r] = p.string(row, Panel.IntField.LABEL);
            newTips[r] = p.string(row, Panel.IntField.TIP);
        }

        items = newItems;
        tips = newTips;
        selected = newSelected;
        if (!Arrays.equals(labels, newLabels)) {
            labels = newLabels;
            setListData(labels);
        }
        ListSelectionModel model = getSelectionModel();
        if (model.getValueIsAdjusting())
            return;
        model.setValueIsAdjusting(true);
        for (int r = 0; r < rows; r++) {
            if (isSelectedIndex(r) != selected.get(r)) {
                if (selected.get(r))
                    model.addSelectionInterval(r, r);
                else
                    model.removeSelectionInterval(r, r);
            }
        }
        model.setValueIsAdjusting(false);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = locationToIndex(e.getPoint());
        Rectangle bounds = row < 0 ? null : getCellBounds(row, row);
        return bounds == null || !bounds.contains(e.getPoint()) || tips[row].isEmpty() ? null : tips[row];
    }

}
