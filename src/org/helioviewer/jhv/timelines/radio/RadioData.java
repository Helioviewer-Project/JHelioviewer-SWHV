package org.helioviewer.jhv.timelines.radio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.base.lut.LUTComboBox;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.AbstractTimelineLayer;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONObject;

public final class RadioData extends AbstractTimelineLayer {

    static final int MAX_AMOUNT_OF_DAYS = 3;

    private final LUTComboBox lutCombo;
    private final JPanel optionsPanel;
    private final RadioState state = RadioState.INSTANCE;
    private final RadioCache cache = new RadioCache(this, state);

    public RadioData(JSONObject jo) {
        String cm = "Spectral";
        if (jo != null) {
            cm = jo.optString("colormap", cm);
            if (LUT.get(cm) == null)
                cm = "Spectral";
        }

        state.setLUT(LUT.get(cm));

        lutCombo = new LUTComboBox();
        lutCombo.setSelectedItem(cm);
        lutCombo.addActionListener(e -> setLUT(lutCombo.getLUT()));
        optionsPanel = optionsPanel(lutCombo);

        setEnabled(false);
    }

    public static AbstractTimelineLayer deserialize(JSONObject jo) { // has to be implemented for state
        return new RadioData(jo);
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("colormap", lutCombo.getColormap());
    }

    private void setLUT(LUT lut) {
        state.setLUT(lut);
        cache.changeColormap();
        DrawController.drawRequest();
    }

    @Override
    public YAxis getYAxis() {
        return state.yAxis();
    }

    @Override
    public boolean showYAxis() {
        return enabled;
    }

    @Override
    public void remove() {
        cache.abolish();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        if (!enabled)
            cache.invalidateAll();
    }

    @Override
    public String getName() {
        return "Callisto Radiogram";
    }

    @Override
    public Color getDataColor() {
        return UIGlobals.foreColor;
    }

    @Override
    public boolean isDownloading() {
        return cache.isDownloading();
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public boolean hasData() {
        return cache.hasData();
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void fetchData(TimeAxis selectedAxis) {
        if (enabled && selectedAxis.end() - selectedAxis.start() <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS) {
            cache.requestVisible(selectedAxis);
        }
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, Point mousePosition) {
        if (!enabled)
            return;

        if (xAxis.end() - xAxis.start() <= TimeUtils.DAY_IN_MILLIS * MAX_AMOUNT_OF_DAYS) {
            drawString(g, graphArea, xAxis, "No data available");
            cache.forEachData(data -> data.draw(g, graphArea, xAxis));
        } else {
            drawString(g, graphArea, xAxis, "Reduce the time interval to see the radio spectrograms.");
        }
    }

    @Override
    public void zoomToFitAxis() {
        state.yAxis().reset(400, 20);
    }

    @Override
    public void resetAxis() {
        state.yAxis().reset(400, 20);
    }

    static void drawString(Graphics2D g, Rectangle ga, TimeAxis xAxis, String text) {
        int dx0 = xAxis.value2pixel(ga.x, ga.width, xAxis.start());
        int dx1 = xAxis.value2pixel(ga.x, ga.width, xAxis.end());
        int dwidth = dx1 - dx0;
        g.setColor(Color.GRAY);
        g.fillRect(dx0, ga.y, dwidth, ga.height);
        g.setColor(Color.WHITE);

        Rectangle2D r = g.getFontMetrics().getStringBounds(text, g);
        int tWidth = (int) r.getWidth();
        int tHeight = (int) r.getHeight();
        int y = ga.y + ga.height / 2 - tHeight / 2;

        for (int x = dx0 + tWidth / 2; x < dx1; x += tWidth + tWidth / 2)
            g.drawString(text, x, y);
    }

    private static JPanel optionsPanel(LUTComboBox combo) {
        JButton availabilityBtn = new JButton("Available data");
        availabilityBtn.addActionListener(e -> JHVGlobals.openURL(DataSources.getServerSetting("ROB", "availability.images") +
                "ID=" + APIRequest.CallistoID));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        panel.add(combo, c);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(availabilityBtn, c);

        return panel;
    }

}
