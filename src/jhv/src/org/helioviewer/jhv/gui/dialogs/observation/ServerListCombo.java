package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.helioviewer.jhv.io.DataSources;

public class ServerListCombo extends JComboBox {

    private static ActionListener change = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataSources.changeServer((String) ((JComboBox) e.getSource()).getModel().getSelectedItem());
            }
        };

    private ServerListCombo() {
        setModel(new DefaultComboBoxModel(DataSources.getServerList()));
        setSelectedItem(DataSources.getSelectedServer());
        addActionListener(change);
    }

    public static JComboBox getInstance() {
        if (instance == null) {
            instance = new ServerListCombo();
            return instance;
        } else
            return new JComboBox(instance.getModel());
    }

    private static ServerListCombo instance;

}
