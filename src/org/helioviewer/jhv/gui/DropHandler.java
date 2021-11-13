package org.helioviewer.jhv.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.TransferHandler;

import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.log.Log;

import org.apache.commons.validator.routines.UrlValidator;

@SuppressWarnings("serial")
class DropHandler extends TransferHandler {

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        Transferable transferable = support.getTransferable();
        return support.isDrop() && (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                transferable.isDataFlavorSupported(DataFlavor.stringFlavor));
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support))
            return false;

        try {
            Transferable transferable = support.getTransferable();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<?> objects = (List<?>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                List<URI> imageUris = new ArrayList<>(objects.size());
                ArrayList<URI> requestUris = new ArrayList<>(objects.size());
                for (Object o : objects) {
                    if (o instanceof File f) {
                        if (f.isFile() && f.canRead()) {
                            URI uri = f.toURI();
                            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
                            if (loc.endsWith(".json"))
                                requestUris.add(uri);
                            else
                                imageUris.add(uri);
                        }
                    }
                }

                requestUris.forEach(Load.request::get);
                if (!imageUris.isEmpty())
                    Load.Image.getAll(imageUris);

                return true;
            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String loc = (String) transferable.getTransferData(DataFlavor.stringFlavor);

                if (UrlValidator.getInstance().isValid(loc)) {
                    URI uri = new URI(loc);
                    if (loc.endsWith(".json"))
                        Load.request.get(uri);
                    else if (loc.endsWith(".jhv"))
                        Load.state.get(uri);
                    else
                        Load.image.get(uri);

                    return true;
                }
            }
        } catch (Exception e) {
            Log.error("Import error", e);
        }

        return false;
    }

}
