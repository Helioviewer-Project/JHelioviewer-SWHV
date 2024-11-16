package org.helioviewer.jhv.imagedata;

import java.util.concurrent.RecursiveAction;

@SuppressWarnings("serial")
interface ArrayOp {

    void accept(float[] op1, float[] op2, float[] dest, int start, int end);

    int THRESHOLD = 64; // Adjust based on image size and system

    class TaskTwo extends RecursiveAction {

        private final float[] op1;
        private final float[] op2;
        private final float[] dest;
        private final int start;
        private final int end;
        private final ArrayOp operation;

        TaskTwo(float[] op1, float[] op2, float[] dest, int start, int end, ArrayOp operation) {
            this.op1 = op1;
            this.op2 = op2;
            this.dest = dest;
            this.start = start;
            this.end = end;
            this.operation = operation;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                operation.accept(op1, op2, dest, start, end);
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new TaskTwo(op1, op2, dest, start, mid, operation),
                        new TaskTwo(op1, op2, dest, mid, end, operation));
            }
        }

    }

}
