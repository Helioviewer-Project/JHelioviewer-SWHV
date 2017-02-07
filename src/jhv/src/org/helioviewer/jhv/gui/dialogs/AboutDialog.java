package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
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

@SuppressWarnings("serial")
public class AboutDialog extends JDialog implements ShowableDialog, HyperlinkListener {

    public AboutDialog() {
        super(ImageViewerGui.getMainFrame(), "About JHelioviewer", true);

        JPanel contentPane = new JPanel(new BorderLayout());

        JLabel logo = new JLabel(IconBank.getIcon(JHVIcon.HVLOGO_SMALL));
        logo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(logo, BorderLayout.WEST);

        StringBuilder text = new StringBuilder();
        text.append("<center><b>").append(getVersionString()).append("</b><br>");
        text.append("©2017 <a href='http://www.jhelioviewer.org/about.html'>ESA JHelioviewer Team</a><br>");
        text.append("Part of the ESA/NASA Helioviewer Project<br>");
        text.append("Enhanced at ROB/SIDC (ESA Contract No. 4000107325/12/NL/AK)<br><br>");
        text.append("JHelioviewer is released under the<br>");
        text.append("<a href=JHelioviewer.txt>Mozilla Public License Version 2.0</a><br><br>");
        text.append("<a href='http://www.jhelioviewer.org'>www.jhelioviewer.org</a><br><br>");
        text.append("Contact: <a href='mailto:Daniel.Mueller@esa.int'>Daniel.Mueller@esa.int</a></center>");
        text.append("<hr>This software uses the <a href=\"http://www.kakadusoftware.com\">Kakadu JPEG2000 Toolkit</a>,<br>©2015, NewSouth Innovations Ltd (NSI), <a href=Kakadu.txt>(License)</a><br>");
        text.append("<p>This software uses <a href=\"https://jogamp.org\">JogAmp</a>, the Java high performance libraries for 3D Graphics, Multimedia and Processing,<br>©JogAmp Community and others<br>");
        text.append("<p>This software uses <a href=\"https://commons.apache.org\">Apache Commons</a>,<br>©2001-2015, The Apache Software Foundation<br>");
        text.append("<p>This software uses <a href=\"http://logging.apache.org/log4j/index.html\">log4j</a> from the Apache Logging Services Project, licensed under the <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License version 2.0</a>,<br>©2010, The Apache Software Foundation<br>");
        text.append("<p>This software uses the <a href=\"https://github.com/stleary/JSON-java\">JSON in Java</a> Library, licensed under a custom <a href=\"http://www.json.org/license.html\">License</a>.");
        text.append("<p>This software uses the <a href=\"https://github.com/xerial/sqlite-jdbc\">Xerial SQLite JDBC Driver</a>, licensed under the <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License version 2.0</a>.<br>");
        text.append("<p>This software uses <a href=\"http://jcodec.org\">JCodec</a>, licensed under the FreeBSD License.<br>");
        text.append("<p>This software uses <a href=\"https://github.com/jidesoft/jide-oss\">JIDE Common Layer</a>, © 2002-2017, JIDE Software, Inc.<br>");
        text.append("<p>This software uses the <a href=\"http://nom-tam-fits.github.io/nom-tam-fits/\">FITS in Java</a> public domain library.");
        text.append("<p>This software uses the <a href=\"http://www.davekoelle.com/alphanum.html\">Alphanum Algorithm</a>, licensed under the LGPLv2.1.<br>Its source code can be downloaded <a href=\"http://jhelioviewer.org/libjhv/external/AlphanumComparator.java\">here</a>.<br>");

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

            text.append("<hr><b>").append(pluginName).append("</b><br>").append(pluginAboutLicense);
        }

        JEditorPane content = new JEditorPane("text/html", text.toString());
        content.setOpaque(false);
        content.setEditable(false);
        content.addHyperlinkListener(this);
        content.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(content, BorderLayout.CENTER);

        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> setVisible(false));
        closeButtonContainer.add(closeButton);
        contentPane.add(closeButtonContainer, BorderLayout.SOUTH);

        add(new JScrollPane(contentPane));

        getRootPane().registerKeyboardAction(e -> setVisible(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(closeButton);
        getRootPane().setFocusable(true);
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

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

    public static void dialogShow() {
        new AboutDialog().showDialog();
    }

}
