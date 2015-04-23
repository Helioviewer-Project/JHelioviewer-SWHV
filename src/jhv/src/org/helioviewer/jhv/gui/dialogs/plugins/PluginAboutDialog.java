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
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.TextDialog;
import org.helioviewer.viewmodelplugin.interfaces.Plugin;

/**
 * Dialog that is used to display information about a plug-in.
 * 
 * @author Stephan Pagel
 */
public class PluginAboutDialog extends JDialog implements ActionListener, HyperlinkListener {

    private static final Dimension DIALOG_SIZE = new Dimension(500, 350);

    private final Plugin plugin;

    private final JPanel contentPane = new JPanel();
    private final JButton closeButton = new JButton("Close");

    private PluginAboutDialog(final Plugin plugin) {
        super(ImageViewerGui.getMainFrame(), "About...", true);
        this.plugin = plugin;
        initVisualComponents();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {
        // dialog
        setContentPane(contentPane);

        setMinimumSize(DIALOG_SIZE);
        setPreferredSize(DIALOG_SIZE);
        setSize(DIALOG_SIZE);

        // content
        final JPanel topPane = new JPanel();
        final JPanel centerPane = new JPanel();
        final JPanel bottomPane = new JPanel();

        contentPane.setLayout(new BorderLayout());
        contentPane.add(topPane, BorderLayout.PAGE_START);
        contentPane.add(centerPane, BorderLayout.CENTER);
        contentPane.add(bottomPane, BorderLayout.PAGE_END);

        // header
        topPane.setLayout(new BorderLayout());
        topPane.add(getTextArea(getHeaderText()), BorderLayout.CENTER);

        // main part
        final JScrollPane scrollPane = new JScrollPane(getTextArea(getContentText()), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        centerPane.setLayout(new BorderLayout());
        centerPane.add(scrollPane, BorderLayout.CENTER);

        // footer
        bottomPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        bottomPane.add(closeButton);

        closeButton.addActionListener(this);
    }

    /**
     * Creates a new JEditorPane in order to display information.
     * */
    private JEditorPane getTextArea(final String text) {
        final JEditorPane pane = new JEditorPane("text/html", text);
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
        final String pluginName = plugin.getName() == null ? "Unknown plug-in name" : plugin.getName();
        final StringBuilder headerText = new StringBuilder();
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
        final String pluginDesc = plugin.getDescription() == null ? "No description available" : plugin.getDescription();
        final String pluginLicense = plugin.getAboutLicenseText() == null ? " " : plugin.getAboutLicenseText();

        final StringBuilder contentText = new StringBuilder();
        contentText.append("<html><center>");
        contentText.append("<font style=\"font-family: '" + getFont().getFamily() + "'; font-size: " + getFont().getSize() + ";\">");
        contentText.append("<p><b>Plug-in description</b><br>" + pluginDesc + "</p>");
        contentText.append("<p><b>Plug-in license information</b><br>" + pluginLicense + "</p>");
        contentText.append("</font></center></html>");

        return contentText.toString();
    }

    // /**
    // * Formats the version and revision string
    // *
    // * @return the formatted version and revision string
    // */
    // private String getVersionString() {
    // //TODO: Get version of plug-in
    // String versionString = null;
    // String revisionString = null;
    //
    // if (versionString == null) {
    // Log.warn(">> PluginAboutDialog.getVersionString() > No version found. Use default version and revision strings.");
    // versionString = "?.?.?";
    // }
    //
    // if (revisionString == null) {
    // Log.warn(">> PluginAboutDialog.getVersionString() > No revision found. Use default version and revision strings.");
    // revisionString = "?";
    // }
    // return versionString + " - Revision " + revisionString;
    // }

    /**
     * Brings up the dialog.
     * */
    public static void showDialog(final Plugin plugin) {
        final PluginAboutDialog dialog = new PluginAboutDialog(plugin);
        dialog.showDialog();
    }

    /**
     * Brings up the dialog.
     * */
    private void showDialog() {
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());

        setVisible(true);
    }

    // Action Listener

    /**
     * {@inheritDoc}
     * */
    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getSource().equals(closeButton)) {
            dispose();
        }
    }

    // Action Listener

    /**
     * Opens a browser or email client after clicking on a hyperlink.
     */
    public void hyperlinkUpdate(final HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                final TextDialog textDialog = new TextDialog("License - " + e.getDescription().substring(0, e.getDescription().indexOf('.')), ImageViewerGui.class.getResource("/licenses/" + e.getDescription()));
                textDialog.showDialog();
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

}
