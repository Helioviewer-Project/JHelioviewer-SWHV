package org.helioviewer.jhv.base;

public class Pair<A, B> {

    public final A a;
    public final B b;

    public Pair(A _a, B _b) {
        a = _a;
        b = _b;
    }

    @Override
    public String toString() {
        return "Pair<" + a + "," + b + ">";
    }

}
