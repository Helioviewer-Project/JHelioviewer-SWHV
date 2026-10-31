package org.helioviewer.jhv.ui;

// A screen of controls: it describes itself on demand and takes typed input, knowing neither toolkit nor window.
public interface Screen {

    // Borrowed snapshot, consumed synchronously before the next build.
    Panel build();

    // The one view of this screen. The listener runs on any thread when build() would show something else.
    default void listen(Runnable listener) {}

    default void command(int owner, int item, Panel.Command command) {}

    default void number(int owner, int item, double value) {}

    default void checked(int owner, int item, boolean value) {}

    default void choice(int owner, int item, int value) {}

    default void reorder(int owner, int item, int destination) {}

    default void setRange(int owner, int item, double low, double high) {}

    default void setText(int owner, int item, String text) {}

    default void files(int owner, int item, String[] paths) {}

}
