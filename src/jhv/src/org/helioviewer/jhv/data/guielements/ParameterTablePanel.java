package org.helioviewer.jhv.data.guielements;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.guielements.model.EmptyRowFilter;
import org.helioviewer.jhv.data.guielements.model.ParameterTableModel;

/**
 * Represents a panel with a table containing all the parameters from the given
 * list.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ParameterTablePanel extends JPanel {

    /** the table */
    private final JTable table;
    /** the model for the table */
    private final ParameterTableModel parameterModel;
    /** checkbox indicating null value should be visible */
    private final JCheckBox nullValue;

    private final TableRowSorter<ParameterTableModel> sorter;

    /**
     * Creates a table panel for the given parameters.
     * 
     * @param parameters
     *            the parameters
     */
    public ParameterTablePanel(List<JHVEventParameter> parameters) {
        super();
        setLayout(new BorderLayout());
        parameterModel = new ParameterTableModel(parameters);
        table = new JTable(parameterModel) {
            /*
             * @Override public boolean getScrollableTracksViewportWidth() {
             * return getPreferredSize().width < getParent().getWidth(); }
             */
        };
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.setPreferredScrollableViewportSize(new Dimension(table.getWidth(), 150));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sorter = new TableRowSorter<ParameterTableModel>(parameterModel);
        table.setRowSorter(sorter);
        nullValue = new JCheckBox();
        JPanel nullValuePanel = new JPanel();
        JLabel nullValueLabel = new JLabel("Show Empty Parameters");
        nullValuePanel.add(nullValue);
        nullValuePanel.add(nullValueLabel);
        RowFilter<ParameterTableModel, Integer> rf = null;
        // If current expression doesn't parse, don't update.
        try {
            rf = new EmptyRowFilter();
        } catch (java.util.regex.PatternSyntaxException ex) {
            return;
        }
        sorter.setRowFilter(rf);

        nullValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nullValue.isSelected()) {
                    sorter.setRowFilter(null);
                } else {
                    RowFilter<ParameterTableModel, Integer> rf = null;
                    // If current expression doesn't parse, don't update.
                    try {
                        rf = new EmptyRowFilter();
                    } catch (java.util.regex.PatternSyntaxException ex) {
                        return;
                    }
                    sorter.setRowFilter(rf);
                }

            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(nullValuePanel, BorderLayout.PAGE_END);
    }

}
