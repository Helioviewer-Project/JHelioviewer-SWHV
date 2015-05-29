package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesListener;

//Java 6 does not support generics for JComboBox and DefaultComboBoxModel
//Should be removed if support for Java 6 is not needed anymore
//Class will not be serialized so we suppress the warnings
@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
public class RadioObservationDialogUIPanel extends SimpleObservationDialogUIPanel implements DataSourcesListener {

    private final String[] serverList;
    private final JComboBox comboServer;
    private boolean setFromOutside;

    public RadioObservationDialogUIPanel() {
        super();
        serverList = DataSources.getSingletonInstance().getServerList();
        comboServer = new JComboBox(serverList);
        setFromOutside = true;
        comboServer.setSelectedItem(DataSources.getSingletonInstance().getSelectedServer());
        setFromOutside = false;

        JLabel labelServer = new JLabel("Server", JLabel.RIGHT);
        JPanel container = new JPanel();

        container.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        container.add(labelServer);
        container.add(comboServer);

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

        add(container);

        DataSources.getSingletonInstance().addListener(this);
    }

    @Override
    public void serverChanged(boolean donotloadStartup) {
        setFromOutside = true;
        comboServer.setSelectedItem(DataSources.getSingletonInstance().getSelectedServer());
    }

}
