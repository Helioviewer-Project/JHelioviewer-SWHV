package org.helioviewer.jhv.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

// Simple helper for accessing the user's clipboard.
public class ClipBoardCopier implements ClipboardOwner {

    private static final ClipBoardCopier instance = new ClipBoardCopier();

    public static ClipBoardCopier getSingletonInstance() {
        return instance;
    }

    private ClipBoardCopier() {
    }

    @Override
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
    }

    /**
     * Set the content of the clipboard
     * 
     * @param data
     *            content to write to the clipboard
     */
    public void setString(String data) {
        StringSelection stringSelection = new StringSelection(data);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, this);
    }

    /**
     * Read the current content from the clipboard.
     * 
     * @return clipboard content
     */
    public static String getString() {
        Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        boolean hasTransferableText = contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

}
