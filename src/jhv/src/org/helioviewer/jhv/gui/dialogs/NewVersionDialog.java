package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
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
@SuppressWarnings("serial")
public class NewVersionDialog extends JDialog implements ActionListener, ShowableDialog, HyperlinkListener {
    /**
     * New setting for check.update.next
     */
    private int nextCheck = 0;
    /**
     * Suspended startups when clicked remindMeLater
     */
    private final int suspendedStarts = 5;

    private JEditorPane messagePane;

    public NewVersionDialog(boolean verbose) {
        super(ImageViewerGui.getMainFrame(), false);
        setLayout(new BorderLayout());
        setResizable(false);

        messagePane = new JEditorPane("text/html", "");

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
                JHVGlobals.openURL(JHVGlobals.downloadURL);
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

        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

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
    public void init(String newVersion, String message) {
        this.setTitle("JHelioviewer " + newVersion + " is now available");
        messagePane.setText(message);
    }

    public void init() {
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
        dispose();
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
