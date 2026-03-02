package org.helioviewer.jhv.base.lut;

import java.util.HashMap;
import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

@SuppressWarnings("serial")
public final class LUTComboBox extends JComboBox<String> {

    private final HashMap<String, LUT> customLuts = new HashMap<>();

    public LUTComboBox() {
        setModel(new DefaultComboBoxModel<>(LUT.names()));
        setToolTipText("Choose a color table");
        com.jidesoft.swing.SearchableUtils.installSearchable(this);
    }

    public LUT getLUT() {
        String name = Objects.requireNonNull(getSelectedItem()).toString();
        LUT lut = customLuts.get(name);
        return lut == null ? LUT.get(name) : lut;
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
            if (LUT.get(name) == null && customLuts.putIfAbsent(name, lut) == null) {
                addItem(name);
            }
        }
        setSelectedItem(name);
    }

}
