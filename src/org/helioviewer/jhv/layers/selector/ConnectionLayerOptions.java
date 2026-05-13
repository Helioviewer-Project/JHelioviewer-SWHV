package org.helioviewer.jhv.layers.selector;

import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.URI;
import java.util.function.BiConsumer;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.ConnectionLayer;
import org.helioviewer.jhv.layers.connect.LoadConnectivity;
import org.helioviewer.jhv.layers.connect.LoadFootpoint;
import org.helioviewer.jhv.layers.connect.LoadHCS;

@SuppressWarnings("serial")
final class ConnectionLayerOptions extends JPanel {

    ConnectionLayerOptions(ConnectionLayer layer) {
        JButton clearBtn = new JButton("Clear all");
        clearBtn.addActionListener(e -> layer.clear());

        JButton connectivityBtn = new JButton("Connectivity");
        connectivityBtn.addActionListener(e -> load(layer, LoadConnectivity::submit));

        JButton hcsBtn = new JButton("HCS");
        hcsBtn.addActionListener(e -> load(layer, LoadHCS::submit));

        JButton footpointBtn = new JButton("Footpoint");
        footpointBtn.addActionListener(e -> load(layer, LoadFootpoint::submit));

        setLayout(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        add(clearBtn, c0);
        c0.gridx = 1;
        add(connectivityBtn, c0);
        c0.gridx = 2;
        add(hcsBtn, c0);
        c0.gridx = 3;
        add(footpointBtn, c0);
    }

    private void load(ConnectionLayer layer, BiConsumer<URI, ConnectionLayer> consumer) {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        fileDialog.setMultipleMode(false);
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            consumer.accept(fileNames[0].toURI(), layer);
    }

}
