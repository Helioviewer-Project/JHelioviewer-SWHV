package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesListener;

@SuppressWarnings("serial")
public class ServerListPanel extends JPanel implements DataSourcesListener {

    private final JComboBox comboServer;
    private boolean setFromOutside;

    public ServerListPanel(String label) {
        JLabel labelServer = new JLabel(label, JLabel.RIGHT);

        comboServer = new JComboBox(DataSources.getSingletonInstance().getServerList());
        setFromOutside = true;
        comboServer.setSelectedItem(DataSources.getSingletonInstance().getSelectedServer());
        setFromOutside = false;

        comboServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!setFromOutside) {
                    String server = (String) comboServer.getSelectedItem();
                    DataSources.getSingletonInstance().changeServer(server, true);
                } else {
                    setFromOutside = false;
                }
            }
        });

        DataSources.getSingletonInstance().addListener(this);

        setLayout(new GridLayout(1, 2, ObservationDialogPanel.GRIDLAYOUT_HGAP, ObservationDialogPanel.GRIDLAYOUT_VGAP));
        add(labelServer);
        add(comboServer);
    }

    @Override
    public void serverChanged(boolean donotloadStartup) {
        setFromOutside = true;
        comboServer.setSelectedItem(DataSources.getSingletonInstance().getSelectedServer());
    }

}
