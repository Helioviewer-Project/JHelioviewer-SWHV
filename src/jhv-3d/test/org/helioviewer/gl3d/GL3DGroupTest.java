package org.helioviewer.gl3d;

import junit.framework.Assert;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.junit.Test;

public class GL3DGroupTest {
    @Test
    public void testMoveUp() {
        GL3DGroup group = new GL3DGroup("Test Group");
        GL3DGroup child1 = new GL3DGroup("Test Child 1");
        GL3DGroup child2 = new GL3DGroup("Test Child 2");
        group.addNode(child1);
        group.addNode(child2);

        Assert.assertEquals(child1, group.getChild(0));
        Assert.assertEquals(child2, group.getChild(1));
        Assert.assertEquals(null, group.getChild(2));

        group.moveNode(child2, 0);

        Assert.assertEquals(child1, group.getChild(1));
        Assert.assertEquals(child2, group.getChild(0));

        group = new GL3DGroup("Test Group");
        child1 = new GL3DGroup("Test Child 1");
        child2 = new GL3DGroup("Test Child 2");
        GL3DGroup child3 = new GL3DGroup("Test Child 3");
        group.addNode(child1);
        group.addNode(child2);
        group.addNode(child3);

        group.moveNode(child3, 0);

        Assert.assertEquals(child3, group.getChild(0));
        Assert.assertEquals(child1, group.getChild(1));
        Assert.assertEquals(child2, group.getChild(2));

        group.moveNode(child1, 0);

        Assert.assertEquals(child1, group.getChild(0));
        Assert.assertEquals(child3, group.getChild(1));
        Assert.assertEquals(child2, group.getChild(2));
    }

    @Test
    public void testMoveDown() {
        GL3DGroup group = new GL3DGroup("Test Group");
        GL3DGroup child1 = new GL3DGroup("Test Child 1");
        GL3DGroup child2 = new GL3DGroup("Test Child 2");
        group.addNode(child1);
        group.addNode(child2);

        Assert.assertEquals(child1, group.getChild(0));
        Assert.assertEquals(child2, group.getChild(1));
        Assert.assertEquals(null, group.getChild(2));

        group.moveNode(child1, 1);

        Assert.assertEquals(child2, group.getChild(0));
        Assert.assertEquals(child1, group.getChild(1));

        group = new GL3DGroup("Test Group");
        child1 = new GL3DGroup("Test Child 1");
        child2 = new GL3DGroup("Test Child 2");
        GL3DGroup child3 = new GL3DGroup("Test Child 3");
        group.addNode(child1);
        group.addNode(child2);
        group.addNode(child3);

        group.moveNode(child1, 2);

        Assert.assertEquals(child2, group.getChild(0));
        Assert.assertEquals(child3, group.getChild(1));
        Assert.assertEquals(child1, group.getChild(2));

        group.moveNode(child2, 1);

        Assert.assertEquals(child3, group.getChild(0));
        Assert.assertEquals(child2, group.getChild(1));
        Assert.assertEquals(child1, group.getChild(2));

        group.moveNode(child2, 1);

        Assert.assertEquals(child3, group.getChild(0));
        Assert.assertEquals(child2, group.getChild(1));
        Assert.assertEquals(child1, group.getChild(2));
    }

}
