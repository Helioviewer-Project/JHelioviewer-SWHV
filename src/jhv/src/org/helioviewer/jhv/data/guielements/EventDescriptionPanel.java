package org.helioviewer.jhv.data.guielements;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.helioviewer.base.datetime.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;

/**
 * This is a panel describing an event in short.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
@SuppressWarnings({"serial"})
public class EventDescriptionPanel extends JPanel {

    /** The event */
    private final JHVEvent event;

    /** Label showing the event icon in the summary of the event */
    private JLabel labelIcon = new JLabel();

    /**
     * Label showing the event type in the summary of the event
     */
    private JTextArea textType = new JTextArea("N/A");

    /**
     * Label showing the event date in the summary of the event
     */
    private JTextArea textStartTime = new JTextArea("N/A");

    /**
     * Label showing the event coordinates in the summary of the event
     */
    private JTextArea textEndTime = new JTextArea("N/A");

    /**
     * Label showing the color of the selected event.
     */
    private final JLabel colorLabel = new JLabel();

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
        colorLabel.setBackground(event.getEventRelationShip().getRelationshipColor());
        colorLabel.setOpaque(true);

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

        this.addLineToSummaryPanel(summaryPanel, 0, "Start Time", textStartTime);
        this.addLineToSummaryPanel(summaryPanel, 1, "End Time", textEndTime);

        GridBagConstraints colorLabelConstraint = new GridBagConstraints();
        colorLabelConstraint.gridx = 2;
        colorLabelConstraint.gridy = 2;
        colorLabelConstraint.weightx = 0;
        colorLabelConstraint.weighty = 0;

        colorLabelConstraint.fill = GridBagConstraints.BOTH;
        colorLabelConstraint.anchor = GridBagConstraints.CENTER;
        colorLabelConstraint.ipadx = 10;
        colorLabelConstraint.ipady = 4;
        summaryPanel.add(colorLabel, colorLabelConstraint);

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
        textType = new JTextArea(event.getJHVEventType().getEventType());
        textType.setBackground(this.getBackground());
        textType.setLineWrap(true);
        textType.setWrapStyleWord(true);
        textType.setFont(labelIcon.getFont());
        textType.setMargin(new Insets(0, 0, 0, 0));

        textStartTime = new JTextArea(TimeUtils.utcDateFormat.format(event.getStartDate()));
        textStartTime.setBackground(this.getBackground());
        textStartTime.setLineWrap(true);
        textStartTime.setWrapStyleWord(true);
        textStartTime.setFont(labelIcon.getFont());
        textStartTime.setMargin(new Insets(0, 0, 0, 0));

        textEndTime = new JTextArea(TimeUtils.utcDateFormat.format(event.getEndDate()));
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
