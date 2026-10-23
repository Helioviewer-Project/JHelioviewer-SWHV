package org.helioviewer.jhv.layers.selector;

import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.io.File;
import java.net.URI;
import java.util.function.BiConsumer;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.ConnectionLayer;
import org.helioviewer.jhv.layers.connect.LoadConnectivity;
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.layers.connect.LoadHCS;

@SuppressWarnings("serial")
final class ConnectionLayerOptions extends JPanel {

    ConnectionLayerOptions(ConnectionLayer layer) {
        super(new FlowLayout(FlowLayout.LEADING, 0, 0));
        JButton clearBtn = new JButton("Clear all");
        clearBtn.addActionListener(e -> layer.clear());

        JButton connectivityBtn = new JButton("Connectivity");
        connectivityBtn.addActionListener(e -> load(layer, LoadConnectivity::submit));

        JButton hcsBtn = new JButton("HCS");
        hcsBtn.addActionListener(e -> load(layer, LoadHCS::submit));

        JButton footpointBtn = new JButton("Footpoint");
        footpointBtn.addActionListener(e -> load(layer, LoadFootpoint::submit));

        add(clearBtn);
        add(connectivityBtn);
        add(hcsBtn);
        add(footpointBtn);
    }

    private void load(ConnectionLayer layer, BiConsumer<URI, ConnectionLayer> consumer) {
        FileDialog fileDialog = new FileDialog(MainFrame.get(), "Choose a file", FileDialog.LOAD);
        fileDialog.setMultipleMode(false);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            consumer.accept(fileNames[0].toURI(), layer);
    }

}
