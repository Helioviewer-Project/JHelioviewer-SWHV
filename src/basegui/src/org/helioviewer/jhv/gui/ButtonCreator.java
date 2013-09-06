package org.helioviewer.jhv.gui;

import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

/**
 * Some convenience methods for creating buttons
 * 
 * @author mnuhn
 */
public class ButtonCreator {

    /**
     * Creates a button for this panel,
     * 
     * Sets all common properties.
     * 
     * @param icon
     *            Icon to use
     * @param tooltip
     *            Tooltip to show
     * @return Button with all desired properties
     */
    public static JButton createButton(Icon icon, String tooltip, ActionListener actionListener) {
        JButton newButton = new JButton(icon);
        // newButton.setBorder(BorderFactory.createEtchedBorder());
        // newButton.setPreferredSize(new
        // Dimension(newButton.getMinimumSize().width,
        // newButton.getMinimumSize().height));
        newButton.setToolTipText(tooltip);
        newButton.addActionListener(actionListener);
        return newButton;
    }

    /**
     * Creates a button for this panel,
     * 
     * Sets all common properties.
     * 
     * @param icon
     *            Icon to use
     * @param text
     *            Text to show with the button
     * @param tooltip
     *            Tooltip to show
     * @return Button with all desired properties
     */
    public static JButton createTextButton(Icon icon, String text, String tooltip, ActionListener actionListener) {
        JButton newButton = new JButton(text, icon);
        // newButton.setBorder(BorderFactory.createEtchedBorder());
        // newButton.setPreferredSize(new
        // Dimension(newButton.getMinimumSize().width,
        // newButton.getMinimumSize().height));
        newButton.setToolTipText(tooltip);
        newButton.addActionListener(actionListener);
        return newButton;
    }

}
