package org.helioviewer.jhv.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.TransferLoad;
import org.helioviewer.jhv.thread.AppThread;

public final class TransferAccess {

    public static boolean canImport(Transferable transferable) {
        return transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                || transferable.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    public static boolean importTransferable(Transferable transferable) {
        try {
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<?> objects = (List<?>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                AppThread.create(() -> TransferLoad.transferFileList(objects), "JHV-TransferFileList").start(); // avoid file system operations on EDT
                return true;
            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String loc = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                AppThread.create(() -> TransferLoad.transferStringArray(loc), "JHV-TransferStringArray").start(); // avoid file system operations on EDT
                return true;
            }
        } catch (Exception e) {
            Log.warn("Import error", e);
        }
        return false;
    }

    public static void readClipboard() {
        try {
            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents != null)
                importTransferable(contents);
        } catch (IllegalStateException e) {
            Log.warn("Clipboard temporarily unavailable", e);
        }
    }

    public static void writeClipboard(String data) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(data), null);
        } catch (IllegalStateException e) {
            Log.warn("Clipboard temporarily unavailable", e);
        }
    }

    private TransferAccess() {}
}
