package org.helioviewer.jhv.layers.selector;

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

import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

// Handles DnD row reordering
@SuppressWarnings("serial")
class TableRowTransferHandler extends TransferHandler {

    private final JTable grid;
    private BufferedImage image;

    TableRowTransferHandler(JTable table) {
        grid = table;
    }

    private boolean isOurs(TransferHandler.TransferSupport info) {
        return info.getComponent() == grid && info.isDrop() && info.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    private void createImageOfRow(int rowIndex) {
        int w = grid.getWidth();
        int h = grid.getRowHeight();
        int x = grid.getX();
        int y = h * rowIndex;
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();
        float opacity = 0.5f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2.translate(-x, -y);
        grid.paint(g2);
        g2.dispose();
    }

    @Nullable
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c != grid)
            return null;

        int row = grid.getSelectedRow();
        if (row == -1)
            return null;

        Object el = grid.getModel().getValueAt(row, 0);
        if (!(el instanceof ImageLayer))
            return null;

        createImageOfRow(row);
        return new StringSelection(Integer.toString(row));
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        boolean ours = isOurs(info);
        grid.setCursor(ours ? Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(10, 10), "DnD") : DragSource.DefaultMoveNoDrop);
        return ours;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY_OR_MOVE;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        TransferHandler.DropLocation idl;
        if (isOurs(info) && (idl = info.getDropLocation()) instanceof JTable.DropLocation) {
            int index = ((JTable.DropLocation) idl).getRow();
            int max = grid.getModel().getRowCount();
            if (index < 0 || index > max)
                index = max;

            try {
                Object obj = info.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int rowFrom = Integer.parseInt((String) obj);
                if (rowFrom != -1 && rowFrom != index) {
                    ((Reorderable) grid.getModel()).reorder(rowFrom, index);
                    grid.repaint(); // multiple rows involved
                    return true;
                }
            } catch (Exception e) {
                Log.error(e);
            }
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int act) {
        if (act == TransferHandler.MOVE || act == TransferHandler.NONE) {
            grid.setCursor(Cursor.getDefaultCursor());
        }
        MovieDisplay.display();
    }

}
