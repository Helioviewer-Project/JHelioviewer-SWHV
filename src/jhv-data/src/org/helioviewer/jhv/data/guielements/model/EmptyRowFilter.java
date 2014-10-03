package org.helioviewer.jhv.data.guielements.model;

import javax.swing.RowFilter;

/**
 * Filter avoiding empty values.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EmptyRowFilter extends RowFilter<ParameterTableModel, Integer> {

    @Override
    public boolean include(javax.swing.RowFilter.Entry<? extends ParameterTableModel, ? extends Integer> entry) {
        ParameterTableModel model = entry.getModel();
        String value = (String) model.getValueAt(entry.getIdentifier(), 1);
        return value != null && !value.equals("");
    }

}
