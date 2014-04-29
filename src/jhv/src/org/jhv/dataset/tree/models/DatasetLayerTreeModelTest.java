package org.jhv.dataset.tree.models;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DatasetLayerTreeModelTest {
    DatasetLayerTreeModel modeltest;
    LayersModel layersModel;
    ArrayList<LayerDescriptor> descriptors;
    ArrayList<String> intervals;

    @Before
    public void setUp() throws Exception {
        modeltest = new DatasetLayerTreeModel();
        descriptors = new ArrayList<LayerDescriptor>();
        intervals = new ArrayList<String>();
        intervals.add(0, "first");
        intervals.add(1, "second");
        intervals.add(2, "third");

        descriptors.add(0, new LayerDescriptor("imageInfoView", intervals.get(0)));
        descriptors.add(1, new LayerDescriptor("imageInfoView", intervals.get(1)));
        descriptors.add(2, new LayerDescriptor("imageInfoView", intervals.get(2)));

        descriptors.add(3, new LayerDescriptor("imageInfoView", intervals.get(0)));
        descriptors.add(4, new LayerDescriptor("imageInfoView", intervals.get(1)));
        descriptors.add(5, new LayerDescriptor("imageInfoView", intervals.get(2)));

        descriptors.add(6, new LayerDescriptor("imageInfoView", intervals.get(2)));

        descriptors.add(7, new LayerDescriptor("image", intervals.get(2)));

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAddRemove3() {
        modeltest.addLayerDescriptor(descriptors.get(2), 0);
        modeltest.getDescriptor(descriptors.get(2).getInterval(), descriptors.get(2).getType(), 0);
        modeltest.addLayerDescriptor(descriptors.get(5), 0);
        modeltest.getDescriptor(descriptors.get(5).getInterval(), descriptors.get(5).getType(), 0);
        modeltest.getDescriptor(descriptors.get(2).getInterval(), descriptors.get(2).getType(), 1);
        modeltest.addLayerDescriptor(descriptors.get(6), 0);
        modeltest.getDescriptor(descriptors.get(6).getInterval(), descriptors.get(6).getType(), 0);
        modeltest.getDescriptor(descriptors.get(5).getInterval(), descriptors.get(5).getType(), 1);
        modeltest.getDescriptor(descriptors.get(2).getInterval(), descriptors.get(2).getType(), 2);
        assertTrue(descriptors.get(2) == modeltest.getDescriptor(descriptors.get(2).getInterval(), descriptors.get(2).getType(), 2));

        modeltest.removeLayerDescriptor(descriptors.get(6), 0);
        modeltest.getDescriptor(descriptors.get(5).getInterval(), descriptors.get(5).getType(), 0);
        modeltest.getDescriptor(descriptors.get(2).getInterval(), descriptors.get(2).getType(), 1);
        modeltest.removeLayerDescriptor(descriptors.get(5), 0);
        modeltest.getDescriptor(descriptors.get(2).getInterval(), descriptors.get(2).getType(), 0);
        assertTrue(descriptors.get(2) == modeltest.getDescriptor(descriptors.get(2).getInterval(), descriptors.get(2).getType(), 0));
        modeltest.removeLayerDescriptor(descriptors.get(2), 0);
    }

    @Test
    public void testAddRemove2() {
        modeltest.addLayerDescriptor(descriptors.get(0), 0);
        assertTrue(modeltest.getNumLayers() == 1);
        assertTrue(modeltest.getNumTypes() == 1);

        modeltest.addLayerDescriptor(descriptors.get(1), 0);
        assertTrue(modeltest.getNumLayers() == 2);
        assertTrue(modeltest.getNumTypes() == 2);

        modeltest.addLayerDescriptor(descriptors.get(2), 0);
        assertTrue(modeltest.getNumLayers() == 3);
        assertTrue(modeltest.getNumTypes() == 3);

        modeltest.addLayerDescriptor(descriptors.get(3), 0);
        assertTrue(modeltest.getNumLayers() == 4);
        assertTrue(modeltest.getNumTypes() == 3);

        modeltest.addLayerDescriptor(descriptors.get(4), 0);
        assertTrue(modeltest.getNumLayers() == 5);
        assertTrue(modeltest.getNumTypes() == 3);

        modeltest.addLayerDescriptor(descriptors.get(5), 0);
        assertTrue(modeltest.getNumLayers() == 6);
        assertTrue(modeltest.getNumTypes() == 3);

        modeltest.addLayerDescriptor(descriptors.get(6), 0);
        assertTrue(modeltest.getNumLayers() == 7);
        assertTrue(modeltest.getNumTypes() == 3);
        assertTrue(modeltest.getNumIntervals() == 3);

    }

    @Test
    public void testAddRemove() {
        modeltest.addLayerDescriptor(descriptors.get(0), 0);
        assertTrue(modeltest.getNumLayers() == 1);

        modeltest.addLayerDescriptor(descriptors.get(1), 0);
        assertTrue(modeltest.getNumLayers() == 2);

        modeltest.addLayerDescriptor(descriptors.get(2), 0);
        assertTrue(modeltest.getNumLayers() == 3);

        modeltest.addLayerDescriptor(descriptors.get(3), 0);
        assertTrue(descriptors.get(3) != null);
        assertTrue(modeltest.getNumLayers() == 4);

        modeltest.addLayerDescriptor(descriptors.get(4), 0);
        assertTrue(modeltest.getNumLayers() == 5);

        modeltest.addLayerDescriptor(descriptors.get(5), 0);
        assertTrue(modeltest.getNumLayers() == 6);

        modeltest.addLayerDescriptor(descriptors.get(6), 0);
        assertTrue(modeltest.getNumLayers() == 7);

        assertTrue(modeltest.getNumLayers() == 7);
        assertTrue(modeltest.getNumTypes() == 3);
        assertTrue(modeltest.getNumIntervals() == 3);

        modeltest.addLayerDescriptor(descriptors.get(7), 0);
        assertTrue(modeltest.getNumLayers() == 8);
        assertTrue(modeltest.getNumTypes() == 4);
        assertTrue(modeltest.getNumIntervals() == 3);

        modeltest.removeLayerDescriptor(descriptors.get(6), 0);
        assertTrue(modeltest.getNumLayers() == 7);
        assertTrue(modeltest.getNumTypes() == 4);
        assertTrue(modeltest.getNumIntervals() == 3);

        modeltest.removeLayerDescriptor(descriptors.get(0), 0);
        assertTrue(modeltest.getNumLayers() == 6);
        assertTrue(modeltest.getNumTypes() == 4);
        assertTrue(modeltest.getNumIntervals() == 3);

        modeltest.removeLayerDescriptor(descriptors.get(3), 0);
        assertTrue(modeltest.getNumLayers() == 5);
        assertTrue(modeltest.getNumTypes() == 3);
        assertTrue(modeltest.getNumIntervals() == 2);

    }

    @Test
    public void testAddInterval() {
        String intervalkey = "intervalkey";
        modeltest.addInterval(intervalkey);
        assertTrue(modeltest.getNumIntervals() == 1);
        assertTrue(modeltest.getSortedLayers().containsKey(intervalkey));
    }

    @Test
    public void testAddType() {
        String typekey = "typekey";
        String intervalkey = "intervalkey";
        assertTrue(!modeltest.getSortedLayers().containsKey(intervalkey));

        modeltest.addType(typekey, intervalkey);
        assertTrue(modeltest.getNumIntervals() == 1);
        assertTrue(modeltest.getNumTypes() == 1);
        assertTrue(modeltest.getSortedLayers().containsKey(intervalkey));
        assertTrue(modeltest.getSortedLayers().get(intervalkey).containsKey(typekey));
        assertTrue(modeltest.getSortedLayers().get(intervalkey).get(typekey) != null);

    }

}
