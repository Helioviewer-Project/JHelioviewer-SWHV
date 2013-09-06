package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

/**
 * Dialog to show that a new version is available
 * 
 * @author Helge Dietert
 */
public class NewVersionDialog extends JDialog implements ActionListener, ShowableDialog, HyperlinkListener {

    private static final long serialVersionUID = 1L;

    /**
     * New setting for check.update.next
     */
    private int nextCheck = 0;
    /**
     * Suspended startups when clicked remindMeLater
     */
    private final int suspendedStarts = 5;

    /**
     * Creates a dialog with the given parameters
     * 
     * @param newVersion
     *            new version which is available
     * @param message
     *            Message for this new version
     * @param verbose
     *            If false show suspension buttons
     */
    public NewVersionDialog(String newVersion, String message, boolean verbose) {
        super(ImageViewerGui.getMainFrame(), "JHelioviewer " + newVersion + " is now available", false);
        setLayout(new BorderLayout());
        setResizable(false);

        JEditorPane messagePane = new JEditorPane("text/html", message);

        messagePane.setEditable(false);
        messagePane.setOpaque(false);
        messagePane.addHyperlinkListener(this);
        messagePane.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        add(messagePane, BorderLayout.CENTER);

        JPanel closeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        final JButton downloadButton = new JButton("Download");
        closeButtonContainer.add(downloadButton);
        downloadButton.addActionListener(this);
        downloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                JHVGlobals.openURL("http://jhelioviewer.org");
            }
        });

        if (!verbose) {
            final JButton laterButton = new JButton("Remind me later");
            closeButtonContainer.add(laterButton);
            laterButton.addActionListener(this);
            laterButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg) {
                    nextCheck = suspendedStarts;
                }
            });
        }

        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        closeButtonContainer.add(closeButton);
        add(closeButtonContainer, BorderLayout.SOUTH);
    }

    /**
     * {@inheritDoc}
     */
    public void showDialog() {
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    /**
     * Closes the dialog.
     */
    public void actionPerformed(ActionEvent a) {
        this.dispose();
    }

    /**
     * Opens a browser or email client after clicking on a hyperlink.
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JHVGlobals.openURL(e.getURL().toString());
        }
    }

    /**
     * New proposed setting for udpate.check.next
     */
    public int getNextCheck() {
        return nextCheck;
    }
}
