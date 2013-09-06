package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

/**
 * Dialog used to display all supported characters for a date pattern. It also
 * gives some examples how to create a pattern.
 * 
 * @author Stephan Pagel
 */
public class DateFormatInfoDialog extends JDialog implements ActionListener, ShowableDialog {

    // ////////////////////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private JButton closeButton = new JButton("Close");

    // ////////////////////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     */
    public DateFormatInfoDialog() {

        super(ImageViewerGui.getMainFrame(), "Date format information", true);
        setLayout(new BorderLayout());
        setResizable(false);

        final String sep = System.getProperty("line.separator");

        // the content panel:
        JTextArea content = new JTextArea("The following list shows the supported date patterns:" + sep + sep + "Letter  Date Component        Examples" + sep + "y       Year                  2009; 09" + sep + "M       Month of year         July; Jul; 07" + sep + "d       Day of month          10" + sep + "E       Day of week           Tuesday; Tue" + sep + "w       Week of year          34" + sep + "D       Day of year           189" + sep + sep + "Month:" + sep + "If more than 3 letters are entered, the month is" + sep + "interpreted as text, otherwise it is interpreted as a number." + sep + sep + "Year:" + sep + "If the number of pattern letters is 2, the year is truncated" + sep + "to 2 digits." + sep + sep + "Examples:" + sep + sep + "Pattern           Result" + sep + "yyyy/MM/dd        2003/10/05" + sep + "MMMM dd, yyyy     October 05, 2003" + sep + "EEE, MMM d, yyyy  Sun, Oct 5, 2003");

        content.setEditable(false);
        content.setFont(new Font("Courier", Font.PLAIN, 13));
        content.setBackground(getBackground());
        content.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        add(content, BorderLayout.CENTER);

        // the button panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttonsPanel.add(closeButton);

        // add action listener to button
        closeButton.addActionListener(this);

        add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Closes the dialog.
     */
    public void actionPerformed(ActionEvent arg0) {
        dispose();
    }

    /**
     * {@inheritDoc}
     */
    public void showDialog() {
        pack();
        setSize(getPreferredSize().width, getPreferredSize().height);
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

}
