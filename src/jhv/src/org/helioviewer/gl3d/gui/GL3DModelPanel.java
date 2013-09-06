package org.helioviewer.gl3d.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.plugin.GL3DModelPlugin;
import org.helioviewer.gl3d.plugin.GL3DPluginController;
import org.helioviewer.gl3d.plugin.GL3DPluginListener;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.layerTable.renderers.IconRenderer;

public class GL3DModelPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = -1285528909577059601L;

    private GL3DPluginController pluginController = GL3DPluginController.getInstance();

    private GL3DModelTableModel pluginModel;

    public void init() {
        this.setLayout(new BorderLayout());

        this.removeAll();

        pluginModel = new GL3DModelTableModel();
        final JTable pluginTable = new JTable(pluginModel);

        pluginTable.setTableHeader(null);
        pluginTable.setRowHeight(25);
        // pluginTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        pluginTable.getColumnModel().getColumn(0).setMaxWidth(30);
        // pluginTable.getColumnModel().getColumn(1).setMinWidth(100);
        pluginTable.getColumnModel().getColumn(2).setMaxWidth(25);

        IconRenderer configureRenderer = new IconRenderer("Configure Model", IconBank.getIcon(JHVIcon.ADD), BorderFactory.createEtchedBorder());
        pluginTable.getColumnModel().getColumn(2).setCellRenderer(configureRenderer);
        // configureRenderer.setFixedWidth(pluginTable, 2);

        JScrollPane scroll = new JScrollPane(pluginTable);
        scroll.setPreferredSize(new Dimension(300, 200));
        // scroll.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Dimension d = pluginTable.getPreferredSize();
        // scroll.setPreferredSize(
        // new Dimension(d.width,pluginTable.getRowHeight()*2+1));

        // scroll.setBorder(BorderFactory.createEmptyBorder());
        // scroll.setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        // add(pluginTable, BorderLayout.EAST);
        // this.revalidate();
        pluginTable.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                int row = pluginTable.rowAtPoint(getMousePosition());
                if (row < 0 || row >= pluginController.getPluginCount()) {
                    Log.error("Illegal row number " + row);
                }
                int col = pluginTable.columnAtPoint(getMousePosition());
                GL3DModelPlugin plugin = pluginController.getPlugin(row);

                if (col == 2) { // remove
                    showPluginConfiguration(plugin);
                }
                // else if(col==3) { //remove
                // pluginController.removePlugin(plugin);
                // }
                super.mouseClicked(e);
            }
        });

        pluginController.addPluginListener(pluginModel);
    }

    private void showPluginConfiguration(GL3DModelPlugin plugin) {
        JDialog configDialog = new JDialog();
        configDialog.getContentPane().setLayout(new BorderLayout());
        configDialog.getContentPane().add(plugin.getConfigurationComponent(), BorderLayout.CENTER);
        configDialog.setTitle("Configure " + plugin.getPluginName());
        configDialog.pack();
        configDialog.setVisible(true);
    }

    public void destroy() {
        pluginController.removePluginListener(pluginModel);
    }

    private class GL3DModelTableModel extends DefaultTableModel implements GL3DPluginListener {

        /**
         * 
         */
        private static final long serialVersionUID = 7230256306058534326L;

        public String getColumnName(int column) {
            String name = "unknown";

            switch (column) {
            case 0:
                name = "Active";
                break;
            case 1:
                name = "Name";
                break;
            case 2:
                name = "Configure";
                break;
            // case 3:
            // name = "Remove";
            // break;
            }
            return name;
        }

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return pluginController.getPluginCount();
        }

        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        public void setValueAt(Object aValue, int row, int column) {
            GL3DModelPlugin plugin = pluginController.getPlugin(row);
            if (column == 0) {
                plugin.setActive((Boolean) aValue);
                if (((Boolean) aValue).booleanValue()) {
                    pluginController.activatePlugin(plugin);
                } else {
                    pluginController.deactivatePlugin(plugin);
                }
            } else {
                super.setValueAt(aValue, row, column);
            }
        }

        public Object getValueAt(int row, int col) {
            GL3DModelPlugin plugin = pluginController.getPlugin(row);
            switch (col) {
            case 0:
                return plugin.isActive();
            case 1:
                return plugin.getPluginName();
            case 2:
                return plugin;
            }
            return plugin;
        }

        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            }
            return Object.class;
        }

        public void pluginLoaded(GL3DModelPlugin plugin) {
            fireTableDataChanged();
        }

        public void pluginUnloaded(GL3DModelPlugin plugin) {
            fireTableDataChanged();
        }
    }
}
