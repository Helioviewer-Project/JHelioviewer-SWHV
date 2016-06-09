package org.helioviewer.jhv.renderable.gui;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.RenderableImageLayer;

// Handles DnD row reordering
@SuppressWarnings("serial")
public class TableRowTransferHandler extends TransferHandler {

    private final JTable grid;
    private BufferedImage image;

    public TableRowTransferHandler(JTable table) {
        this.grid = table;
    }

    private void createImageOfRow(int rowIndex) {
        int x = grid.getX();
        int y = grid.getRowHeight() * rowIndex;
        int w = grid.getWidth();
        int h = grid.getRowHeight();
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();
        float opacity = 0.5f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2.translate(-x, -y);
        grid.paint(g2);
        g2.dispose();
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c != grid)
            return null;

        int row = grid.getSelectedRow();
        Object el = grid.getModel().getValueAt(row, 0);
        if (!(el instanceof RenderableImageLayer)) {
            return null;
        }
        createImageOfRow(row);
        return new StringSelection(Integer.toString(row));
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        boolean b = info.getComponent() == grid && info.isDrop() && info.isDataFlavorSupported(DataFlavor.stringFlavor);
        Cursor mcursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(10, 10), "DnD");
        grid.setCursor(b ? mcursor : DragSource.DefaultMoveNoDrop);
        return b;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY_OR_MOVE;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        TransferHandler.DropLocation idl = info.getDropLocation();
        if (idl instanceof JTable.DropLocation) {
            int index = ((JTable.DropLocation) idl).getRow();
            int max = grid.getModel().getRowCount();
            if (index < 0 || index > max)
                index = max;

            try {
                Object obj = info.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int rowFrom = Integer.parseInt((String) obj);
                if (rowFrom != -1 && rowFrom != index) {
                    ((Reorderable) grid.getModel()).reorder(rowFrom, index);
                    // if (index > rowFrom)
                    //    index--;
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int act) {
        if (act == TransferHandler.MOVE || act == TransferHandler.NONE) {
            grid.setCursor(Cursor.getDefaultCursor());
        }
        Displayer.display();
    }

}
