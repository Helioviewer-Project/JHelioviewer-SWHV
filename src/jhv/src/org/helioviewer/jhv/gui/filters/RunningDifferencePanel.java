package org.helioviewer.jhv.gui.filters;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Panel to control running differences
 *
 * @author Helge Dietert
 *
 */
public class RunningDifferencePanel extends AbstractFilterPanel implements ChangeListener {
    /**
     * Controlled filter by this panel
     */
    private final JSpinner truncateSpinner;
    private final JLabel truncationLabel;

    private JCheckBox diffRot;
    private final static String[] combolist = { "No differences", "Running difference", "Base difference" };
    private Action downloadLayerAction;
    private Action showMetaAction;
    private final JButton downloadLayerButton = new JButton();
    private final JButton showMetaButton = new JButton();

    public RunningDifferencePanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        truncateSpinner = new JSpinner();
        truncateSpinner.setModel(new SpinnerNumberModel(new Float(0.8f), new Float(0), new Float(1), new Float(0.01f)));
        truncateSpinner.addChangeListener(this);
        truncationLabel = new JLabel("Contrast boost:");
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(truncateSpinner, "0%");
        truncateSpinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        WheelSupport.installMouseWheelSupport(truncateSpinner);
        addRadioButtons();
    }

    private void addRadioButtons() {
        final JComboBox comboBox = new JComboBox(combolist);
        comboBox.setSelectedItem("No differences");
        diffRot = new JCheckBox("Rotation correction");
        diffRot.setSelected(true);
        final JPanel radPanel = new JPanel(new FlowLayout());
        final JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 1;

        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridx = 0;
        topPanel.add(comboBox, c);
        c.gridx = 1;
        c.weightx = 0.1;
        topPanel.add(downloadLayerButton, c);
        c.gridx = 2;
        topPanel.add(showMetaButton, c);
        add(topPanel);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 3;
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboBox.getSelectedItem() == combolist[0]) {
                    topPanel.remove(radPanel);
                    jp2view.setDifferenceMode(false);
                    jp2view.setBaseDifferenceMode(false);
                } else if (comboBox.getSelectedItem() == combolist[1]) {
                    topPanel.add(radPanel, c);
                    jp2view.setDifferenceMode(true);
                    jp2view.setBaseDifferenceMode(false);
                    jp2view.setRunDiffNoRot(!diffRot.isSelected());
                } else if (comboBox.getSelectedItem() == combolist[2]) {
                    topPanel.add(radPanel, c);
                    jp2view.setDifferenceMode(true);
                    jp2view.setBaseDifferenceMode(true);
                    jp2view.setBaseDifferenceNoRot(!diffRot.isSelected());
                }
                Displayer.display();
                topPanel.revalidate();
                topPanel.repaint();
            }
        });
        diffRot.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (comboBox.getSelectedItem() == combolist[2]) {
                    jp2view.setBaseDifferenceNoRot(!diffRot.isSelected());
                } else {
                    jp2view.setRunDiffNoRot(!diffRot.isSelected());
                }
                Displayer.display();
            }
        });

        radPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 0);
        gc.weightx = 1;
        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        gc.gridx = 0;
        radPanel.add(diffRot, gc);
        gc.gridx = 1;
        radPanel.add(truncateSpinner, gc);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        float value = ((SpinnerNumberModel) truncateSpinner.getModel()).getNumber().floatValue();
        jp2view.setTruncation(1 - value);
        Displayer.display();

    }

    /**
     * Overridden setEnabled to keep in sync with child elements
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void setJP2View(AbstractView jp2view) {
        super.setJP2View(jp2view);
        truncateSpinner.setValue(1.f - jp2view.getTruncation());

        downloadLayerAction = new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Download the selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.DOWNLOAD));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                Displayer.getLayersModel().downloadLayer((JHVJP2View) jp2view);
            }
        };
        downloadLayerButton.setAction(downloadLayerAction);
        showMetaAction = new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Show metadata of the selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                Displayer.getLayersModel().showMetaInfo(jp2view);
            }
        };
        showMetaButton.setAction(showMetaAction);
        showMetaButton.revalidate();
        if (jp2view instanceof JHVJP2View) {
            this.downloadLayerButton.setEnabled(true);
        } else {
            this.downloadLayerButton.setEnabled(false);
        }

    }

}
