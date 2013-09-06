package org.helioviewer.jhv.plugins.hekplugin.cache.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ButtonCreator;
import org.helioviewer.jhv.gui.ClipBoardCopier;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.TableColumnResizer;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEventTableModel;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;

/**
 * Popup displaying informations about a HEK event.
 * 
 * <p>
 * This panel is a JDialog, so that it can be displayed on top of an GLCanvas,
 * which is not possible for other swing components.
 * 
 * <p>
 * For further informations about solar events, see
 * {@link org.helioviewer.jhv.solarevents}.
 * 
 * @author Markus Langenberg
 * @author Malte Nuhn
 * 
 */
public class HEKEventInformationDialog extends JDialog implements ActionListener, MouseListener, HyperlinkListener {

    private static final long serialVersionUID = 1L;

    private static final Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor clickCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    private boolean isAdvanced = false;

    private HEKEvent event;

    /**
     * EditorPane showing all hyperlinks available for the event
     */
    private JEditorPane hyperLinkPanel = new JEditorPane("text/html", "");

    /**
     * Label showing the event type in the summary of the event
     */
    private JTextArea textType = new JTextArea("N/A");

    /**
     * Label showing the event type in the summary of the event
     */
    private JTextArea textDescription = new JTextArea("N/A");

    /**
     * Label showing the event date in the summary of the event
     */
    private JTextArea textDuration = new JTextArea("N/A");

    /**
     * Label showing the event coordinates in the summary of the event
     */
    private JTextArea textCoordinates = new JTextArea("N/A");

    /**
     * Label showing the event ID in the summary of the event
     */
    private JTextArea textID = new JTextArea("N/A");

    /**
     * Label showing the event icon in the summary of the event
     */
    private JLabel labelIcon = new JLabel("");

    /**
     * Button for showing more/less information about the event
     */
    private JButton moreButton = ButtonCreator.createTextButton(IconBank.getIcon(JHVIcon.SHOW_MORE), "More", "More Event Information", this);

    /**
     * Table showing all event fields
     */
    private JTable infoTable = new JTable();

    /**
     * ScrollPane containing the infoTable
     */
    private JScrollPane informationScroller = new JScrollPane(infoTable);

    /**
     * Preloaded icon
     */
    private static final Icon openIcon = IconBank.getIcon(JHVIcon.SHOW_MORE);

    /**
     * Preloaded icon
     */
    private static final Icon closeIcon = IconBank.getIcon(JHVIcon.SHOW_LESS);

