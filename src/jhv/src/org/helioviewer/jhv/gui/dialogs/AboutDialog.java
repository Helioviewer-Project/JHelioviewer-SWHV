package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

// Dialog that is used to display information about the program, including version and contact information
@SuppressWarnings("serial")
public class AboutDialog extends JDialog implements ShowableDialog, HyperlinkListener {

    private final JScrollPane scrollPane;

    public AboutDialog() {
        super(ImageViewerGui.getMainFrame(), "About JHelioviewer", true);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel contentPane = new JPanel(new BorderLayout());

        JLabel logo = new JLabel(IconBank.getIcon(JHVIcon.HVLOGO_SMALL));
        logo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(logo, BorderLayout.WEST);

        Font font = getFont();

        JEditorPane content = new JEditorPane("text/html", "<html><center><font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">" +
                "<b>" + getVersionString() + "</b><br>" +
                '\u00A9' + "2016 <a href='http://www.jhelioviewer.org/about.html'>ESA JHelioviewer Team</a><br>" +
                "Part of the ESA/NASA Helioviewer Project<br>" +
                "Enhanced at ROB/SIDC (ESA Contract No. 4000107325/12/NL/AK)<br><br>" +
                "JHelioviewer is released under the<br>" +
                "<a href=JHelioviewer.txt>Mozilla Public License Version 2.0</a><br><br>" +
                "<a href='http://www.jhelioviewer.org'>www.jhelioviewer.org</a><br><br>" +
                "Contact: <a href='mailto:Daniel.Mueller@esa.int'>Daniel.Mueller@esa.int</a>" +
                "</font></center></html>");
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setEditable(false);
        content.setOpaque(false);
        content.addHyperlinkListener(this);
        contentPane.add(content, BorderLayout.CENTER);

        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.PAGE_AXIS));

        boxPanel.add(new JSeparator());
        StringBuilder text = new StringBuilder();
        text.append("<html><font style=\"font-family: '").append(font.getFamily()).append("'; font-size: ").append(font.getSize()).append(";\">");
        text.append("This software uses the <a href=\"http://www.kakadusoftware.com\">Kakadu JPEG2000 Toolkit</a>,<br> " + '\u00A9' + " 2015, NewSouth Innovations Ltd (NSI), <a href=Kakadu.txt>(License)</a><br>");
        text.append("<p>This software uses <a href=\"https://jogamp.org\">JogAmp</a>, the Java high performance libraries for 3D Graphics, Multimedia and Processing,<br>").append('\u00A9').append(" JogAmp Community and others<br>");
        text.append("<p>This software uses <a href=\"https://commons.apache.org\">Apache Commons</a>,<br>").append('\u00A9').append(" 2001-2015, The Apache Software Foundation<br>");
        text.append("<p>This software uses <a href=\"http://logging.apache.org/log4j/index.html\">log4j</a> from the Apache Logging Services Project, licensed under the <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License version 2.0</a>,<br>" + '\u00A9' + " 2010, The Apache Software Foundation<br>");
        text.append("<p>This software uses the <a href=\"https://github.com/stleary/JSON-java\">JSON in Java</a> Library, licensed under a custom <a href=\"http://www.json.org/license.html\">License</a>.");
        text.append("<p>This software uses the <a href=\"https://github.com/xerial/sqlite-jdbc\">Xerial SQLite JDBC Driver</a>, licensed under the <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License version 2.0</a>.<br>");
        text.append("<p>This software uses <a href=\"http://jcodec.org\">JCodec</a>, licensed under the FreeBSD License.<br>");
        text.append("<p>This software uses the <a href=\"http://nom-tam-fits.github.io/nom-tam-fits/\">FITS in Java</a> public domain library.");
        text.append("<p>This software uses the <a href=\"http://www.davekoelle.com/alphanum.html\">Alphanum Algorithm</a>, licensed under the LGPLv2.1.<br> Its source code can be downloaded <a href=\"http://jhelioviewer.org/libjhv/external/AlphanumComparator.java\">here</a>.<br>");

        for (PluginContainer pluginContainer : PluginManager.getSingletonInstance().getAllPlugins()) {
            Plugin plugin = pluginContainer.getPlugin();
            String pluginName = plugin.getName();
            String pluginAboutLicense = plugin.getAboutLicenseText();

            if (pluginName == null || pluginName.isEmpty()) {
                pluginName = "Unknown Plugin";
            }

            if (pluginAboutLicense == null || pluginAboutLicense.isEmpty()) {
                pluginAboutLicense = "No License Text Available.";
            }

            text.append("<p>============ Plugin: ").append(pluginName).append(" ============<br>");
            text.append(pluginAboutLicense);

        }

        JEditorPane license = new JEditorPane("text/html", text.toString());

        license.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        license.setEditable(false);
        license.setOpaque(false);
        license.addHyperlinkListener(this);
        boxPanel.add(license);

        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        closeButtonContainer.add(closeButton);
        boxPanel.add(closeButtonContainer);

        contentPane.add(boxPanel, BorderLayout.SOUTH);

        scrollPane = new JScrollPane(contentPane);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.getVerticalScrollBar().setUnitIncrement(100);
        add(scrollPane);

        setPreferredSize(new Dimension(getPreferredSize().width + 50, 600));

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(closeButton);
        getRootPane().setFocusable(true);
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
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

    /**
     * Formats the version and revision string
     *
     * @return the formatted version and revision string
     */
    private static String getVersionString() {
        String versionString = JHVGlobals.getJhvVersion();
        String revisionString = JHVGlobals.getJhvRevision();

        if (versionString == null) {
            Log.warn("AboutDialog.getVersionString() > No version found. Use default version and revision strings.");
            versionString = "2.-1.-1";
        }

        if (revisionString == null) {
            Log.warn("AboutDialog.getVersionString() > No revision found. Use default version and revision strings.");
            revisionString = "-1";
        }
        return JHVGlobals.programName + " " + versionString + " - Revision " + revisionString;
    }

    @Override
    public void init() {
    }

    public static void dialogShow() {
        AboutDialog dialog = new AboutDialog();
        dialog.init();
        dialog.showDialog();
    }

}
