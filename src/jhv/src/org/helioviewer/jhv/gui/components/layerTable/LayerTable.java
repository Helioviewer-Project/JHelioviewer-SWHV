package org.helioviewer.jhv.gui.components.layerTable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.border.Border;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.actions.DownloadJPIPOfView;
import org.helioviewer.jhv.gui.actions.HideLayerAction;
import org.helioviewer.jhv.gui.actions.MoveLayerDownAction;
import org.helioviewer.jhv.gui.actions.MoveLayerUpAction;
import org.helioviewer.jhv.gui.actions.RemoveLayerAction;
import org.helioviewer.jhv.gui.actions.ShowMetainfoOfView;
import org.helioviewer.jhv.gui.actions.UnHideLayerAction;
import org.helioviewer.jhv.gui.components.layerTable.renderers.DescriptorIconRenderer;
import org.helioviewer.jhv.gui.components.layerTable.renderers.DescriptorTimestampRenderer;
import org.helioviewer.jhv.gui.components.layerTable.renderers.DescriptorTitleRenderer;
import org.helioviewer.jhv.gui.components.layerTable.renderers.IconRenderer;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * Extended JTable, showing the Layers currently being used
 * 
 * @author Malte Nuhn
 * @author Helge Dietert
 */
public class LayerTable extends JTable {
    private static final long serialVersionUID = 1L;

    public static final int ROW_HEIGHT = 25;

    public LayerTable() {
        super(LayerTableModel.getSingletonInstance());
        this.setSelectionModel(LayerTableSelectionModel.getSingletonInstance());

        // set proper layout
        this.setTableHeader(null);
        this.setShowGrid(false);
        this.setRowSelectionAllowed(true);
        this.setColumnSelectionAllowed(false);
        this.setIntercellSpacing(new Dimension(0, 0));
        this.setRowHeight(ROW_HEIGHT);
        this.setBackground(Color.white);

        this.setupColumns();

        this.addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            /**
             * Handle with right-click menus
             * 
             * @param e
             */
            public void handlePopup(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();

                JTable source = (JTable) e.getSource();
                int row = source.rowAtPoint(e.getPoint());
                int column = source.columnAtPoint(e.getPoint());

                if (!source.isRowSelected(row))
                    source.changeSelection(row, column, false, false);

                View view = LayersModel.getSingletonInstance().getLayer(row);

                if (view != null) {
                    menu.add(new MoveLayerUpAction(view));
                    menu.add(new MoveLayerDownAction(view));

                    menu.add(new JSeparator());

                    menu.add(new ShowMetainfoOfView(view));
                    menu.add(new DownloadJPIPOfView(view));

                    menu.add(new JSeparator());

                    if (LayersModel.getSingletonInstance().isVisible(view)) {

                        menu.add(new HideLayerAction(view));

                    } else {

                        menu.add(new UnHideLayerAction(view));

                    }

                    menu.add(new RemoveLayerAction(view));

                    menu.show(e.getComponent(), e.getX(), e.getY());
                }

            }

            /**
             * Handle with clicks on hide/show/remove layer icons
             */
            public void mouseClicked(MouseEvent e) {

                LayersModel layersModel = LayersModel.getSingletonInstance();

                int row = rowAtPoint(new Point(e.getX(), e.getY()));
                int col = columnAtPoint(new Point(e.getX(), e.getY()));

                if (col < 0 || row < 0)
                    return;

                int index = row;

                if (col == LayerTableModel.COLUMN_VISIBILITY) {

                    layersModel.setVisibleLink(row, !layersModel.isVisible(index));

                } else if (col == LayerTableModel.COLUMN_BUTTON_REMOVE) {

                    layersModel.removeLayer(index);

                }

            }
        });

    }

    /**
     * Setup the layout of this Table
     * <p>
     * For that the renderer will be set and the width of the columns will be
     * adjusted
     */
    private void setupColumns() {
        Border descriptionIconBorder = BorderFactory.createEmptyBorder(5, 9, 5, 9);
        DescriptorIconRenderer descriptorIconRenderer = new DescriptorIconRenderer(descriptionIconBorder);
        descriptorIconRenderer.setFixedWidth(this, LayerTableModel.COLUMN_VISIBILITY);
        getColumnModel().getColumn(LayerTableModel.COLUMN_VISIBILITY).setCellRenderer(descriptorIconRenderer);

        getColumnModel().getColumn(LayerTableModel.COLUMN_TITLE).setCellRenderer(new DescriptorTitleRenderer(true));
        getColumnModel().getColumn(LayerTableModel.COLUMN_TITLE).setPreferredWidth(20);

        getColumnModel().getColumn(LayerTableModel.COLUMN_TIMESTAMP).setCellRenderer(new DescriptorTimestampRenderer(true));

        Border removeIconBorder = BorderFactory.createEmptyBorder(5, 14, 5, 9);
        IconRenderer iconRenderer = new IconRenderer("Remove Layer", IconBank.getIcon(JHVIcon.REMOVE_LAYER), removeIconBorder);
        iconRenderer.setFixedWidth(this, LayerTableModel.COLUMN_BUTTON_REMOVE);
        getColumnModel().getColumn(LayerTableModel.COLUMN_BUTTON_REMOVE).setCellRenderer(iconRenderer);
    }
}