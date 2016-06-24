package org.helioviewer.jhv.data.guielements;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

/**
 * This is a panel describing an event in short.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
@SuppressWarnings("serial")
public class EventDescriptionPanel extends JPanel implements MouseListener {

    private final JHVRelatedEvents revent;

    public EventDescriptionPanel(JHVRelatedEvents revent, JHVEvent event) {
        this.revent = revent;

        JLabel labelIcon = new JLabel(revent.getIcon());

        JTextArea textType = new JTextArea(event.getName());
        textType.setBackground(this.getBackground());
        textType.setFont(labelIcon.getFont());
        textType.setMargin(new Insets(0, 0, 0, 0));

        JTextArea textStartTime = new JTextArea(TimeUtils.utcDateFormat.format(event.start));
        textStartTime.setBackground(getBackground());
        textStartTime.setFont(labelIcon.getFont());
        textStartTime.setMargin(new Insets(0, 0, 0, 0));

        JTextArea textEndTime = new JTextArea(TimeUtils.utcDateFormat.format(event.end));
        textEndTime.setBackground(getBackground());
        textEndTime.setFont(labelIcon.getFont());
        textEndTime.setMargin(new Insets(0, 0, 0, 0));

        addMouseListener(this);
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

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        JHVEventCache.highlight(revent);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JHVEventCache.highlight(null);
    }

}
