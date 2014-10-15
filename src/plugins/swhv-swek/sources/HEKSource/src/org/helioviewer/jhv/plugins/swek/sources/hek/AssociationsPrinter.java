package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AssociationsPrinter {
    public static void printAssociation(String association1, String association2) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter("/Users/bramb/ass.txt", true));
            writer.write("    \"" + association1 + "\" -> \"" + association2 + "\";\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void printNodeColor(Color c) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter("/Users/bramb/ass.txt", true));
            writer.write("    node [ color=\"" + ((1.0 * c.getRed()) / 255) + " " + (1.0 * c.getGreen()) / 255 + " " + (1.0 * c.getBlue())
                    / 255 + "\"];\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void printEdgeColor(Color c) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter("/Users/bramb/ass.txt", true));
            writer.write("    edge [ color=\"" + ((1.0 * c.getRed()) / 255) + " " + (1.0 * c.getGreen()) / 255 + " " + (1.0 * c.getBlue())
                    / 255 + "\"];\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
