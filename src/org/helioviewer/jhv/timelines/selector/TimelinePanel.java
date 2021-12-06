package org.helioviewer.jhv.timelines.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.TableValue;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.gui.NewLayerAction;
import org.helioviewer.jhv.timelines.selector.cellrenderer.RendererColor;
import org.helioviewer.jhv.timelines.selector.cellrenderer.RendererEnabled;
import org.helioviewer.jhv.timelines.selector.cellrenderer.RendererLoading;
import org.helioviewer.jhv.timelines.selector.cellrenderer.RendererName;
import org.helioviewer.jhv.timelines.selector.cellrenderer.RendererRemove;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class TimelinePanel extends JPanel {

    private static final int ICON_WIDTH = 12;

    private static final int ENABLED_COL = 0;
    private static final int TITLE_COL = 1;
    public static final int LOADING_COL = 2;
    private static final int LINECOLOR_COL = 3;
    private static final int REMOVE_COL = 4;

    public static final int NUMBEROFCOLUMNS = 5;
    private static final int NUMBEROFVISIBLEROWS = 5;

    private final TimelineTable grid;
    private final JPanel optionsPanelWrapper;

    private static class TimelineTable extends JTable implements LazyComponent {

        TimelineTable(TableModel tm) {
            super(tm);
            UITimer.register(this);
        }

        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend) {
            if (col != ENABLED_COL && col != REMOVE_COL)
                super.changeSelection(row, col, toggle, extend);
            // otherwise prevent changing selection
        }

        @Override
        public void clearSelection() {
            // prevent losing selection
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            super.tableChanged(e);
            if (e.getType() == TableModelEvent.INSERT) {
                int row = e.getLastRow();
                setRowSelectionInterval(row, row);
            }
        }

        @Override
        public void repaint() {
            dirty = true;
        }

        @Override
        public void repaint(int x, int y, int width, int height) {
            dirty = true;
        }

        private boolean dirty = false;

        @Override
        public void lazyRepaint() {
            if (dirty) {
                super.repaint();
                dirty = false;
            }
        }

    }

    public TimelinePanel(TimelineLayers model) {
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        grid = new TimelineTable(model);

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.lightGray));
        jsp.getViewport().setBackground(grid.getBackground());

        JideButton addLayerButton = new JideButton(Buttons.newLayer);
        addLayerButton.addActionListener(e -> new NewLayerAction().actionPerformed(new ActionEvent(addLayerButton, 0, "")));

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(addLayerButton, BorderLayout.LINE_START);
        addLayerButtonWrapper.add(DrawController.getOptionsPanel(), BorderLayout.LINE_END);

        JPanel jspContainer = new JPanel(new BorderLayout());
        jspContainer.add(addLayerButtonWrapper, BorderLayout.CENTER);
        jspContainer.add(jsp, BorderLayout.PAGE_END);
        add(jspContainer, gc);

        grid.setTableHeader(null);
        grid.setShowHorizontalLines(true);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.getColumnModel().getColumn(ENABLED_COL).setCellRenderer(new RendererEnabled());
        grid.getColumnModel().getColumn(ENABLED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(ENABLED_COL).setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new RendererName());

        grid.getColumnModel().getColumn(LOADING_COL).setCellRenderer(new RendererLoading());
        grid.getColumnModel().getColumn(LOADING_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(LOADING_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(LINECOLOR_COL).setCellRenderer(new RendererColor());
        grid.getColumnModel().getColumn(LINECOLOR_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LINECOLOR_COL).setMaxWidth(20);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new RendererRemove());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                setOptionsPanel((TimelineLayer) grid.getValueAt(grid.getSelectedRow(), 0));
            }
        });

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            private void handlePopup(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof TimelineLayer timeline))
                    return;

                if (v.col == ENABLED_COL) {
                    timeline.setEnabled(!timeline.isEnabled());
                    model.updateCell(v.row, v.col);
                    if (grid.getSelectedRow() == v.row)
                        setOptionsPanel(timeline);
                    DrawController.graphAreaChanged();
                } else if (v.col == REMOVE_COL && timeline.isDeletable()) {
                    model.remove(timeline);
                    int idx = grid.getSelectedRow();
                    if (v.row <= idx)
                        grid.getSelectionModel().setSelectionInterval(idx - 1, idx - 1);
                }
            }
        });

        jsp.setPreferredSize(new Dimension(-1, grid.getRowHeight() * NUMBEROFVISIBLEROWS + 1));

        gc.gridy = 1;
        optionsPanelWrapper = new JPanel(new BorderLayout());
        add(optionsPanelWrapper, gc);
    }

    private void setOptionsPanel(TimelineLayer timeline) {
        optionsPanelWrapper.removeAll();
        Component optionsPanel = timeline == null ? null : timeline.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.setEnabled(optionsPanel, timeline.isEnabled());
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

}
