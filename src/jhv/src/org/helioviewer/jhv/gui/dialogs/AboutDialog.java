package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.plugin.controller.PluginContainer;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class AboutDialog extends StandardDialog implements ShowableDialog, HyperlinkListener {

    public AboutDialog() {
        super(ImageViewerGui.getMainFrame(), "About JHelioviewer", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        return new CloseButtonPanel(this);
    }

    @Override
    public JComponent createContentPanel() {
        StringBuilder text = new StringBuilder("This software uses the <a href=\"http://www.kakadusoftware.com\">Kakadu JPEG2000 Toolkit</a>,<br>©2015, NewSouth Innovations Ltd (NSI). <a href=Kakadu.txt>License</a><br>");
        text.append("<p>This software uses <a href=\"https://jogamp.org\">JogAmp</a>, the Java high performance libraries for 3D Graphics, Multimedia and Processing,<br>©JogAmp Community and others.<br>");
        text.append("<p>This software uses <a href=\"https://commons.apache.org\">Apache Commons</a>, ©2001-2015, The Apache Software Foundation.<br>");
        text.append("<p>This software uses <a href=\"http://logging.apache.org/log4j/index.html\">log4j</a> from the Apache Logging Services Project, licensed under the <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License version 2.0</a>,<br>©2010, The Apache Software Foundation.<br>");
        text.append("<p>This software uses the <a href=\"https://github.com/stleary/JSON-java\">JSON in Java</a> Library, licensed under a custom <a href=\"http://www.json.org/license.html\">License</a>.");
        text.append("<p>This software uses the <a href=\"https://github.com/xerial/sqlite-jdbc\">Xerial SQLite JDBC Driver</a>, licensed under the <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License version 2.0</a>.<br>");
        text.append("<p>This software uses <a href=\"http://jcodec.org\">JCodec</a>, licensed under the FreeBSD License.<br>");
        text.append("<p>This software uses <a href=\"https://github.com/jidesoft/jide-oss\">JIDE Common Layer</a>, ©2002-2017, JIDE Software, Inc.<br>");
        text.append("<p>This software uses the <a href=\"http://nom-tam-fits.github.io/nom-tam-fits/\">FITS in Java</a> public domain library.<br>");

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

        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText(text.toString());
        pane.setEditable(false);
        pane.addHyperlinkListener(this);
        pane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return new JScrollPane(pane);
    }

    @Override
    public JComponent createBannerPanel() {
        String text = "<center><b><big>" + JHVGlobals.programName + "</big><br>Version " + JHVGlobals.version + '.' + JHVGlobals.revision + "</b><br>" +
                "©2017 <a href='http://www.jhelioviewer.org/about.html'>ESA JHelioviewer Team</a><br>" +
                "Part of the ESA/NASA Helioviewer Project<br>" +
                "Enhanced at ROB/SIDC (ESA Contract No. 4000107325/12/NL/AK)<br><br>" +
                "JHelioviewer is released under the<br>" +
                "<a href=JHelioviewer.txt>Mozilla Public License Version 2.0</a><br><br>" +
                "<a href='http://www.jhelioviewer.org'>www.jhelioviewer.org</a><br><br>" +
                "Contact: <a href='mailto:Daniel.Mueller@esa.int'>Daniel.Mueller@esa.int</a>";

        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText(text);
        pane.setEditable(false);
        pane.addHyperlinkListener(this);
        pane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setOpaque(false);

        JPanel banner = new JPanel(new BorderLayout());
        banner.add(new JLabel(IconBank.getIcon(JHVIcon.HVLOGO_SMALL)), BorderLayout.WEST);
        banner.add(pane, BorderLayout.EAST);
        banner.setBorder(BorderFactory.createEmptyBorder(5, 35, 5, 35));
        return banner;
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
                TextDialog textDialog = new TextDialog("License - " + e.getDescription().substring(0, e.getDescription().indexOf('.')),
                                                       FileUtils.URL2String(ImageViewerGui.class.getResource("/licenses/" + e.getDescription())), true);
                textDialog.showDialog();
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

}
