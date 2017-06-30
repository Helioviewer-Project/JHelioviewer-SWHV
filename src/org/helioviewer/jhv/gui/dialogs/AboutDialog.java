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
        String text = "<center>This software was built using several components including:</center><ul>" +
            "<li>The <a href=\"http://www.kakadusoftware.com\">Kakadu</a> JPEG2000 Toolkit, ©2015, <a href=Kakadu.txt>licensed</a> from NewSouth Innovations Ltd.</li>" +
            "<li><a href=\"https://jogamp.org\">JogAmp</a>, the Java libraries for 3D Graphics, Multimedia and Processing.</li>" +
            "<li>The <a href=\"https://github.com/stleary/JSON-java\">JSON in Java</a> library.</li>" +
            "<li>The JSON <a href=\"https://github.com/everit-org/json-schema\">Schema Validator</a> library.</li>" +
            "<li>The <a href=\"https://github.com/xerial/sqlite-jdbc\">Xerial</a> SQLite JDBC driver.</li>" +
            "<li><a href=\"http://jcodec.org\">JCodec</a>, a pure Java video codec library.</li>" +
            "<li>The <a href=\"http://www.star.bristol.ac.uk/%7Embt/jsamp/index.html\">JSAMP</a> toolkit for the Simple Applications Messaging Protocol.</li>" +
            "<li>The <a href=\"http://www.ocpsoft.org/prettytime/nlp\">PrettyTime NLP</a> human time parsing library.</li>" +
            "<li><a href=\"https://github.com/jidesoft/jide-oss\">JIDE Common Layer</a>, ©2002-2017, JIDE Software, Inc.</li>" +
            "<li>The <a href=\"http://nom-tam-fits.github.io/nom-tam-fits/\">FITS in Java</a> public domain library.</li>" +
            "<li>The <a href=\"http://logging.apache.org/log4j/index.html\">log4j</a> logging library.</li>";

        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText(text);
        pane.setEditable(false);
        pane.addHyperlinkListener(this);
        pane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return new JScrollPane(pane);
    }

    @Override
    public JComponent createBannerPanel() {
        String text = "<center><b><big>" + JHVGlobals.programName + "</big><br/>Version " + JHVGlobals.version + '.' + JHVGlobals.revision + "</b><br/>" +
                "©2017 <a href='http://www.jhelioviewer.org/about.html'>ESA JHelioviewer Team</a><br/>" +
                "Part of the ESA/NASA Helioviewer Project<br/>" +
                "Enhanced at ROB/SIDC (ESA Contract No. 4000107325/12/NL/AK)<br/><br/>" +
                "JHelioviewer is released under the<br/>" +
                "<a href=JHelioviewer.txt>Mozilla Public License Version 2.0</a><br/><br/>" +
                "<a href='http://www.jhelioviewer.org'>www.jhelioviewer.org</a><br/><br/>" +
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
