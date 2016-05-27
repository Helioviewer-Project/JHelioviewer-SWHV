package org.helioviewer.jhv.gui.dialogs.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

/**
 * Dialog that is used to display information about a plug-in.
 *
 * @author Stephan Pagel
 */
@SuppressWarnings("serial")
public class PluginAboutDialog extends JDialog implements HyperlinkListener {

    private static final Dimension DIALOG_SIZE = new Dimension(500, 350);

    private final Plugin plugin;

    private PluginAboutDialog(Plugin plugin) {
        super(ImageViewerGui.getMainFrame(), "About...", true);
        this.plugin = plugin;

        // dialog
        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        setMinimumSize(DIALOG_SIZE);
        setPreferredSize(DIALOG_SIZE);
        setSize(DIALOG_SIZE);

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
        bottomPane.add(closeButton);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    /**
     * Creates a new JEditorPane in order to display information.
     * */
    private JEditorPane getTextArea(String text) {
        JEditorPane pane = new JEditorPane("text/html", text);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.addHyperlinkListener(this);

        return pane;
    }

    /**
     * Creates the header text of the dialog.
     * */
    private String getHeaderText() {
        String pluginName = plugin.getName() == null ? "Unknown plug-in name" : plugin.getName();
        StringBuilder headerText = new StringBuilder();
        headerText.append("<html><center>");
        headerText.append("<font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + (getFont().getSize() + 2) + ";\">");
        headerText.append("<b>" + pluginName + "</b>");
        headerText.append("</font></center></html>");

        return headerText.toString();
    }

    /**
     * Creates the text containing information about the plug-in and license.
     * */
    private String getContentText() {
        String pluginDesc = plugin.getDescription() == null ? "No description available" : plugin.getDescription();
        String pluginLicense = plugin.getAboutLicenseText() == null ? " " : plugin.getAboutLicenseText();

        StringBuilder contentText = new StringBuilder();
        contentText.append("<html><center>");
        contentText.append("<font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + getFont().getSize() + ";\">");
        contentText.append("<p><b>Plug-in description</b><br>" + pluginDesc + "</p>");
        contentText.append("<p><b>Plug-in license information</b><br>" + pluginLicense + "</p>");
        contentText.append("</font></center></html>");

        return contentText.toString();
    }

    public static void showDialog(Plugin plugin) {
        PluginAboutDialog dialog = new PluginAboutDialog(plugin);
        dialog.showDialog();
    }

    private void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());

        setVisible(true);
    }

    /**
     * Opens a browser or email client after clicking on a hyperlink.
     */
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                TextDialog textDialog = new TextDialog("License - " + e.getDescription().substring(0, e.getDescription().indexOf('.')), ImageViewerGui.class.getResource("/licenses/" + e.getDescription()));
                textDialog.showDialog();
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

}
