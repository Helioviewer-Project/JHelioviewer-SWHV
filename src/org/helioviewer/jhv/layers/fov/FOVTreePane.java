package org.helioviewer.jhv.layers.fov;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.components.base.JHVTreeCell;
import org.helioviewer.jhv.layers.fov.FOVInstrument.FOVType;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
public class FOVTreePane extends JScrollPane {

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

    public FOVTreePane(JSONObject jo) {
        JSONObject jpo, empty = new JSONObject();

        if (jo == null)
            jo = empty;

        jpo = jo.optJSONObject("SOLO", empty);
        FOVPlatform plat = new FOVPlatform("SOLO", "SOLO", SpaceObject.SOLO.getColor(), jpo);
        plat.add(new FOVInstrument("EUI/HRI", FOVType.RECTANGULAR, 0, 16.6 / 60., 16.6 / 60., jpo));
        plat.add(new FOVInstrument("EUI/FSI", FOVType.RECTANGULAR, 0, 228 / 60., 228 / 60., jpo));
        plat.add(new FOVInstrument("Metis", FOVType.CIRCULAR, 3.2, 6.8, 6.8, jpo));
        plat.add(new FOVInstrument("PHI/HRT", FOVType.RECTANGULAR, 0, 0.28, 0.28, jpo));
        plat.add(new FOVInstrument("PHI/FDT", FOVType.RECTANGULAR, 0, 2, 2, jpo));
        plat.add(new FOVInstrument("SPICE", FOVType.RECTANGULAR, 0, 16 / 60., 11 / 60., jpo));
        plat.add(new FOVInstrument("STIX", FOVType.RECTANGULAR, 0, 2, 2, jpo));
        root.add(plat);

        jpo = jo.optJSONObject("STEREO-A", empty);
        plat = new FOVPlatform("STEREO-A", "STEREO AHEAD", SpaceObject.STA.getColor(), jpo);
        plat.add(new FOVInstrument("EUVI", FOVType.RECTANGULAR, 0, 1.5877740 * 2048 / 3600., 1.5877740 * 2048 / 3600., jpo));
        plat.add(new FOVInstrument("COR1", FOVType.RECTANGULAR, 0, 15.008600 * 512 / 3600., 15.008600 * 512 / 3600., jpo));
        plat.add(new FOVInstrument("COR2", FOVType.CIRCULAR, 0, 14.700000 * 2048 / 3600., 14.700000 * 2048 / 3600., jpo));
        root.add(plat);

        jpo = jo.optJSONObject("SDO", empty);
        plat = new FOVPlatform("SDO", "EARTH", Colors.Blue, jpo); // Earth approximate
        plat.add(new FOVInstrument("AIA", FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., jpo));
        plat.add(new FOVInstrument("HMI", FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., jpo));
        root.add(plat);

        jpo = jo.optJSONObject("PROBA-2", empty);
        plat = new FOVPlatform("PROBA-2", "EARTH", Colors.Blue, jpo); // Earth approximate
        plat.add(new FOVInstrument("SWAP", FOVType.RECTANGULAR, 0, (3.1646941 * 1024) / 3600., (3.1646941 * 1024) / 3600., jpo));
        root.add(plat);

        JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(null);
        tree.setCellRenderer(new JHVTreeCell.Renderer());
        tree.setCellEditor(new JHVTreeCell.Editor());
        tree.setRowHeight(0); // force calculation of nodes heights

        setViewportView(tree);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        getVerticalScrollBar().setUnitIncrement(20); // ugh
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.lightGray));
        setPreferredSize(new Dimension(-1, 98));
    }

    public void init(GL2 gl) {
        root.children().asIterator().forEachRemaining(c -> ((FOVPlatform) c).init(gl));
    }

    public void dispose(GL2 gl) {
        root.children().asIterator().forEachRemaining(c -> ((FOVPlatform) c).dispose(gl));
    }

    public void render(Camera camera, Viewport vp, GL2 gl) {
        root.children().asIterator().forEachRemaining(c -> ((FOVPlatform) c).render(camera, vp, gl));
    }

    public void serialize(JSONObject jo) {
        root.children().asIterator().forEachRemaining(c -> jo.put(c.toString(), ((FOVPlatform) c).toJson()));
    }

}
