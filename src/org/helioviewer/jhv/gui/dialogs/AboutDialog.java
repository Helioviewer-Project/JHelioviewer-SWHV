package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.base.HTMLPane;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.opengl.GLInfo;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class AboutDialog extends StandardDialog implements ShowableDialog, HyperlinkListener {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public AboutDialog() {
        super(JHVFrame.getFrame(), "About JHelioviewer", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        return new CloseButtonPanel(this);
    }

    @Override
    public JComponent createContentPanel() {
        String text = "<center>This software was built using several components including:</center><ul>" +
                "<li><a href=\"http://www.kakadusoftware.com\">Kakadu</a> JPEG2000 Toolkit, © 2015, <a href='/licenses/Kakadu.txt'>licensed</a> from NewSouth Innovations Ltd.</li>" +
                "<li><a href=\"https://jogamp.org\">JogAmp</a> libraries for Java 3D graphics.</li>" +
                "<li><a href=\"https://github.com/JOML-CI/JOML\">JOML</a>, a Java math library for OpenGL rendering calculations.</li>" +
                "<li><a href=\"https://naif.jpl.nasa.gov/naif/\">SPICE</a>, the observation geometry system for space science missions.</li>" +
                "<li><a href=\"https://ffmpeg.org\">FFmpeg</a>, the leading multimedia framework.</li>" +
                "<li><a href=\"https://github.com/square/okio\">Okio</a> and <a href=\"https://github.com/square/okhttp\">OkHttp</a> libraries by Square, Inc.</li>" +
                "<li><a href=\"https://github.com/google/guava\">Guava</a>, Google core libraries for Java.</li>" +
                "<li><a href=\"https://github.com/ben-manes/caffeine\">Caffeine</a>, a high performance caching library for Java.</li>" +
                "<li><a href=\"http://www.ehcache.org\">Ehcache</a> library.</li>" +
                "<li><a href=\"http://www.ocpsoft.org/prettytime/nlp\">PrettyTime NLP</a>, a human time parsing library.</li>" +
                "<li><a href=\"https://github.com/jidesoft/jide-oss\">JIDE Common Layer</a>, © 2002-2017, JIDE Software, Inc.</li>" +
                "<li><a href=\"https://github.com/JFormDesigner/FlatLaf\">FlatLaf</a>, a modern cross-platform Look and Feel for Java Swing.</li>" +
                "<li><a href=\"http://www.star.bristol.ac.uk/%7Embt/jsamp/index.html\">JSAMP</a> toolkit for the Simple Applications Messaging Protocol.</li>" +
                "<li><a href=\"http://nom-tam-fits.github.io/nom-tam-fits\">FITS</a> in Java public domain library.</li>" +
                "<li><a href=\"https://github.com/xerial/sqlite-jdbc\">Xerial</a> SQLite JDBC driver.</li>" +
                "<li><a href=\"https://www.ej-technologies.com/products/install4j/overview.html\">install4j</a>, the multi-platform installer builder.</li>";

        HTMLPane pane = new HTMLPane();
        pane.setText(text);
        pane.addHyperlinkListener(this);
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return new JScrollPane(pane);
    }

    @Override
    public JComponent createBannerPanel() {
        int delta = 3;
        int fontSize = UIGlobals.uiFont.getSize();
        String text = "<center><b><span style='font-size:" + (fontSize + delta) + "pt'>" +
                "<a href='http://www.jhelioviewer.org'>" + JHVGlobals.programName + "</a></span><br/>" +
                "Version " + JHVGlobals.version + '.' + JHVGlobals.revision + "</b><br/>" +
                "<span style='font-size:" + (fontSize - delta) + "pt'>" + JHVGlobals.versionDetail + "<br/>" + GLInfo.glVersion + "</span><br/><br/>" +
                "© 2021 <a href='http://www.jhelioviewer.org/about.html'>ESA JHelioviewer Team</a><br/>" +
                "Part of the ESA/NASA Helioviewer Project<br/>" +
                "Enhanced at ROB/SIDC (ESA Contract No. 4000107325/12/NL/AK)<br/><br/>" +
                "JHelioviewer is released under the<br/>" +
                "<a href='/licenses/JHelioviewer.txt'>Mozilla Public License Version 2.0</a><br/>" +
                "and its use is governed by the<br/>" +
                "<a href='/licenses/EULA.txt'>End-User License Agreement</a><br/><br/>" +
                "Contact: <a href='mailto:Daniel.Mueller@esa.int'>Daniel.Mueller@esa.int</a>";

        HTMLPane pane = new HTMLPane();
        pane.setText(text);
        pane.addHyperlinkListener(this);
        pane.setOpaque(false);

        JPanel banner = new JPanel(new BorderLayout());
        banner.add(new JLabel(IconBank.getAIcon()/*getIcon(JHVIcon.HVLOGO_SMALL)*/), BorderLayout.LINE_START);
        banner.add(pane, BorderLayout.LINE_END);
        banner.setBorder(BorderFactory.createEmptyBorder(5, 35, 5, 35));
        return banner;
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                String res = e.getDescription();
                String name = res.substring(Math.max(0, res.lastIndexOf('/') + 1));
                try (InputStream is = FileUtils.getResource(res)) {
                    new TextDialog("License - " + name.substring(0, name.indexOf('.')), FileUtils.streamToString(is), true).showDialog();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "hyperlinkUpdate", ex);
                }
            } else {
                JHVGlobals.openURL(e.getURL().toString());
            }
        }
    }

}
