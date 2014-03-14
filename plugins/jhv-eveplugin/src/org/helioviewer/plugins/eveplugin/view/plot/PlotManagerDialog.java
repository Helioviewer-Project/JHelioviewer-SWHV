package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.settings.BandType;
import org.helioviewer.plugins.eveplugin.settings.BandGroup;

import org.helioviewer.plugins.eveplugin.settings.BandTypeAPI;

public class PlotManagerDialog extends JDialog implements ActionListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;

    private final PlotsContainerPanel plotContainer;
    
    private final JPanel contentPane = new JPanel();
    private final PlotManagerPanel plot1Pane = new PlotManagerPanel(false, PlotsContainerPanel.PLOT_IDENTIFIER_MASTER, "Plot 1");
    private final PlotManagerPanel plot2Pane = new PlotManagerPanel(true, PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE, "Plot 2");
    private final JButton okButton = new JButton("Ok");
    private final JButton cancelButton = new JButton("Cancel");
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public PlotManagerDialog(final PlotsContainerPanel plotContainer) {
        this.plotContainer = plotContainer;
        
        initVisualComponents();
    }
    
    private void initVisualComponents() {
        setModal(true);
        setContentPane(contentPane);
        setTitle("Plot Manager");
        
        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
        
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.add(plot1Pane);
        contentPane.add(plot2Pane);
        contentPane.add(buttonPane);
        
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }
    
    public void showDialog() {
        plot1Pane.updateGroupBoxValue();
        plot2Pane.updateGroupBoxValue();
        
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        
        setVisible(true);
    }
    
    private void resizeDialog() {
        pack();
        setSize(getPreferredSize());
    }
    
    private void updateBandController() {
        final BandController bandController = BandController.getSingletonInstance();
        
        bandController.selectBandGroup(PlotsContainerPanel.PLOT_IDENTIFIER_MASTER, plot1Pane.getSelectedGroup());
        bandController.removeAllBands(PlotsContainerPanel.PLOT_IDENTIFIER_MASTER);
        
        for (final BandType value : plot1Pane.getSelectedValues()) {
            bandController.addBand(PlotsContainerPanel.PLOT_IDENTIFIER_MASTER, value);    
        }
        
        final BandGroup selectedGroup = plot2Pane.getSelectedGroup();
        
        if (selectedGroup == null) {
            bandController.selectBandGroup(PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE, null);
            plotContainer.setPlot2Visible(false);
        } else {
            bandController.selectBandGroup(PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE, selectedGroup);
            bandController.removeAllBands(PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE);
            
            for (final BandType value : plot2Pane.getSelectedValues()) {
                bandController.addBand(PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE, value);    
            }
            
            plotContainer.setPlot2Visible(true);
        }
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Action Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource().equals(okButton)) {
            updateBandController();
            setVisible(false);
        } else if (arg0.getSource().equals(cancelButton)) {
            setVisible(false);
        }
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Plot Manager Panel
    // //////////////////////////////////////////////////////////////////////////////
    
    private class PlotManagerPanel extends JPanel implements ActionListener, ListSelectionListener, MouseListener {
        
        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////
        
        private static final long serialVersionUID = 1L;
        
        private final boolean deselectable;
        private final String identifier;
        
        private final JComboBox groupComboBox = new JComboBox(new DefaultComboBoxModel());
        private final JList selectedList = new JList(new DefaultListModel());
        private final JList availableList = new JList(new DefaultListModel());
        private final JButton addButton = new JButton("<");
        private final JButton removeButton = new JButton(">");
        private final JPanel centerPane = new JPanel();
        
        // //////////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////////
        
        public PlotManagerPanel(final boolean deselectable, final String identifer, final String plotName) {
            this.deselectable = deselectable;
            this.identifier = identifer;
            
            initVisualComponents(plotName);
            initGroups();
        }
        
        private void initVisualComponents(final String plotName) {
            setResizable(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(plotName));
            
            final JPanel headerPane = new JPanel();
            headerPane.setLayout(new FlowLayout(FlowLayout.LEFT));
            headerPane.add(new JLabel("Group:"));
            headerPane.add(groupComboBox);
            
            
            centerPane.setLayout(new GridBagLayout());
            
            final GridBagConstraints c = new GridBagConstraints();
            
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(4, 4, 0, 2);
            centerPane.add(new JLabel("Selected Bands"), c);
            
            final JScrollPane selectedListPane = new JScrollPane(selectedList);
            selectedListPane.setPreferredSize(new Dimension(120, 60));
            
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(2, 4, 2, 2);
            centerPane.add(selectedListPane, c);
            
            final JPanel middlePane = new JPanel(new GridBagLayout());
            final JPanel controlButtonPane = new JPanel();
            controlButtonPane.setLayout(new BoxLayout(controlButtonPane, BoxLayout.Y_AXIS));
            middlePane.add(controlButtonPane);
            
            controlButtonPane.add(addButton);
            controlButtonPane.add(removeButton);
            
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(4, 0, 0, 0);
            centerPane.add(middlePane, c);
            
            c.gridx = 2;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(4, 2, 0, 4);
            centerPane.add(new JLabel("Available Bands"), c);
            
            final JScrollPane availableListPane = new JScrollPane(availableList);
            availableListPane.setPreferredSize(new Dimension(120, 60));
            
            c.gridx = 2;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(2, 2, 2, 4);
            centerPane.add(availableListPane, c);
            
            add(headerPane, BorderLayout.PAGE_START);
            add(centerPane, BorderLayout.CENTER);
            
            groupComboBox.addActionListener(this);
            addButton.addActionListener(this);
            addButton.setToolTipText("Add Band");
            removeButton.addActionListener(this);
            removeButton.setToolTipText("Remove Band");
            selectedList.addListSelectionListener(this);
            selectedList.addMouseListener(this);
            availableList.addListSelectionListener(this);
            availableList.addMouseListener(this);
        }
        
        private void initGroups() {
            final BandGroup[] groups = BandTypeAPI.getSingletonInstance().getGroups();
            
            DefaultComboBoxModel model = (DefaultComboBoxModel) groupComboBox.getModel();
            model.removeAllElements();
            
            if (deselectable) {
                model.addElement("None");
            }
            
            for (final BandGroup group : groups) {
                model.addElement(group);
            }
        }
        
        private void enableComponents() {
            final boolean enable = !deselectable || (groupComboBox.getSelectedIndex() > 0 && deselectable);
            
            centerPane.setVisible(enable);
            resizeDialog();
        }
        
        public void updateGroupBoxValue() {
            if (deselectable && !plotContainer.isPlot2Visible()) {
                groupComboBox.setSelectedIndex(0);
            } else {
                final BandController bandController = BandController.getSingletonInstance();
                groupComboBox.setSelectedItem(bandController.getSelectedGroup(identifier));
            }
        }
        
        private void updateValues() {
            final BandController bandController = BandController.getSingletonInstance();
            final DefaultListModel selectedModel = (DefaultListModel) selectedList.getModel();
            final DefaultListModel availableModel = (DefaultListModel) availableList.getModel();
            final BandGroup selectedGroup = (BandGroup) groupComboBox.getSelectedItem();
            
            selectedModel.removeAllElements();
            availableModel.removeAllElements();
            
            final BandType[] values = BandTypeAPI.getSingletonInstance().getBandTypes(selectedGroup);
            
            for (final BandType value : values) {
                if (bandController.getBand(identifier, value) == null) {
                    availableModel.addElement(value);
                } else {
                    selectedModel.addElement(value);
                }
            }
            
            selectedList.setSelectedIndex(0);
            availableList.setSelectedIndex(0);
        }
        
        private void updateButtonStates() {
            addButton.setEnabled(availableList.getSelectedIndex() >= 0);
            removeButton.setEnabled(selectedList.getSelectedIndex() >= 0); 
        }
        
        private void addValue() {
            if (availableList.getSelectedIndex() >= 0) {
                final Object selectedItem = ((DefaultListModel) availableList.getModel()).get(availableList.getSelectedIndex());
                ((DefaultListModel) selectedList.getModel()).addElement(selectedItem);
                ((DefaultListModel) availableList.getModel()).removeElement(selectedItem);
            }
        }
        
        private void removeValue() {
            if (selectedList.getSelectedIndex() < 0) {
                return;
            }
            
            final Object selectedItem = ((DefaultListModel) selectedList.getModel()).get(selectedList.getSelectedIndex());
            final DefaultListModel availableListModel = (DefaultListModel) availableList.getModel(); 
            availableListModel.addElement(selectedItem);
            ((DefaultListModel) selectedList.getModel()).removeElement(selectedItem);
            
            final Object[] values = availableListModel.toArray();
            final SortedMap<String, BandType> map = new TreeMap<String, BandType>(); 
            
            for (final Object obj : values) {
                final BandType item = (BandType) obj;
                map.put(item.getLabel(), item); 
            } 
            
            availableListModel.removeAllElements();
            
            for (final BandType value : map.values()) {
                availableListModel.addElement(value);
            }
        }
        
        public BandGroup getSelectedGroup() {
            if (deselectable && groupComboBox.getSelectedIndex() == 0) {
                return null;
            }
            
            return (BandGroup) groupComboBox.getSelectedItem();
        }
        
        public BandType[] getSelectedValues() {
            final DefaultListModel model = (DefaultListModel) selectedList.getModel();
            final BandType[] result = new BandType[model.size()]; 
            
            for (int i = 0; i < model.size(); ++i) {
                result[i] = (BandType) model.get(i);
            }
            
            return result;
        }
        
        // //////////////////////////////////////////////////////////////////////////////
        // Action Listener
        // //////////////////////////////////////////////////////////////////////////////

        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getSource().equals(groupComboBox)) {
                enableComponents();
                updateValues();
            } else if (arg0.getSource().equals(addButton)) {                
                addValue();
            } else if (arg0.getSource().equals(removeButton)) {
                removeValue();
            }
        }
        
        // //////////////////////////////////////////////////////////////////////////////
        // List Selection Listener
        // //////////////////////////////////////////////////////////////////////////////

        public void valueChanged(ListSelectionEvent e) {
            updateButtonStates();
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && e.getSource().equals(availableList)) {
                addValue();
            } else if (e.getClickCount() == 2 && e.getSource().equals(selectedList)) {
                removeValue();
            }
        }

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {}

        public void mouseReleased(MouseEvent e) {}
    }
}
