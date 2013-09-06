package org.helioviewer.gl3d.scenegraph;

/**
 * The draw bits store attributes within the scene graph. Every node has its
 * draw bits object. An attribute applies to the node and all of its child
 * nodes.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DDrawBits {
    private boolean[] drawBits;

    public GL3DDrawBits() {
        this.init();
    }

    public void toggle(Bit bit) {
        set(bit, !get(bit));
    }

    public void on(Bit bit) {
        set(bit, true);
    }

    public void off(Bit bit) {
        set(bit, false);
    }

    public void set(Bit bit, boolean value) {
        this.drawBits[bit.pos] = value;
    }

    public boolean get(Bit bit) {
        return this.drawBits[bit.pos];
    }

    public void allOff() {
        setAll(false);
    }

    public void allOn() {
        setAll(true);
    }

    private void setAll(boolean value) {
        for (int i = 0; i < drawBits.length; i++) {
            this.drawBits[i] = value;
        }
    }

    private void init() {
        this.drawBits = new boolean[Bit.values().length];
        for (int i = 0; i < drawBits.length; i++) {
            this.drawBits[i] = find(i).value;
        }
    }

    private Bit find(int pos) {
        for (Bit b : Bit.values()) {
            if (b.pos == pos)
                return b;
        }
        return null;
    }

    public enum Bit {
        Hidden(0, false), Normals(1, false), BoundingBox(2, false), Wireframe(3, false), Selected(4, false);

        public boolean value;
        public int pos;

        private Bit(int pos, boolean defaultValue) {
            this.value = defaultValue;
            this.pos = pos;
        }
    }

}
