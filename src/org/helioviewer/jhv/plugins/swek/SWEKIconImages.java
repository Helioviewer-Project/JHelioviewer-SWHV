package org.helioviewer.jhv.plugins.swek;

import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.timelines.draw.DrawController;

import com.formdev.flatlaf.util.UIScale;

// UI thread only
public final class SWEKIconImages {

    // User-space sizes; the event tree and description scale TEXT and DESCRIPTION with the font
    static final int MARKER = 8;
    static final int MARKER_HIGHLIGHTED = 12;
    public static final int TEXT = 16;
    public static final int DESCRIPTION = 32;

    // By icon name: the full icon, and the images indexed by device size
    private record Icons(HashMap<String, SWEKIconBank.Pixels> full, HashMap<String, BufferedImage[]> images) {}

    private static final AffineTransform IDENTITY = new AffineTransform();
    private static final HashSet<Double> scales = new HashSet<>();
    private static List<String> names = List.of();
    private static int job;
    private static Icons icons = new Icons(new HashMap<>(), new HashMap<>());

    static void load(List<SWEKGroup> groups) {
        LinkedHashSet<String> iconNames = new LinkedHashSet<>();
        for (SWEKGroup group : groups)
            iconNames.add(group.getIconKey());
        names = List.copyOf(iconNames);
        if (!GraphicsEnvironment.isHeadless()) {
            for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                scales.add(device.getDefaultConfiguration().getDefaultTransform().getScaleX());
        }
        make();
    }

    @Nullable
    static SWEKIconBank.Pixels full(String iconName) {
        return icons.full.get(iconName);
    }

    // Draws the icon unscaled on the device pixels of the user-space square at x, y; transform is the current one of g
    public static void draw(Graphics2D g, AffineTransform transform, String iconName, int x, int y, int size) {
        double scale = transform.getScaleX();
        int deviceSize = deviceSize(size, scale);
        BufferedImage[] images = icons.images.get(iconName);
        BufferedImage image = images != null && deviceSize < images.length ? images[deviceSize] : null;
        if (image == null) {
            if (scales.add(scale)) // screen added later
                make();
            return;
        }
        g.setTransform(IDENTITY);
        g.drawImage(image, snap(transform.getTranslateX() + x * scale), snap(transform.getTranslateY() + y * transform.getScaleY()), null);
        g.setTransform(transform);
    }

    private static int deviceSize(int size, double scale) {
        return Math.max(1, (int) Math.round(size * scale));
    }

    // First device pixel whose center lies at or past v
    private static int snap(double v) {
        return (int) Math.ceil(v - 0.5);
    }

    // Decodes each icon once in the background, at full size and at every size on every scale
    private static void make() {
        int[] userSizes = {MARKER, MARKER_HIGHLIGHTED, TEXT, UIScale.scale(TEXT), UIScale.scale(DESCRIPTION)};
        TreeSet<Integer> deviceSizes = new TreeSet<>();
        for (double scale : scales) {
            for (int size : userSizes)
                deviceSizes.add(deviceSize(size, scale));
        }
        int[] sizes = new int[deviceSizes.size()];
        int i = 0;
        for (int size : deviceSizes)
            sizes[i++] = size;
        int current = ++job;
        List<String> iconNames = names;
        Task.submitBackground(() -> build(iconNames, sizes), made -> {
            if (current != job)
                return;
            icons = made;
            for (Window window : Window.getWindows())
                window.repaint();
            DrawController.drawRequest();
            DisplayController.display();
        }, t -> Log.warn("Cannot load event icons", t));
    }

    private static Icons build(List<String> iconNames, int[] sizes) throws IOException {
        // Sizes are ascending
        int length = sizes.length == 0 ? 0 : sizes[sizes.length - 1] + 1;
        Icons made = new Icons(new HashMap<>(), new HashMap<>());
        for (String name : iconNames) {
            SWEKIconBank.Pixels[] pixels = SWEKIconBank.decode(name, sizes);
            made.full.put(name, pixels[0]);
            BufferedImage[] images = new BufferedImage[length];
            for (int i = 0; i < sizes.length; i++) {
                BufferedImage image = new BufferedImage(sizes[i], sizes[i], BufferedImage.TYPE_4BYTE_ABGR_PRE);
                // Raster elements are RGBA; the standard image stores them as ABGR.
                image.getRaster().setDataElements(0, 0, sizes[i], sizes[i], pixels[i + 1].rgba());
                images[sizes[i]] = image;
            }
            made.images.put(name, images);
        }
        return made;
    }

    private SWEKIconImages() {}
}
