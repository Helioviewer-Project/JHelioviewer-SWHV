package org.helioviewer.jhv.ui;

import java.util.function.Consumer;

// Replies to the requests of one kind: only the latest counts, unless cancelled.
final class Requests {

    private int latest;

    void cancel() {
        latest++;
    }

    <T> Consumer<T> latest(Consumer<T> reply) {
        int request = ++latest;
        return value -> {
            if (request == latest)
                reply.accept(value);
        };
    }

}
