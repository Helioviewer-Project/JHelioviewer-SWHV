package org.helioviewer.jhv.plugins.eveplugin.radio.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import java.util.Collection;

import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.RadioDataManager;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.NoDataConfig;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.PlotConfig;

public class RadioImagePane implements ImageObserver, DrawableElement {

    private YAxis yAxis;
    private boolean intervalTooBig;

    public RadioImagePane() {
        intervalTooBig = false;
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        return false;
    }

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.RADIO;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxisG, Rectangle graphArea, Rectangle leftAxisArea, Point mousePosition) {
        Collection<NoDataConfig> noDataConfigs = RadioDataManager.getSingletonInstance().getNoDataConfigurations();
        Collection<PlotConfig> configs = RadioDataManager.getSingletonInstance().getPlotConfigurations();
        if (!intervalTooBig) {
            for (NoDataConfig ndc : noDataConfigs) {
                ndc.draw(g);
            }
            for (PlotConfig pc : configs) {
                pc.draw(g);
            }
        } else {
            String text1 = "The selected interval is too big.";
            String text2 = "Reduce the interval to see the radio spectrograms.";
            final int text1Width = (int) g.getFontMetrics().getStringBounds(text1, g).getWidth();
            final int text2Width = (int) g.getFontMetrics().getStringBounds(text2, g).getWidth();
            final int text1height = (int) g.getFontMetrics().getStringBounds(text2, g).getHeight();
            final int text2height = (int) g.getFontMetrics().getStringBounds(text2, g).getHeight();
            final int x1 = graphArea.x + (graphArea.width / 2) - (text1Width / 2);
            final int y1 = (int) (graphArea.y + (graphArea.height / 2) - 1.5 * text1height);
            final int x2 = graphArea.x + (graphArea.width / 2) - (text2Width / 2);
            final int y2 = (int) (graphArea.y + graphArea.height / 2 + 0.5 * text2height);
            g.setColor(Color.black);
            g.drawString(text1, x1, y1);
            g.drawString(text2, x2, y2);
        }
    }

    @Override
    public void setYAxis(YAxis _yAxis) {
        yAxis = _yAxis;

    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public boolean hasElementsToDraw() {
        return intervalTooBig || (RadioDataManager.getSingletonInstance().getPlotConfigurations() != null && !RadioDataManager.getSingletonInstance().getPlotConfigurations().isEmpty()) || !RadioDataManager.getSingletonInstance().getNoDataConfigurations().isEmpty();
    }

    public void setIntervalTooBig(boolean b) {
        intervalTooBig = b;
    }

    @Override
    public long getLastDateWithData() {
        return -1;
    }

}
