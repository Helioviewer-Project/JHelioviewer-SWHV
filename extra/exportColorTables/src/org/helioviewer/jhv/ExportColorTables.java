package org.helioviewer.jhv;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.LUT;

/**
 * Simple program which will take all available color tables of JHV and export
 * them to use them with the website.
 * 
 * @author Helge Dietert
 */
public class ExportColorTables {
    /**
     * Writes out a png for every available color table in JHV
     * 
     * @param args
     */
    public static void main(String[] args) {
        Map<String, LUT> list = LUT.getStandardList();
        for (Map.Entry<String, LUT> e : list.entrySet()) {
            try {
                BufferedImage image = new BufferedImage(1, 256, BufferedImage.TYPE_INT_RGB);
                for (int i = 0; i < 256; ++i) {
                    image.setRGB(0, i, e.getValue().getLut8()[i]);
                }
                String filename = e.getKey().replace("/", "-") + ".png";
                ImageIO.write(image, "png", new File(filename));
                System.out.println(e.getKey() + ".png");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
