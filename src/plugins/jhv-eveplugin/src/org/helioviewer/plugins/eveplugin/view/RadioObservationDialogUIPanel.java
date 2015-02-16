package org.helioviewer.plugins.eveplugin.view;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.io.DataSourceServerListener;
import org.helioviewer.jhv.io.DataSourceServers;
import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;

public class RadioObservationDialogUIPanel extends SimpleObservationDialogUIPanel implements DataSourceServerListener {

    private static final long serialVersionUID = -885570748845807311L;

    private final String[] serverList;
    private final JComboBox comboServer;
    private final JLabel labelServer = new JLabel("Server");
    private boolean setFromOutside;

    public RadioObservationDialogUIPanel(PlotsContainerPanel plotsContainerPanel) {
        super(plotsContainerPanel);
        serverList = DataSourceServers.getSingletonInstance().getServerList();
        comboServer = new JComboBox(serverList);
        setFromOutside = true;
        comboServer.setSelectedItem(DataSourceServers.getSingletonInstance().getSelectedServer());
        setFromOutside = false;
        DataSourceServers.getSingletonInstance().addListener(this);
        initVisualComponents();
    }

    private void initVisualComponents() {
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createTitledBorder("Choose experiment specific data source"));
        container.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        container.add(labelServer);
        container.add(comboServer);
        comboServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!setFromOutside) {
                    String server = (String) comboServer.getSelectedItem();
                    DataSourceServers.getSingletonInstance().changeServer(server, true);
                } else {
                    setFromOutside = false;
                }
            }
        });

        add(container);
    }

    @Override
    public void serverChanged(boolean donotloadStartup) {
        setFromOutside = true;
        comboServer.setSelectedItem(DataSourceServers.getSingletonInstance().getSelectedServer());
    }
}
