package org.helioviewer.jhv.gui.components;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.TransferHandler;

import org.helioviewer.jhv.io.Load;

@SuppressWarnings("serial")
class MainDropHandler extends TransferHandler {

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return info.isDrop() && info.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!canImport(info)) {
            return false;
        }

        try {
            List<?> objects = (List<?>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

            ArrayList<URI> imageUris = new ArrayList<>(objects.size());
            ArrayList<URI> requestUris = new ArrayList<>(objects.size());
            for (Object object : objects) {
                if (object instanceof File) {
                    File f = (File) object;
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
