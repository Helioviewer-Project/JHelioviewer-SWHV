package org.helioviewer.gl3d.spaceobjects;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.gl3d.camera.GL3DSpaceObject;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.layers.LayersModel;

public class PlanetOptionsPanel extends JPanel {
    private final JList planetList;
    private final DefaultListModel planetListModel;
    private JComboBox objectCombobox;
    private JComboBox viewPointCombobox;

    public PlanetOptionsPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        planetListModel = new DefaultListModel();
        planetList = new JList(planetListModel);
        add(planetList);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));
        bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        addObjectCombobox();
        add(this.objectCombobox);
        addViewPointCombobox();
        add(this.viewPointCombobox);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] so = objectCombobox.getSelectedObjects();
                Object[] vp = viewPointCombobox.getSelectedObjects();
                if (so.length > 0) {
                    GL3DSpaceObject spaceobj = (GL3DSpaceObject) (so[0]);
                    GL3DSpaceObject vpo = (GL3DSpaceObject) (vp[0]);
                    addPlanet(new Planet(spaceobj, vpo));
                }
            }
        });
        bottomPanel.add(addButton);
        bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        JButton removeButton = new JButton("Remove");

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] idxs = planetList.getSelectedIndices();
                for (int i = 0; i < idxs.length; i++) {
                    removePlanet((Planet) (planetListModel.elementAt(idxs[i])));
                }
            }
        });
        bottomPanel.add(removeButton);
        add(bottomPanel);

    }

    public void addPlanet(Planet planet) {
        GL3DViewchainFactory.currentSceneGraph.getRoot().addNode(planet);
        this.planetListModel.addElement(planet);
        GL3DViewchainFactory.currentSceneGraph.addViewListener(planet);
        LayersModel.getSingletonInstance().addLayersListener(planet);
    }

    public void removePlanet(Planet planet) {
        GL3DViewchainFactory.currentSceneGraph.getRoot().removeNode(planet);
        GL3DViewchainFactory.currentSceneGraph.removeViewListener(planet);
        LayersModel.getSingletonInstance().removeLayersListener(planet);
        this.planetListModel.removeElement(planet);
    }

    private void addObjectCombobox() {
        objectCombobox = new JComboBox();
        GL3DSpaceObject[] objectList = GL3DSpaceObject.getObjectList();
        for (int i = 0; i < objectList.length; i++) {
            objectCombobox.addItem(objectList[i]);
        }
        objectCombobox.setSelectedItem(GL3DSpaceObject.earth);
    }

    private void addViewPointCombobox() {
        viewPointCombobox = new JComboBox();
        GL3DSpaceObject[] objectList = GL3DSpaceObject.getObjectList();
        for (int i = 0; i < objectList.length; i++) {
            viewPointCombobox.addItem(objectList[i]);
        }
        viewPointCombobox.setSelectedItem(GL3DSpaceObject.earth);
    }
}
