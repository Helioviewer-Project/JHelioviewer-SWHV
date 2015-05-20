package org.helioviewer.jhv.renderable.gui;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.image.BufferedImage;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.helioviewer.jhv.display.Displayer;

/**
 * Handles drag & drop row reordering
 */
@SuppressWarnings({"serial"})
public class TableRowTransferHandler extends TransferHandler {

    private final DataFlavor integerObjectFlavor = new ActivationDataFlavor(Integer.class, "Integer Row Index");
    private JTable grid = null;
    BufferedImage image;

    public TableRowTransferHandler(JTable table) {
        this.grid = table;
    }

    public void createImageOfRow(int rowIndex) {
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
        assert (c == grid);
        createImageOfRow(grid.getSelectedRow());
        return new DataHandler(new Integer(grid.getSelectedRow()), integerObjectFlavor.getMimeType());
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        boolean b = info.getComponent() == grid && info.isDrop() && info.isDataFlavorSupported(integerObjectFlavor);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Cursor mcursor = toolkit.createCustomCursor(image, new Point(10, 10), "DnD");
        grid.setCursor(b ? mcursor : DragSource.DefaultMoveNoDrop);
        return b;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY_OR_MOVE;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {

        JTable target = (JTable) info.getComponent();
        JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
        int index = dl.getRow();

        int max = grid.getModel().getRowCount();
        if (index < 0 || index > max)
            index = max;
        target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        try {
            Object obj = info.getTransferable().getTransferData(integerObjectFlavor);
            Integer rowFrom = (Integer) obj;

            if (rowFrom != -1 && rowFrom != index) {
                ((Reorderable) grid.getModel()).reorder(rowFrom, index);
                if (index > rowFrom)
                    index--;
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int act) {
        if (act == TransferHandler.MOVE) {
            grid.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        Displayer.display();
    }

}
