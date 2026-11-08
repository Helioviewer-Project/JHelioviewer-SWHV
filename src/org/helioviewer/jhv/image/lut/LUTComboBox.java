package org.helioviewer.jhv.image.lut;

import java.util.HashMap;

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
        Object selected = getSelectedItem();
        if (selected == null)
            return LUT.gray();
        String name = selected.toString();
        LUT lut = customLuts.get(name);
        if (lut != null)
            return lut;
        lut = LUT.get(name);
        return lut == null ? LUT.gray() : lut;
    }

    public void selectLUT(LUT lut) {
        String name;
        if (lut == null) // e.g. RGB
            name = LUT.gray().name();
        else {
            name = lut.name();
            if (LUT.get(name) == null && customLuts.put(name, lut) == null) {
                addItem(name);
            }
        }
        if (!name.equals(getSelectedItem()))
            setSelectedItem(name);
    }

}
