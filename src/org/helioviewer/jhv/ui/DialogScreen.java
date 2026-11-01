package org.helioviewer.jhv.ui;

// A screen in a window of its own, which it asks to close.
public abstract class DialogScreen implements Screen {

    private Runnable changed = () -> {};
    private Runnable dismiss = () -> {};

    public abstract String title();

    @Override
    public final void listen(Runnable listener) {
        changed = listener;
    }

    protected final void changed() {
        changed.run();
    }

    // The window is about to open.
    public void show() {}

    public void close() {
        dismiss.run();
    }

    public final void onDismiss(Runnable listener) {
        dismiss = listener;
    }

}
