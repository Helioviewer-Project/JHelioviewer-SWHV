/**
 * 
 */
package ch.fhnw.jhv.plugins.vectors.control;

import java.awt.Dimension;
import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;

/**
 * Information Plugin
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class InformationControlPlugin extends AbstractPlugin implements ControlPlugin {

    /**
     * Title Lables
     */
    private JLabel lblMin = new JLabel("<html><body><b>Min length:</b></body></html> ");
    private JLabel lblMax = new JLabel("<html><body><b>Max length: </b></body></html>");
    private JLabel lblAmountOfVectors = new JLabel("<html><body><b>Total vectors:</b></body></html> ");
    private JLabel lblAmountOfDisplayedVectors = new JLabel("<html><body><b>Rendered vectors:</b></body></html> ");

    /**
     * Value labels
     */
    private JLabel lblMinValue = new JLabel();
    private JLabel lblMaxValue = new JLabel();
    private JLabel lblAmountOfVectorsValue = new JLabel();
    private JLabel lblAmountOfDisplayedVectorsValue = new JLabel();

    /**
     * Return the title of the ControlPlugin
     */
    public String getTitle() {
        return "Vector Field Information";
    }

    /**
     * Return the JComponent
     * 
     * @return JComponent contains all the gui stuff of the ControlPlugin
     */
    public JComponent getComponent() {
        JPanel panel = new JPanel(new SpringLayout());
        panel.setPreferredSize(new Dimension(300, 100));

        lblMin.setToolTipText("Value of the shortest vector strength.");
        lblMinValue.setToolTipText("Value of the shortest vector strength.");

        panel.add(lblMin);
        panel.add(lblMinValue);

        lblMax.setToolTipText("Value of the longest vector strength.");
        lblMaxValue.setToolTipText("Value of the longest vector strength.");

        panel.add(lblMax);
        panel.add(lblMaxValue);

        lblAmountOfVectors.setToolTipText("Total amount of vectors.");
        lblAmountOfVectorsValue.setToolTipText("Total amount of vectors.");

        panel.add(lblAmountOfVectors);
        panel.add(lblAmountOfVectorsValue);

        lblAmountOfDisplayedVectors.setToolTipText("Total amount of the rendered vectors.");
        lblAmountOfDisplayedVectorsValue.setToolTipText("Total amount of the rendered vectors.");

        panel.add(lblAmountOfDisplayedVectors);
        panel.add(lblAmountOfDisplayedVectorsValue);

        SpringUtilities.makeGrid(panel, 4, 2, 0, 0, 0, 0);

        return panel;
    }

    /**
     * Receive an Information update from the VectorFieldManager
     * 
     * @param minValue
     *            min value
     * @param maxValue
     *            max value
     * @param amountOfVectors
     *            amount of vectors
     * @param amountOfDisplayedVectors
     *            amount of displayed vectors
     */
    public void receiveInformations(float minValue, float maxValue, int amountOfVectors, int amountOfDisplayedVectors) {

        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(3);

        lblMinValue.setText(format.format(minValue) + "");
        lblMaxValue.setText(format.format(maxValue) + "");
        lblAmountOfVectorsValue.setText(amountOfVectors + "");
        lblAmountOfDisplayedVectorsValue.setText(amountOfDisplayedVectors + "");
    }

    /**
     * Return the ControlPluginType
     * 
     * @return ControlPluginType
     */
    public ControlPluginType getType() {
        return ControlPluginType.INFORMATION;
    }

    /**
     * Should the ControlPlugin be expanded on start
     * 
     * @return boolean should it be expanded
     */
    public boolean shouldStartExpanded() {
        return false;
    }
}
