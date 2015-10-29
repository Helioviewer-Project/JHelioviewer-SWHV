package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.helioviewer.jhv.io.DataSources;

public class ServerListCombo extends JComboBox {

    private ServerListCombo() {
        setModel(new DefaultComboBoxModel(DataSources.getSingletonInstance().getServerList()));
        setSelectedItem(DataSources.getSingletonInstance().getSelectedServer());

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                DataSources.getSingletonInstance().changeServer((String) getSelectedItem());
            }
        });
    }

    public static ServerListCombo getInstance() {
        if (instance == null) {
            instance = new ServerListCombo();
        }
        return instance;
    }

    private static ServerListCombo instance;

}
