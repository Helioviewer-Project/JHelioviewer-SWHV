package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

@SuppressWarnings("serial")
public class PluginsDialog extends JDialog implements ShowableDialog, PluginsListEntryChangeListener {

    private static final Dimension DIALOG_SIZE_MINIMUM = new Dimension(400, 500);
    private static final Dimension DIALOG_SIZE_PREFERRED = new Dimension(400, 500);

    private final JComboBox<String> filterComboBox = new JComboBox<>(new String[]{ "All", "Enabled", "Disabled" });

    private final JLabel emptyLabel = new JLabel("No plug-ins available", JLabel.CENTER);
    private final PluginsList pluginList = new PluginsList();
    private final JPanel listContainerPane = new JPanel();
    private final CardLayout listLayout = new CardLayout();

    public PluginsDialog() {
        super(ImageViewerGui.getMainFrame(), "Plug-in Manager", true);

        // dialog
        setMinimumSize(DIALOG_SIZE_MINIMUM);
        setPreferredSize(DIALOG_SIZE_PREFERRED);

        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }
        });

        // header
        JLabel headerLabel = new JLabel("You can enable or disable JHelioviewer plug-ins.");
        headerLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 6, 3));

        // center - installed plug-ins
        JPanel installedFilterPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        installedFilterPane.add(new JLabel("Filter"));
        installedFilterPane.add(filterComboBox);

        filterComboBox.addActionListener(e -> updatePluginList());

        pluginList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        emptyScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(Color.WHITE);

        listContainerPane.setLayout(listLayout);
        listContainerPane.add(emptyScrollPane, "empty");
        listContainerPane.add(pluginList, "list");

        pluginList.addListEntryChangeListener(this);

        JPanel installedPane = new JPanel(new BorderLayout());
        installedPane.setBorder(BorderFactory.createTitledBorder(" Installed Plug-ins "));
        installedPane.add(installedFilterPane, BorderLayout.PAGE_START);
        installedPane.add(listContainerPane, BorderLayout.CENTER);

        // center
        JPanel centerPane = new JPanel(new BorderLayout());
        centerPane.add(installedPane, BorderLayout.CENTER);
        // footer
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> closeDialog());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        footer.add(closeButton);
        // content pane
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        contentPane.add(headerLabel, BorderLayout.PAGE_START);
        contentPane.add(centerPane, BorderLayout.CENTER);
        contentPane.add(footer, BorderLayout.PAGE_END);

        updatePluginList();

        getRootPane().registerKeyboardAction(e -> closeDialog(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(closeButton);
        getRootPane().setFocusable(true);
    }

    private void updateVisualComponents() {
        if (pluginList.getNumberOfItems() > 0) {
            listLayout.show(listContainerPane, "list");
        } else {
            switch (filterComboBox.getSelectedIndex()) {
            case 0:
                emptyLabel.setText("No plug-ins available");
                break;
            case 1:
                emptyLabel.setText("No plug-ins enabled");
                break;
            case 2:
                emptyLabel.setText("No plug-ins disabled");
                break;
            default:
                break;
            }
            listLayout.show(listContainerPane, "empty");
        }
    }

    private void closeDialog() {
        PluginManager.getSingletonInstance().saveSettings();
        setVisible(false);
    }

    private void updatePluginList() {
        pluginList.removeAllEntries();

        int filterIndex = filterComboBox.getSelectedIndex();
        for (PluginContainer plugin : PluginManager.getSingletonInstance().getAllPlugins()) {
            if (filterIndex == 0 || (plugin.isActive() && filterIndex == 1) || (!plugin.isActive() && filterIndex == 2)) {
                pluginList.addEntry(plugin.getName(), new PluginsListEntry(plugin, pluginList));
            }
        }
        pluginList.updateList();
        updateVisualComponents();
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    @Override
    public void listChanged() {
        updatePluginList();
    }

    @Override
    public void init() {
    }

}
