package org.helioviewer.jhv.timelines.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.TableValue;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandReaderHapi;
import org.helioviewer.jhv.timelines.band.BandType;
import org.helioviewer.jhv.timelines.draw.DrawController;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public final class TimelinePanel extends JPanel {

    private static final int ICON_WIDTH = 12;
    private static final String NONE_ITEM = "None";

    private static final int ENABLED_COL = 0;
    private static final int TITLE_COL = 1;
    public static final int LOADING_COL = 2;
    private static final int LINECOLOR_COL = 3;
    private static final int REMOVE_COL = 4;

    public static final int NUMBEROFCOLUMNS = 5;
    private static final int NUMBEROFVISIBLEROWS = 6;

    private final TimelineTable grid;
    private final TimelineLayers layers;
    private final JPanel optionsPanelWrapper;
    private final JComboBox<String> predefinedCombo;
    private boolean suppressComboAction;

    private static class TimelineTable extends JTable implements Interfaces.LazyComponent {

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
        public void tableChanged(TableModelEvent e) {
            super.tableChanged(e);
            if (e.getType() == TableModelEvent.INSERT) {
                int row = e.getLastRow();
                setRowSelectionInterval(row, row);
            }
        }

        @Override
        public void repaint() {
            dirty().setBounds(0, 0, getWidth(), getHeight());
            dirtyValid = true;
        }

        @Override
        public void repaint(int x, int y, int width, int height) {
            if (!dirtyValid) {
                dirty().setBounds(x, y, width, height);
                dirtyValid = true;
            } else {
                dirty().add(x, y);
                dirty().add(x + width, y + height);
            }
        }

        private Rectangle dirty = null;
        private boolean dirtyValid = false;

        private Rectangle dirty() {
            if (dirty == null)
                dirty = new Rectangle();
            return dirty;
        }

        @Override
        public void lazyRepaint() {
            if (dirtyValid) {
                super.repaint(dirty.x, dirty.y, dirty.width, dirty.height);
                dirtyValid = false;
            }
        }

    }

    public TimelinePanel(TimelineLayers model) {
        layers = model;
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;

        grid = new TimelineTable(model);

        JScrollPane jsp = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().brighter()));
        jsp.getViewport().setBackground(grid.getBackground());

        JideButton addLayerButton = new JideButton(Buttons.newLayer);
        addLayerButton.addActionListener(e -> new TimelineActions.NewLayer().actionPerformed(new ActionEvent(addLayerButton, 0, "")));

        predefinedCombo = new JComboBox<>();
        predefinedCombo.setToolTipText("Predefined plot");
        predefinedCombo.addActionListener(e -> {
            if (suppressComboAction)
                return;
            Object selected = predefinedCombo.getSelectedItem();
            if (selected instanceof String groupName && !NONE_ITEM.equals(groupName))
                loadPredefinedGroup(groupName);
        });

        refreshPredefinedCombo();
        BandReaderHapi.setOnCatalogLoaded(this::refreshPredefinedCombo);

        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        leftButtonPanel.add(addLayerButton);
        leftButtonPanel.add(new JLabel("Predefined:"));
        leftButtonPanel.add(predefinedCombo);

        JPanel addLayerButtonWrapper = new JPanel(new BorderLayout());
        addLayerButtonWrapper.add(leftButtonPanel, BorderLayout.LINE_START);
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

        grid.getColumnModel().getColumn(ENABLED_COL).setCellRenderer(new CellRenderer.Enabled());
        grid.getColumnModel().getColumn(ENABLED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(ENABLED_COL).setMaxWidth(ICON_WIDTH + 8);

        grid.getColumnModel().getColumn(TITLE_COL).setCellRenderer(new CellRenderer.Name());

        grid.getColumnModel().getColumn(LOADING_COL).setCellRenderer(new CellRenderer.Loading());
        grid.getColumnModel().getColumn(LOADING_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(LOADING_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getColumnModel().getColumn(LINECOLOR_COL).setCellRenderer(new CellRenderer.LineColor());
        grid.getColumnModel().getColumn(LINECOLOR_COL).setPreferredWidth(20);
        grid.getColumnModel().getColumn(LINECOLOR_COL).setMaxWidth(20);

        grid.getColumnModel().getColumn(REMOVE_COL).setCellRenderer(new CellRenderer.Remove());
        grid.getColumnModel().getColumn(REMOVE_COL).setPreferredWidth(ICON_WIDTH + 2);
        grid.getColumnModel().getColumn(REMOVE_COL).setMaxWidth(ICON_WIDTH + 2);

        grid.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                refreshSelectedOptionsPanel();
        });

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof TimelineLayer timeline))
                    return;

                if (v.col == ENABLED_COL) {
                    timeline.setEnabled(!timeline.isEnabled());
                    layers.updateCell(v.row, v.col);
                    if (grid.getSelectedRow() == v.row)
                        setOptionsPanel(timeline);
                    DrawController.graphAreaChanged();
                } else if (v.col == REMOVE_COL && timeline.isDeletable()) {
                    layers.remove(timeline);
                    selectExistingRow(v.row);
                }
            }
        });

        jsp.setPreferredSize(new Dimension(-1, grid.getRowHeight() * NUMBEROFVISIBLEROWS + 1));

        gc.gridy = 1;
        optionsPanelWrapper = new JPanel(new BorderLayout());
        add(optionsPanelWrapper, gc);
    }

    @Nullable
    private TimelineLayer selectedLayer() {
        int row = grid.getSelectedRow();
        if (row < 0)
            return null;
        return grid.getValueAt(row, 0) instanceof TimelineLayer timeline ? timeline : null;
    }

    private void selectExistingRow(int preferredRow) {
        int rowCount = grid.getRowCount();
        if (rowCount == 0) {
            setOptionsPanel(null);
            return;
        }
        int row = Math.min(preferredRow, rowCount - 1);
        grid.getSelectionModel().setSelectionInterval(row, row);
        refreshSelectedOptionsPanel();
    }

    private void refreshSelectedOptionsPanel() {
        setOptionsPanel(selectedLayer());
    }

    private void setOptionsPanel(@Nullable TimelineLayer timeline) {
        optionsPanelWrapper.removeAll();
        JPanel optionsPanel = timeline == null ? null : timeline.getOptionsPanel();
        if (optionsPanel != null) {
            ComponentUtils.setEnabled(optionsPanel, timeline.isEnabled());
            optionsPanelWrapper.add(optionsPanel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    private void loadPredefinedGroup(String groupName) {
        LinkedHashMap<String, List<BandType>> groups = BandReaderHapi.getPredefinedGroups();
        List<BandType> bandTypes = groups.get(groupName);
        if (bandTypes == null)
            return;

        List<TimelineLayer> existing = new ArrayList<>(TimelineLayers.get());
        for (TimelineLayer layer : existing)
            layers.remove(layer);

        for (BandType bandType : bandTypes)
            layers.add(Band.createFromType(bandType));
    }

    private void refreshPredefinedCombo() {
        suppressComboAction = true;
        LinkedHashMap<String, List<BandType>> groups = BandReaderHapi.getPredefinedGroups();
        List<String> items = new ArrayList<>();
        items.add(NONE_ITEM);
        items.addAll(groups.keySet());
        predefinedCombo.setModel(new DefaultComboBoxModel<>(items.toArray(String[]::new)));
        predefinedCombo.setSelectedIndex(0);
        suppressComboAction = false;
    }

}
