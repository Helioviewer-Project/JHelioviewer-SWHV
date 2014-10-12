package org.helioviewer.jhv.data.guielements;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;

/**
 * This is a panel describing an event in short.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventDescriptionPanel extends JPanel {
    /** UID */
    private static final long serialVersionUID = -2859591591100257931L;

    /** The event */
    private final JHVEvent event;

    /** Label showing the event icon in the summary of the event */
    private JLabel labelIcon = new JLabel("");

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
    private JTextArea textStartTime = new JTextArea("N/A");

    /**
     * Label showing the event coordinates in the summary of the event
     */
    private JTextArea textEndTime = new JTextArea("N/A");

    /**
     * 
     * 
     * @param event
     */
    public EventDescriptionPanel(JHVEvent event) {
        this.event = event;
        initVisualComponents();
    }

    private void initVisualComponents() {
        initDescriptionText();

        setLayout(new GridBagLayout());

        labelIcon = new JLabel(event.getIcon());

        GridBagConstraints iconLabelConstraint = new GridBagConstraints();
        iconLabelConstraint.gridx = 0;
        iconLabelConstraint.gridy = 0;
        iconLabelConstraint.weightx = 0;
        iconLabelConstraint.weighty = 0;
        iconLabelConstraint.fill = GridBagConstraints.NONE;
        iconLabelConstraint.anchor = GridBagConstraints.CENTER;
        iconLabelConstraint.ipadx = 10;

        // add the icon
        this.add(labelIcon, iconLabelConstraint);

        // setup the summary panel
        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 20));

        this.addLineToSummaryPanel(summaryPanel, 0, "Type", textType);
        this.addLineToSummaryPanel(summaryPanel, 1, "Description", textDescription);
        this.addLineToSummaryPanel(summaryPanel, 2, "Start Time", textStartTime);
        this.addLineToSummaryPanel(summaryPanel, 3, "End Time", textEndTime);

        // add the shortPanel
        GridBagConstraints shortPanelConstraint = new GridBagConstraints();
        shortPanelConstraint.fill = GridBagConstraints.BOTH;
        shortPanelConstraint.anchor = GridBagConstraints.CENTER;
        shortPanelConstraint.gridx = 1;
        shortPanelConstraint.gridy = 0;
        shortPanelConstraint.weightx = 1;
        shortPanelConstraint.weighty = 0;
        shortPanelConstraint.gridwidth = 1;
        this.add(summaryPanel, shortPanelConstraint);

    }

    /**
     * Initialize the short description.
     * 
     */
    private void initDescriptionText() {
        textDescription = new JTextArea(event.getShortDescription());
        textDescription.setLineWrap(true);
        textDescription.setFocusable(false);
        textDescription.setBackground(this.getBackground());
        textDescription.setWrapStyleWord(true);
        textDescription.setFont(labelIcon.getFont());
        textDescription.setMargin(new Insets(0, 0, 0, 0));

        textType = new JTextArea(event.getJHVEventType().getEventType());
        textType.setBackground(this.getBackground());
        textType.setLineWrap(true);
        textType.setWrapStyleWord(true);
        textType.setFont(labelIcon.getFont());
        textType.setMargin(new Insets(0, 0, 0, 0));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        textStartTime = new JTextArea(sdf.format(event.getStartDate()));
        textStartTime.setBackground(this.getBackground());
        textStartTime.setLineWrap(true);
        textStartTime.setWrapStyleWord(true);
        textStartTime.setFont(labelIcon.getFont());
        textStartTime.setMargin(new Insets(0, 0, 0, 0));

        textEndTime = new JTextArea(sdf.format(event.getEndDate()));
        textEndTime.setBackground(this.getBackground());
        textEndTime.setLineWrap(true);
        textEndTime.setWrapStyleWord(true);
        textEndTime.setFont(labelIcon.getFont());
        textEndTime.setMargin(new Insets(0, 0, 0, 0));

    }

    /**
     * Add component to the summary panel.
     * 
     * @param panel
     *            the panel to add to
     * @param y
     *            the y position
     * @param fieldName
     *            the name of the field
     * @param component
     *            the component to add
     */
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

}
