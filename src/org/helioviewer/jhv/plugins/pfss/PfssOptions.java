package org.helioviewer.jhv.plugins.pfss;

import java.util.function.Consumer;

import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

public final class PfssOptions implements Screen {

    private static final int DETAIL = 0;
    private static final int RADIUS = 1;
    private static final int FIXED_COLORS = 2;
    private static final int AVAILABLE = 3;
    private final PfssLayer layer;
    private final Consumer<String> openURL;
    private final Panel buffer = new Panel();

    public PfssOptions(PfssLayer _layer, Consumer<String> _openURL) {
        layer = _layer;
        openURL = _openURL;
    }

    @Override
    public Panel build() {
        build(buffer.reset(), layer);
        return buffer;
    }

    public static void build(Panel p, PfssLayer layer) {
        p.grid(3).equalCells(false).swingMargin(2, 2, 2, 2)
                .row().label("Detail ").tight().numberSpinner(DETAIL, layer.getDetail(), 0, PfssSettings.MAX_DETAIL, 1).swingDefaultNumberEditor().rowEnd()
                .row().label("Radius ").tight().numberSpinner(RADIUS, layer.getRadius(), 1.099999999999999, PfssSettings.MAX_RADIUS, 0.1).format(1, 1, "").swingDefaultNumberEditor().rowEnd()
                .check(FIXED_COLORS, "Fixed colors", layer.getFixedColor()).right().endRow()
                .button(AVAILABLE, "Available data").tip("Click here to check the PFSS data availability")
                .gridEnd();
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        command(item, command, openURL);
    }

    public static void command(int item, Panel.Command command, Consumer<String> openURL) {
        if (item != AVAILABLE || command != Panel.Command.CLICK)
            throw new IllegalArgumentException("PFSS command " + item);
        openURL.accept(PfssSettings.AVAILABILITY_URL);
    }

    @Override
    public void number(int owner, int item, double value) {
        number(layer, item, value);
    }

    public static void number(PfssLayer layer, int item, double value) {
        switch (item) {
            case DETAIL -> layer.setDetail((int) Math.round(value));
            case RADIUS -> layer.setRadius(value);
            default -> throw new IllegalArgumentException("PFSS number " + item);
        }
    }

    @Override
    public void checked(int owner, int item, boolean value) {
        checked(layer, item, value);
    }

    public static void checked(PfssLayer layer, int item, boolean value) {
        if (item != FIXED_COLORS)
            throw new IllegalArgumentException("PFSS checkbox " + item);
        layer.setFixedColor(value);
    }
}
