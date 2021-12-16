package org.helioviewer.jhv.base.lut;

import java.util.Map;
import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

@SuppressWarnings("serial")
public class LUTComboBox extends JComboBox<String> {

    private final Map<String, LUT> lutMap;

    public LUTComboBox() {
        lutMap = LUT.copyMap(); // duplicate
        setModel(new DefaultComboBoxModel<>(lutMap.keySet().toArray(String[]::new)));
        setToolTipText("Choose a color table");
        com.jidesoft.swing.SearchableUtils.installSearchable(this);
    }

    public LUTComboBox(String selected) {
        this();
        setSelectedItem(selected);
    }

    public LUT getLUT() {
        return lutMap.get(Objects.requireNonNull(getSelectedItem()).toString());
    }

    public String getColormap() {
        return Objects.requireNonNull(getSelectedItem()).toString();
    }

    public void setLUT(LUT lut) {
        String name;
        if (lut == null) // e.g. RGB
            name = "Gray";
        else {
            name = lut.name();
            if (lutMap.get(name) == null) {
                lutMap.put(name, lut);
                addItem(name);
            }
        }
        setSelectedItem(name);
    }

}
