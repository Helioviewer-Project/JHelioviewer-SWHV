package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

@SuppressWarnings("serial")
public class PluginSelectionPanel extends JPanel {

    private JPanel plugins;
    AbstractList<String> fileNames;
    AbstractMap<JButton, String> removePluginButtons;

    public PluginSelectionPanel() {
        super(new BorderLayout());

        fileNames = new LinkedList<String>();
        removePluginButtons = new HashMap<JButton, String>();

        plugins = new JPanel();
        plugins.setLayout(new BoxLayout(plugins, BoxLayout.Y_AXIS));

        reloadPlugins();

        JScrollPane scrollPane = new JScrollPane(plugins);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void reloadPlugins() {
        plugins.removeAll();

        // Create a file object for your root directory
        File f1 = JHVDirectory.PLUGINS.getFile();

        // Get all the files and directory under your diretcory
        File[] strFilesDirs = f1.listFiles();

        for (int i = 0; i < strFilesDirs.length; i++) {
            if (strFilesDirs[i].isFile()) {
                String fileName = strFilesDirs[i].getName();

                if (fileName.toUpperCase().endsWith(".JAR")) {
                    JPanel tmpPanel = new JPanel();
                    tmpPanel.setLayout(new BoxLayout(tmpPanel, BoxLayout.X_AXIS));
                    tmpPanel.setPreferredSize(new Dimension(0, 25));
                    tmpPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                    JButton button = createRemovePluginButton();

                    tmpPanel.add(new JLabel(fileName));
                    tmpPanel.add(Box.createHorizontalGlue());
                    tmpPanel.add(button);

                    fileNames.add(fileName);
                    removePluginButtons.put(button, fileName);

                    plugins.add(tmpPanel);
                }
            }
        }

        plugins.add(Box.createVerticalGlue());
        plugins.revalidate();
        plugins.repaint();
    }

    private JButton createRemovePluginButton() {
        JButton button = new JButton();
        button.setIcon(IconBank.getIcon(JHVIcon.EX));
        button.setToolTipText("Remove plugin");
        button.setBorder(BorderFactory.createEtchedBorder());

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JButton abstractButton = (JButton) actionEvent.getSource();

                new File(JHVDirectory.PLUGINS.getPath() + removePluginButtons.get(abstractButton)).delete();
                reloadPlugins();
            }
        };

        button.addActionListener(listener);

        return button;
    }
}