    private void addLineToSummaryPanel(JPanel panel, int y, String fieldName, Component component) {

        GridBagConstraints shortPanelLabelConstraint = new GridBagConstraints();
        shortPanelLabelConstraint.weightx = 0;
        shortPanelLabelConstraint.weighty = 0;
        shortPanelLabelConstraint.anchor = GridBagConstraints.NORTHWEST;
        shortPanelLabelConstraint.gridy = y;

        shortPanelLabelConstraint.gridx = 0;
        panel.add(new JLabel(fieldName), shortPanelLabelConstraint);

        shortPanelLabelConstraint.gridx = 1;
        JLabel space = new JLabel(":");
        space.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 7));
        panel.add(space, shortPanelLabelConstraint);

        shortPanelLabelConstraint.gridx = 2;
        shortPanelLabelConstraint.weightx = 1;
        shortPanelLabelConstraint.fill = GridBagConstraints.BOTH;
        panel.add(component, shortPanelLabelConstraint);

    }

    public JPopupMenu popupMenu;
    public JMenuItem copyValueToClipboardMenuItem;
    public JMenuItem copyNameToClipboardMenuItem;
    public JMenuItem copyBothToClipboardMenuItem;

    /**
     * Default constructor.
     */
    public HEKEventInformationDialog() {
        super();// ImageViewerGui.getMainFrame());
        this.setFocusable(false);
        this.setLayout(new GridBagLayout());
        this.setCursor(clickCursor);
        this.setMinimumSize(new Dimension(550, 50));
        this.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent arg0) {
                // store information to not display the event anymore
                if (event != null) {
                    event.setShowEventInfo(false);
                    event = null;
                }
            }

            public void windowClosing(WindowEvent arg0) {
                // store information to not display the event anymore
                if (event != null) {
                    event.setShowEventInfo(false);
                    event = null;
                }
            }
        });

        this.setAlwaysOnTop(true);
        this.setTitle("Event Information");

        GridBagConstraints emptySpaceConstraint = new GridBagConstraints();
        emptySpaceConstraint.gridx = 0;
        emptySpaceConstraint.gridy = 0;
        emptySpaceConstraint.weightx = 0;
        emptySpaceConstraint.weighty = 0;
        emptySpaceConstraint.anchor = GridBagConstraints.WEST;
        emptySpaceConstraint.ipadx = 10;
        emptySpaceConstraint.ipady = 10;

        // add some empty space
        this.add(new JLabel(""), emptySpaceConstraint);

        // setup the icon's gridbag constraints
        GridBagConstraints iconLabelConstraint = new GridBagConstraints();
        iconLabelConstraint.gridx = 1;
        iconLabelConstraint.gridy = 1;
        iconLabelConstraint.weightx = 0;
        iconLabelConstraint.weighty = 0;
        iconLabelConstraint.fill = GridBagConstraints.NONE;
        iconLabelConstraint.anchor = GridBagConstraints.EAST;

        // add the icon
        this.add(labelIcon, iconLabelConstraint);

        // setup the hyperlinkPanel
        hyperLinkPanel.setEditable(false);
        hyperLinkPanel.setOpaque(false);
        hyperLinkPanel.addHyperlinkListener(this);
        hyperLinkPanel.setFont(labelIcon.getFont());
        hyperLinkPanel.setMargin(new Insets(0, 0, 0, 0));

        textDescription.setLineWrap(true);
        textDescription.setFocusable(false);
        textDescription.setBackground(this.getBackground());
        textDescription.setWrapStyleWord(true);
        textDescription.setFont(labelIcon.getFont());
        textDescription.setMargin(new Insets(0, 0, 0, 0));

        textType.setBackground(this.getBackground());
        textType.setLineWrap(true);
        textType.setWrapStyleWord(true);
        textType.setFont(labelIcon.getFont());
        textType.setMargin(new Insets(0, 0, 0, 0));

        textID.setBackground(this.getBackground());
        textID.setLineWrap(true);
        textID.setWrapStyleWord(true);
        textID.setFont(labelIcon.getFont());
        textID.setMargin(new Insets(0, 0, 0, 0));

        textDuration.setBackground(this.getBackground());
        textDuration.setLineWrap(true);
        textDuration.setWrapStyleWord(true);
        textDuration.setFont(labelIcon.getFont());
        textDuration.setMargin(new Insets(0, 0, 0, 0));

        textCoordinates.setBackground(this.getBackground());
        textCoordinates.setLineWrap(true);
        textCoordinates.setWrapStyleWord(true);
        textCoordinates.setFont(labelIcon.getFont());
        textCoordinates.setMargin(new Insets(0, 0, 0, 0));

        // setup the summary panel
        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 20));
        GridBagConstraints shortPanelLabelConstraint = new GridBagConstraints();
        shortPanelLabelConstraint.weightx = 0;
        shortPanelLabelConstraint.weighty = 0;
        shortPanelLabelConstraint.anchor = GridBagConstraints.WEST;

        this.addLineToSummaryPanel(summaryPanel, 0, "Type", textType);
        this.addLineToSummaryPanel(summaryPanel, 1, "Description", textDescription);
        this.addLineToSummaryPanel(summaryPanel, 2, "ID", textID);
        this.addLineToSummaryPanel(summaryPanel, 3, "Duration", textDuration);
        this.addLineToSummaryPanel(summaryPanel, 4, "Coordinates", textCoordinates);
        this.addLineToSummaryPanel(summaryPanel, 5, "Hyperlinks", hyperLinkPanel);

        // add the shortPanel
        GridBagConstraints shortPanelConstraint = new GridBagConstraints();
        shortPanelConstraint.fill = GridBagConstraints.BOTH;
        shortPanelConstraint.anchor = GridBagConstraints.CENTER;
        shortPanelConstraint.gridx = 3;
        shortPanelConstraint.gridy = 1;
        shortPanelConstraint.weightx = 1;
        shortPanelConstraint.weighty = 0;
        shortPanelConstraint.gridwidth = 1;
        this.add(summaryPanel, shortPanelConstraint);

        // add the more Button
        GridBagConstraints moreButtonConstraint = new GridBagConstraints();
        shortPanelConstraint.fill = GridBagConstraints.NONE;
        moreButtonConstraint.anchor = GridBagConstraints.NORTHEAST;
        moreButtonConstraint.gridx = 3;
        moreButtonConstraint.gridy = 2;
        moreButtonConstraint.gridwidth = 1;
        moreButtonConstraint.weightx = 0;
        moreButtonConstraint.weighty = 0;

        // create and add a panel containing the more button
        JPanel moreButtonPanel = new JPanel();
        moreButtonPanel.add(moreButton);
        moreButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
        this.add(moreButtonPanel, moreButtonConstraint);

        // setup the extended information view
        Dimension informationScrollerSize = new Dimension(100, 150);
        informationScroller.setSize(informationScrollerSize);
        informationScroller.setPreferredSize(informationScrollerSize);
        informationScroller.setVisible(false);

        GridBagConstraints informationScrollerConstraint = new GridBagConstraints();
        informationScrollerConstraint.gridx = 0;
        informationScrollerConstraint.gridy = 3;
        informationScrollerConstraint.weightx = 0;
        informationScrollerConstraint.weighty = 1;
        informationScrollerConstraint.gridwidth = 4;
        informationScrollerConstraint.fill = GridBagConstraints.BOTH;
        this.add(informationScroller, informationScrollerConstraint);

        // setup popup menu
        popupMenu = new JPopupMenu();

        // add a "copy to clipboard" entry
        copyValueToClipboardMenuItem = new JMenuItem("Copy Value(s)");
        copyNameToClipboardMenuItem = new JMenuItem("Copy Name(s)");
        copyBothToClipboardMenuItem = new JMenuItem("Copy Name(s) and Value(s)");

        copyValueToClipboardMenuItem.addActionListener(this);
        copyNameToClipboardMenuItem.addActionListener(this);
        copyBothToClipboardMenuItem.addActionListener(this);

        popupMenu.add(copyValueToClipboardMenuItem);
        // popupMenu.add(copyKeyToClipboardMenuItem);
        popupMenu.add(copyBothToClipboardMenuItem);

        // add a mouselistener showing the popup menu when clicking a cell
        infoTable.addMouseListener(new MouseAdapter() {

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int column = infoTable.columnAtPoint(e.getPoint());
                    int row = infoTable.rowAtPoint(e.getPoint());

                    // check if we clicked some cell
                    if (column != -1 && row != -1) {

                        // if we do not click the selection, make a new
                        // selection
                        boolean clickedSelection = false;

                        for (int selectedRow : infoTable.getSelectedRows()) {
                            if (row == selectedRow) {
                                clickedSelection = true;
                                break;
                            }
                        }

                        if (!clickedSelection) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                infoTable.setRowSelectionInterval(row, row);
                            }
                        }

                        // show the popup
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

        });

    }

    /**
     * Sets the corresponding solar event.
     * 
     * @param newEvent
     *            Corresponding solar event
     */
    public void setEvent(HEKEvent newEvent) {

        if (event == newEvent || newEvent == null)
            return;

        // the old event is no longer displayed
        if (event != null) {
            event.setShowEventInfo(false);
        }

        event = newEvent;

        // mark the new event as currently being displayed
        event.setShowEventInfo(true);
        infoTable.setModel(new HEKEventTableModel(event));
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnResizer.autoResizeTable(infoTable, true);

        Date currentDate = LayersModel.getSingletonInstance().getLastUpdatedTimestamp();

        if (currentDate == null && event.getDuration() != null) {
            currentDate = event.getDuration().getStart();
        }

        String typeAcronym = event.getString("event_type");
        String type = HEKConstants.getSingletonInstance().acronymToString(typeAcronym);
        String description = event.getString("event_description");

        labelIcon.setIcon(new ImageIcon(event.getIcon(true)));
        textType.setText(type);

        // trim away the redundant information
        String id = event.getId();

        if (id.startsWith("ivo://helio-informatics.org/")) {
            id = id.substring(28);
        }

        textID.setText(id);

        // Nasty Workaround to have Radius Unit "m" available
        if (currentDate != null) {
            textCoordinates.setText(event.getStony(currentDate).toString() + "m");
        } else {
            textCoordinates.setText("?");
        }

        if (event.getDuration() != null) {
            textDuration.setText(event.getDuration().getStart() + " - " + event.getDuration().getEnd());
        } else {
            textDuration.setText("?");
        }

        if (description.equals("")) {
            textDescription.setText("No Description Available");
        } else {
            textDescription.setText(description);
        }

        // build a short summary of all containing links to websites
        StringBuffer htmlLinks = new StringBuffer("<html>");

        Font font = hyperLinkPanel.getFont();
        htmlLinks.append("<font style=\"font-family: '" + font.getFamily() + "'; font-size: " + font.getSize() + ";\">");

        // add a link to the HEK summary page
        String archivid = newEvent.getString("kb_archivid");

        if (archivid != null) {
            htmlLinks.append("<a href=\"").append(HEKConstants.HEK_SUMMARY_URL + archivid).append("\"> ").append("HEK Summary Page").append("</a>, ");
        }

        // add all other links that can be found
        for (String fieldName : newEvent.getFields()) {
            String currentField = newEvent.getString(fieldName);
            if (currentField.toLowerCase().startsWith("http://")) {
                htmlLinks.append("<a href=\"").append(currentField).append("\"> ").append(fieldName).append("</a>, ");
            }
        }

        // remove last ","
        if (htmlLinks.toString().contains(", ")) {
            htmlLinks.deleteCharAt(htmlLinks.lastIndexOf(", "));
        }

        htmlLinks.append("</font></html>");

        hyperLinkPanel.setText(htmlLinks.toString());

        pack();

    }

    /**
     * Expand the window to show more details
     * 
     * @param advanced
     */
    private void setAdvanced(boolean advanced) {
        this.isAdvanced = advanced;
        moreButton.setIcon(advanced ? closeIcon : openIcon);
        moreButton.setText(advanced ? "Less" : "More");
        informationScroller.setVisible(advanced);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {

        // did we get a click from "copyXXXtoClipboard"?
        if (e.getSource() == copyNameToClipboardMenuItem || e.getSource() == copyBothToClipboardMenuItem || e.getSource() == copyValueToClipboardMenuItem) {

            boolean saveName = (e.getSource() == copyNameToClipboardMenuItem || e.getSource() == copyBothToClipboardMenuItem);
            boolean saveValue = (e.getSource() == copyValueToClipboardMenuItem || e.getSource() == copyBothToClipboardMenuItem);

            // build a list of selected strings
            StringBuffer cellStringBuffer = new StringBuffer();

            for (int currentRow : infoTable.getSelectedRows()) {
                if (saveName) {
                    cellStringBuffer.append(infoTable.getValueAt(currentRow, 0).toString());
                }

                if (saveName && saveValue) {
                    cellStringBuffer.append(": ");
                }

                if (saveValue) {
                    cellStringBuffer.append(infoTable.getValueAt(currentRow, 1).toString());
                }

                cellStringBuffer.append("\n");

            }

            String cellString = cellStringBuffer.toString();

            // strip last "\n"
            if (cellString.contains("\n")) {
                cellString = cellString.substring(0, cellString.lastIndexOf('\n'));
            }

            ClipBoardCopier.getSingletonInstance().setString(cellString);

        } else if (e.getSource() == moreButton) {

            // flip state
            this.setAdvanced(!this.isAdvanced);
            this.pack();

        } else {
            setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            setCursor(clickCursor);
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            setCursor(defaultCursor);
        } else {
            JHVGlobals.openURL(e.getURL().toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) {
        setCursor(clickCursor);
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) {
        setCursor(defaultCursor);
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
    }

    public HEKEvent getEvent() {
        return event;
    }

}
