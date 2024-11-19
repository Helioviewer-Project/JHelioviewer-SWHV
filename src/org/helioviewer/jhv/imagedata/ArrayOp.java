package org.helioviewer.jhv.imagedata;

import java.util.concurrent.RecursiveAction;

@SuppressWarnings("serial")
interface ArrayOp {

    void accept(float[] arg1, float[] arg2, float[] arg3, int start, int end);

    int THRESHOLD = 64; // Adjust based on image size and system

    class Task3 extends RecursiveAction {

        private final float[] arg1;
        private final float[] arg2;
        private final float[] arg3;
        private final int start;
        private final int end;
        private final ArrayOp op;

        Task3(float[] arg1, float[] arg2, float[] arg3, int start, int end, ArrayOp op) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.start = start;
            this.end = end;
            this.op = op;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                op.accept(arg1, arg2, arg3, start, end);
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new Task3(arg1, arg2, arg3, start, mid, op),
                        new Task3(arg1, arg2, arg3, mid, end, op));
            }
        }

    }

}
