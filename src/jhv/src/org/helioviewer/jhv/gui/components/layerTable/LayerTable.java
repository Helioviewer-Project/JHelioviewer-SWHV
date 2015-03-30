package org.helioviewer.jhv.gui.components.layerTable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.layerTable.renderers.DescriptorIconRenderer;
import org.helioviewer.jhv.gui.components.layerTable.renderers.DescriptorTimestampRenderer;
import org.helioviewer.jhv.gui.components.layerTable.renderers.DescriptorTitleRenderer;
import org.helioviewer.jhv.gui.components.layerTable.renderers.IconRenderer;
import org.helioviewer.jhv.layers.LayersModel;

/**
 * Extended JTable, showing the Layers currently being used
 *
 * @author Malte Nuhn
 * @author Helge Dietert
 */
public class LayerTable extends JTable {

    private static final int COLUMN_VISIBILITY = 0;
    private static final int COLUMN_TITLE = 1;
    private static final int COLUMN_TIMESTAMP = 2;
    private static final int COLUMN_BUTTON_REMOVE = 3;

    public static final int ROW_HEIGHT = 25;

    public LayerTable() {
        super(LayersModel.getSingletonInstance());
        this.setSelectionModel(LayerTableSelectionModel.getSingletonInstance());

        this.setTableHeader(null);
        this.setShowGrid(false);
        this.setRowSelectionAllowed(true);
        this.setColumnSelectionAllowed(false);
        this.setIntercellSpacing(new Dimension(0, 0));

        this.setRowHeight(ROW_HEIGHT);
        this.setBackground(Color.white);

        this.setupColumns();

        final LayersModel layersModel = LayersModel.getSingletonInstance();

        this.addMouseListener(new MouseAdapter() {
            /**
             * Handle with clicks on hide/show/remove layer icons
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = rowAtPoint(new Point(e.getX(), e.getY()));
                int col = columnAtPoint(new Point(e.getX(), e.getY()));
                if (col < 0 || row < 0) {
                    return;
                }
                int index = row;

                if (col == COLUMN_VISIBILITY) {
                    layersModel.toggleVisibility(index);
                } else if (col == COLUMN_BUTTON_REMOVE) {
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
        Border border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray);
        Border descriptionIconBorder = border;
        DescriptorIconRenderer descriptorIconRenderer = new DescriptorIconRenderer(descriptionIconBorder);
        descriptorIconRenderer.setFixedWidth(this, COLUMN_VISIBILITY);
        getColumnModel().getColumn(COLUMN_VISIBILITY).setCellRenderer(descriptorIconRenderer);

        getColumnModel().getColumn(COLUMN_TITLE).setCellRenderer(new DescriptorTitleRenderer(true));
        getColumnModel().getColumn(COLUMN_TITLE).setPreferredWidth(38);
        // getColumnModel().getColumn(COLUMN_TITLE).setWidth(5);

        getColumnModel().getColumn(COLUMN_TIMESTAMP).setCellRenderer(new DescriptorTimestampRenderer(true));
        // getColumnModel().getColumn(COLUMN_TIMESTAMP).setPreferredWidth(15);

        Border removeIconBorder = border;
        IconRenderer iconRenderer = new IconRenderer("Remove Layer", IconBank.getIcon(JHVIcon.REMOVE_LAYER), removeIconBorder);
        iconRenderer.setFixedWidth(this, COLUMN_BUTTON_REMOVE);
        getColumnModel().getColumn(COLUMN_BUTTON_REMOVE).setCellRenderer(iconRenderer);
    }

}
