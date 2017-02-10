package org.helioviewer.jhv.base.lut;

import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.jidesoft.swing.SearchableUtils;

@SuppressWarnings("serial")
public class LUTComboBox extends JComboBox<String> {

    private final Map<String, LUT> lutMap;

    public LUTComboBox() {
        lutMap = LUT.copyMap(); // duplicate
        Set<String> set = lutMap.keySet();
        setModel(new DefaultComboBoxModel<String>(set.toArray(new String[set.size()])));
        setToolTipText("Choose a color table");
        SearchableUtils.installSearchable(this);
    }

    public LUTComboBox(String selected) {
        this();
        setSelectedItem(selected);
    }

    public LUT getLUT() {
        return lutMap.get(getSelectedItem());
    }

    public void setLUT(LUT lut) {
        String name;
        if (lut == null) // e.g. RGB
            name = "Gray";
        else {
            name = lut.getName();
            if (lutMap.get(name) == null) {
                lutMap.put(name, lut);
                addItem(name);
            }
        }
        setSelectedItem(name);
    }

}
