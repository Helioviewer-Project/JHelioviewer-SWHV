package org.helioviewer.jhv.data.guielements;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.jhv.gui.components.CollapsiblePane;

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
 * @author Bram.Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKEventInformationDialog extends JDialog implements WindowFocusListener, FocusListener {

    private static final long serialVersionUID = 1L;

    private JPanel allTablePanel;

    private CollapsiblePane standardParameters;

    private CollapsiblePane advancedParameters;

    private CollapsiblePane allParameters;

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
     * Label showing the event icon in the summary of the event
     */
    private JLabel labelIcon = new JLabel("");

    private final JHVEvent event;

    /**
     * 
     * 
     * @param event
     */
    public SWEKEventInformationDialog(JHVEvent event) {
        super();// ImageViewerGui.getMainFrame());
        this.event = event;

        labelIcon = new JLabel(event.getIcon());

        initDescriptionText();

        initAllTablePanel();
        initParameterCollapsiblePanels();

        this.setLayout(new GridBagLayout());
        this.setUndecorated(true);
        this.setMinimumSize(new Dimension(550, 50));

        this.addWindowFocusListener(this);
        this.addFocusListener(this);

        this.setAlwaysOnTop(true);

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

        // setup the summary panel
        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 20));
        GridBagConstraints shortPanelLabelConstraint = new GridBagConstraints();
        shortPanelLabelConstraint.weightx = 0;
        shortPanelLabelConstraint.weighty = 0;
        shortPanelLabelConstraint.anchor = GridBagConstraints.WEST;

        this.addLineToSummaryPanel(summaryPanel, 0, "Type", textType);
        this.addLineToSummaryPanel(summaryPanel, 1, "Description", textDescription);
        this.addLineToSummaryPanel(summaryPanel, 2, "Start Time", textStartTime);
        this.addLineToSummaryPanel(summaryPanel, 3, "End Time", textEndTime);

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

        GridBagConstraints allTablePanelConstraint = new GridBagConstraints();
        allTablePanelConstraint.gridx = 0;
        allTablePanelConstraint.gridy = 3;
        allTablePanelConstraint.gridwidth = 4;
        allTablePanelConstraint.weightx = 0;
        allTablePanelConstraint.weighty = 1;
        allTablePanelConstraint.fill = GridBagConstraints.BOTH;
        this.add(new JScrollPane(allTablePanel), allTablePanelConstraint);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        this.setVisible(false);
        this.dispose();

    }

    @Override
    public void focusGained(FocusEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void focusLost(FocusEvent arg0) {
        this.setVisible(false);
        this.dispose();

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
     * initialize the allTablePanel
     * 
     */
    private void initAllTablePanel() {
        allTablePanel = new JPanel();
        allTablePanel.setLayout(new BoxLayout(allTablePanel, BoxLayout.Y_AXIS));
    }

    /**
     * initialize collapsible panels
     */
    private void initParameterCollapsiblePanels() {
        ParameterTablePanel standardParameterPanel = new ParameterTablePanel(event.getVisibleEventParameters());
        standardParameters = new CollapsiblePane("Standard Parameters", standardParameterPanel, true);
        allTablePanel.add(standardParameters);

        ParameterTablePanel advancedParameterPanel = new ParameterTablePanel(event.getNonVisibleEventParameters());
        advancedParameters = new CollapsiblePane("Advanced Parameters", advancedParameterPanel, false);
        allTablePanel.add(advancedParameters);

        ParameterTablePanel allEventsPanel = new ParameterTablePanel(event.getAllEventParameters());
        allParameters = new CollapsiblePane("All Parameters", allEventsPanel, false);
        allTablePanel.add(allParameters);
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
