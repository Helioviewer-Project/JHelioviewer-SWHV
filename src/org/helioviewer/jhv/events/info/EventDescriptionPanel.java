package org.helioviewer.jhv.events.info;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
class EventDescriptionPanel extends JPanel {

    private final JHVRelatedEvents revent;

    EventDescriptionPanel(JHVRelatedEvents _revent, JHVEvent event) {
        revent = _revent;

        JLabel labelIcon = new JLabel(revent.getIcon());

        JTextArea textStartTime = new JTextArea(TimeUtils.formatShort(event.start));
        textStartTime.setOpaque(false);

        JTextArea textEndTime = new JTextArea(TimeUtils.formatShort(event.end));
        textEndTime.setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JHVEventCache.highlight(revent);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JHVEventCache.highlight(null);
            }
        });
        setLayout(new GridBagLayout());

        JLabel colorLabel = new JLabel();
        colorLabel.setBackground(revent.getColor());
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
        add(labelIcon, iconLabelConstraint);

        // setup the summary panel
        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 20));

        addLineToSummaryPanel(summaryPanel, 0, "Start Time", textStartTime);
        addLineToSummaryPanel(summaryPanel, 1, "End Time", textEndTime);
        // this.addLineToSummaryPanel(summaryPanel, 2, "object_id", new
        // JTextArea(event.toString()));
        // this.addLineToSummaryPanel(summaryPanel, 4, "event_id", new
        // JTextArea(event.getUniqueID()));

        GridBagConstraints colorLabelConstraint = new GridBagConstraints();
        colorLabelConstraint.gridx = 2;
        colorLabelConstraint.gridy = 3;
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
        add(summaryPanel, shortPanelConstraint);
    }

    /**
     * Add component to the summary panel.
     *
     * @param panel     the panel to add to
     * @param y         the y position
     * @param fieldName the name of the field
     * @param component the component to add
     */
    private static void addLineToSummaryPanel(JPanel panel, int y, String fieldName, Component component) {
        GridBagConstraints shortPanelLabelConstraint = new GridBagConstraints();
        shortPanelLabelConstraint.weightx = 0;
        shortPanelLabelConstraint.weighty = 0;
        shortPanelLabelConstraint.anchor = GridBagConstraints.FIRST_LINE_START;
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
