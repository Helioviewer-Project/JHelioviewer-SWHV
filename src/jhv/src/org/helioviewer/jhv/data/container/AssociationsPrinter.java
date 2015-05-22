package org.helioviewer.jhv.data.container;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;

public class AssociationsPrinter {

    private static int fileNumber = 0;

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
            writer.write("    node [ color=\"" + ((1.0 * c.getRed()) / 255) + " " + (1.0 * c.getGreen()) / 255 + " " + (1.0 * c.getBlue()) / 255 + "\"];\n");
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
            writer.write("    edge [ color=\"" + ((1.0 * c.getRed()) / 255) + " " + (1.0 * c.getGreen()) / 255 + " " + (1.0 * c.getBlue()) / 255 + "\"];\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void print(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter("/Users/bramb/ass" + fileNumber + ".txt", true));
            for (NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>> p1 : events.values()) {
                for (NavigableMap<Date, List<JHVEvent>> p2 : p1.values()) {
                    for (List<JHVEvent> p3 : p2.values()) {
                        for (JHVEvent event : p3) {
                            printNodeColor(event.getEventRelationShip().getRelationshipColor(), writer);
                            for (JHVEventRelation er : event.getEventRelationShip().getNextEvents().values()) {
                                if (er.getTheEvent() != null) {
                                    printAssociation(event.getUniqueID(), er.getTheEvent().getUniqueID(), writer);
                                }
                            }
                        }
                    }
                }
            }
            writer.flush();
            writer.close();
            fileNumber++;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void printNodeColor(Color c, BufferedWriter writer) {
        try {
            writer.write("    node [ color=\"" + ((1.0 * c.getRed()) / 255) + " " + (1.0 * c.getGreen()) / 255 + " " + (1.0 * c.getBlue()) / 255 + "\"];\n");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void printAssociation(String association1, String association2, BufferedWriter writer) {

        try {
            writer.write("    \"" + association1 + "\" -> \"" + association2 + "\";\n");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
