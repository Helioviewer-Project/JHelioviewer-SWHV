package org.helioviewer.jhv.gui.component;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.function.IntConsumer;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;

import com.jidesoft.swing.JideButton;

// A flat button that opens its choices in a scrolling list, each with a tooltip. Every pick reports, even of the current choice.
@SuppressWarnings("serial")
public final class ChoicePicker extends JideButton {

    private final IntConsumer onSelect;
    private final JPopupMenu popup = new JPopupMenu();
    private final JList<String> list = new JList<>() {
        @Override
        public String getToolTipText(MouseEvent e) {
            int choice = locationToIndex(e.getPoint());
            Rectangle bounds = choice < 0 ? null : getCellBounds(choice, choice);
            return bounds == null || !bounds.contains(e.getPoint()) ? null : tips[choice];
        }
    };

    private String[] labels = {};
    private String[] tips = {};
    private int index;

    public ChoicePicker(String text, IntConsumer _onSelect) {
        onSelect = _onSelect;
        setText(text);

        list.setVisibleRowCount(15);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ToolTipManager.sharedInstance().registerComponent(list);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                select(list.locationToIndex(e.getPoint()));
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "selectChoice");
        list.getActionMap().put("selectChoice", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                select(list.getSelectedIndex());
            }
        });

        popup.add(new JScrollPane(list));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    list.setSelectedIndex(index);
                    list.ensureIndexIsVisible(index);
                    popup.show(e.getComponent(), 0, e.getComponent().getHeight());
                }
            }
        });
    }

    private void select(int choice) {
        if (choice >= 0) {
            popup.setVisible(false);
            onSelect.accept(choice);
        }
    }

    public void update(String[] _labels, String[] _tips, int _index) {
        if (!Arrays.equals(labels, _labels)) {
            labels = _labels;
            list.setListData(labels);
        }
        tips = _tips;
        index = _index;
    }

}
