package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesListener;

public class ServerListCombo extends JComboBox implements DataSourcesListener {

    private boolean setFromOutside;

    public ServerListCombo() {
        setModel(new DefaultComboBoxModel(DataSources.getSingletonInstance().getServerList()));

        setFromOutside = true;
        setSelectedItem(DataSources.getSingletonInstance().getSelectedServer());
        setFromOutside = false;

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!setFromOutside) {
                    String server = (String) getSelectedItem();
                    DataSources.getSingletonInstance().changeServer(server, true);
                } else {
                    setFromOutside = false;
                }
            }
        });

        DataSources.getSingletonInstance().addListener(this);
    }

    @Override
    public void serverChanged(boolean donotloadStartup) {
        setFromOutside = true;
        setSelectedItem(DataSources.getSingletonInstance().getSelectedServer());
    }

}
