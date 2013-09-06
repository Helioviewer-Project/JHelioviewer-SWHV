package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager.Area;

/**
 * Panel containing a combobox for choosing the color table and button to add
 * further tables adapted
 * 
 * @author Helge Dietert (extended)
 */
public class SOHOLUTPanel extends FilterPanel implements ActionListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;

    private static final Icon invertIcon = IconBank.getIcon(JHVIcon.INVERT);

    private SOHOLUTFilter filter;
    private Map<String, LUT> lutMap;
    private int lastSelectedIndex;

    /**
     * Shown combobox to choose
     */
    private JComboBox combobox;
    /**
     * Shown invert button
     */
    private JToggleButton invertButton = new JToggleButton(invertIcon);
    /**
     * Shown label
     */
    private JLabel title;

    /**
     * Creates a filter panel with the standard list of filters
     */
    public SOHOLUTPanel() {
        lutMap = LUT.getStandardList();

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Color:");
        title.setPreferredSize(new Dimension(FilterPanel.titleWidth, FilterPanel.height));
        add(title);

        // Add add entry
        lutMap.put("<Load new GIMP gradient file>", null);
        combobox = new JComboBox(lutMap.keySet().toArray());
        combobox.setToolTipText("Choose a color table");
        combobox.setPreferredSize(new Dimension(150, combobox.getPreferredSize().height));
        combobox.addActionListener(this);
        lastSelectedIndex = 0;
        add(combobox);

        add(Box.createHorizontalStrut(14));

        invertButton.setToolTipText("Invert color table");
        invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        invertButton.setPreferredSize(new Dimension(FilterPanel.valueWidth - 14, FilterPanel.height));
        invertButton.addActionListener(this);
        add(invertButton);

        setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */

    public void setFilter(Filter filter) {
        if (filter instanceof SOHOLUTFilter) {
            this.filter = (SOHOLUTFilter) filter;
            this.filter.setPanel(this);
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */

    public Area getArea() {
        return Area.BOTTOM;
    }

    /**
     * Sets the color table
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == invertButton) {
            if (invertButton.isSelected()) {
                invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            } else {
                invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            }
        }
        // NULL map means a new gradient
        LUT newMap = lutMap.get(combobox.getSelectedItem());
        if (newMap == null) {
            // Add new color table
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new GGRFilter());
            fc.setMultiSelectionEnabled(false);
            int state = fc.showOpenDialog(null);
            if (state == JFileChooser.APPROVE_OPTION) {
                try {
                    Log.info("Load gradient file " + fc.getSelectedFile());
                    addLut(LUT.readGimpGradientFile(fc.getSelectedFile()));
                    lastSelectedIndex = combobox.getSelectedIndex();
                } catch (IOException ex) {
                    Message.warn("Error loading gradient file", "Error loading gradient file: " + fc.getSelectedFile() + "\n\n" + ex.getMessage());
                    Log.warn("Error loading gradient file: " + fc.getSelectedFile() + " - " + ex.getMessage());
                } catch (GradientError ex) {
                    Message.warn("Error applying gradient file", "Error loading gradient file: " + fc.getSelectedFile() + "\n\n" + ex.getMessage());
                    Log.warn("Error applying gradient file: " + fc.getSelectedFile() + " - " + ex.getMessage());
                }
            } else {
                combobox.setSelectedIndex(lastSelectedIndex);
            }
        } else {
            filter.setLUT(newMap, invertButton.isSelected());
            lastSelectedIndex = combobox.getSelectedIndex();
        }
    }

    /**
     * Set the filter to the filter with the given name if the filter exists for
     * this panel
     * 
     * @param name
     *            Name of the filter
     */
    public void setLutByName(String name) {
        combobox.setSelectedItem(name);
    }

    /**
     * Adds a color table to the available list and set it active
     * 
     * @param lut
     *            Color table to add
     */
    public void addLut(LUT lut) {
        if (lutMap.put(lut.getName(), lut) == null)
            combobox.addItem(lut.getName());
        combobox.setSelectedItem(lut.getName());
        filter.setLUT(lut, invertButton.isSelected());
    }

    public int getDetails() {
        return FilterAlignmentDetails.POSITION_COLORTABLES;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        title.setEnabled(enabled);
        combobox.setEnabled(enabled);
        invertButton.setEnabled(enabled);
    }

    /**
     * Sets the sharpen value.
     * 
     * This may be useful, if the opacity is changed from another source than
     * the slider itself.
     * 
     * @param lut
     *            New look up table
     * @param invertLUT
     *            true if the look up table shall be inverted, false otherwise.
     */
    void setValue(LUT lut, boolean invertLUT) {
        invertButton.setSelected(invertLUT);
        combobox.setSelectedItem(lut.getName());

        if (invertLUT) {
            invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        } else {
            invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }
    }
}
