package org.helioviewer.jhv.layers.fov;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

public final class FOVOptions implements Screen {

    private static final int CENTER = 0;
    private static final int INSTRUMENT = 1000;
    private final FOVCatalog catalog;
    private final Panel buffer = new Panel();

    public FOVOptions(FOVCatalog _catalog) {
        catalog = _catalog;
    }

    @Override
    public Panel build() {
        build(buffer.reset(), catalog);
        return buffer;
    }

    public static void build(Panel p, FOVCatalog catalog) {
        p.treeView(0, 0).cellEditor();
        int row = 0;
        for (FOVPlatform platform : catalog.platforms()) {
            p.section(platform.toString(), false).identity(INSTRUMENT + row).headerRow().stretchColumns(0, 1, 2).row();
            pointing(p, CENTER + 2 * row, "    δx ", platform.getCenterX());
            pointing(p, CENTER + 2 * row + 1, "    δy ", platform.getCenterY());
            p.rowEnd();
            row++;
            // The trailing label prevents checkbox ellipsis on Windows.
            for (FOVInstrument instrument : platform.instruments())
                p.row().check(INSTRUMENT + row++, instrument.toString(), instrument.isEnabled()).flat().label("      ").rowEnd();
            p.sectionEnd();
        }
        p.treeViewEnd();
    }

    private static void pointing(Panel p, int id, String label, double value) {
        p.row().fill().label(label).tight().fill().numberSpinner(id, value, FOVPlatform.MIN_CENTER_ARCMIN, FOVPlatform.MAX_CENTER_ARCMIN, 0.1)
                .format(2, 1, "′").rowEnd();
    }

    @Override
    public void number(int owner, int item, double value) {
        number(catalog, item, value);
    }

    public static void number(FOVCatalog catalog, int item, double value) {
        int row = 0;
        for (FOVPlatform platform : catalog.platforms()) {
            double offset = Math.clamp(value, FOVPlatform.MIN_CENTER_ARCMIN, FOVPlatform.MAX_CENTER_ARCMIN);
            if (item == CENTER + 2 * row) {
                platform.setCenterX(offset);
                return;
            }
            if (item == CENTER + 2 * row + 1) {
                platform.setCenterY(offset);
                return;
            }
            row += 1 + platform.instruments().size();
        }
        throw new IllegalArgumentException("FOV number " + item);
    }

    @Override
    public void checked(int owner, int item, boolean value) {
        checked(catalog, item, value);
    }

    public static void checked(FOVCatalog catalog, int item, boolean value) {
        int row = 0;
        for (FOVPlatform platform : catalog.platforms()) {
            row++;
            for (FOVInstrument instrument : platform.instruments()) {
                if (item == INSTRUMENT + row++) {
                    instrument.setEnabled(value);
                    DisplayController.display();
                    return;
                }
            }
        }
        throw new IllegalArgumentException("FOV checkbox " + item);
    }
}
