package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

@SuppressWarnings("serial")
class PluginAboutDialog extends JDialog implements HyperlinkListener {

    private final Plugin plugin;

    private PluginAboutDialog(Plugin _plugin) {
        super(ImageViewerGui.getMainFrame(), "About...", true);
        plugin = _plugin;

        // dialog
        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        // content
        JPanel topPane = new JPanel(new BorderLayout());
        JPanel centerPane = new JPanel(new BorderLayout());
        JPanel bottomPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        contentPane.add(topPane, BorderLayout.PAGE_START);
        contentPane.add(centerPane, BorderLayout.CENTER);
        contentPane.add(bottomPane, BorderLayout.PAGE_END);

        // header
        topPane.add(getTextArea(getHeaderText()), BorderLayout.CENTER);
        // main part
        JScrollPane scrollPane = new JScrollPane(getTextArea(getContentText()), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        centerPane.add(scrollPane, BorderLayout.CENTER);

        // footer
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> setVisible(false));
        bottomPane.add(closeButton);

        getRootPane().registerKeyboardAction(e -> setVisible(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(closeButton);
        getRootPane().setFocusable(true);
    }

    private JEditorPane getTextArea(String text) {
        JEditorPane pane = new JEditorPane("text/html", text);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.addHyperlinkListener(this);
        return pane;
    }

    private String getHeaderText() {
        String pluginName = plugin.getName() == null ? "Unknown plug-in name" : plugin.getName();
        return "<html><center><font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + (getFont().getSize() + 2) + ";\">" +
                "<b>" + pluginName + "</b></font></center></html>";
    }

    private String getContentText() {
        String pluginDesc = plugin.getDescription() == null ? "No description available" : plugin.getDescription();
        String pluginLicense = plugin.getAboutLicenseText() == null ? " " : plugin.getAboutLicenseText();
        return "<html><center><font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + getFont().getSize() + ";\">" +
                "<p><b>Plug-in description</b><br>" + pluginDesc + "</p><p><b>Plug-in license information</b><br>" + pluginLicense + "</p></font></center></html>";
    }

    public static void showDialog(Plugin plugin) {
        PluginAboutDialog dialog = new PluginAboutDialog(plugin);
        dialog._show();
    }

    private void _show() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                TextDialog textDialog = new TextDialog("License - " + e.getDescription().substring(0, e.getDescription().indexOf('.')),
                                                       FileUtils.URL2String(ImageViewerGui.class.getResource("/licenses/" + e.getDescription())));
                textDialog.showDialog();
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

}
